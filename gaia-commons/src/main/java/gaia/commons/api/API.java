package gaia.commons.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;
import org.restlet.routing.Route;
import org.restlet.routing.Router;
import org.restlet.security.Authenticator;
import org.restlet.security.Role;
import org.restlet.security.RoleAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.commons.management.APIMBean;

public abstract class API extends Application implements APIMBean {
	private static transient Logger LOG = LoggerFactory.getLogger(API.class);
	protected Router router;
	protected ResourceFinder finder;
	protected Collection<String> paths;
	volatile long numRequests;
	volatile long numErrors;
	volatile long totalTime = 0L;
	long handlerStart = System.currentTimeMillis();

	protected API(ResourceFinder finder) {
		this.finder = finder;
		paths = new ArrayList<String>();
	}

	public Restlet createInboundRoot() {
		router = new Router(getContext());
		Restlet guard = guardPossibly();
		initAttachments();
		return guard;
	}

	protected Restlet guardPossibly() {
		return router;
	}

	public void handle(Request request, Response response) {
		logStart(request, response);
		long start = System.currentTimeMillis();
		super.handle(request, response);
		long finish = System.currentTimeMillis();
		if (response.getStatus().isError()) {
			numErrors += 1L;
		}
		long diff = finish - start;
		if ((LOG.isInfoEnabled()) && (request.isLoggable())) {
			LOG.info(getAPIName() + "/" + getAPIRoot() + " took " + diff + " ms");
		}
		totalTime += diff;
		logFinish(request, response);
		numRequests += 1L;
	}

	protected void logFinish(Request request, Response response) {
		if ((LOG.isInfoEnabled()) && (request.isLoggable()))
			LOG.info("Finish Req #: " + numRequests + " Req: " + request.toString() + " Rsp: " + response.toString());
	}

	protected void logStart(Request request, Response response) {
		if ((LOG.isInfoEnabled()) && (request.isLoggable()))
			LOG.info("Start Req #: " + numRequests + " Req: " + request.toString());
	}

	protected Route attach(String path, Class<? extends ServerResource> resource) {
		return attach(path, router, resource, (Role[]) null);
	}

	protected Route attach(String path, Router base, Class<? extends ServerResource> resource) {
		return attach(path, base, resource, (Role[]) null);
	}

	protected Route attach(String path, Router base, Class<? extends ServerResource> resource, Role[] roles) {
		return attach(path, base, resource, (roles != null) && (roles.length > 0) ? Arrays.asList(roles) : null);
	}

	protected Route attach(String path, Router base, Class<? extends ServerResource> resource, List<Role> roles) {
		Restlet attachment = null;
		if ((roles != null) && (!roles.isEmpty())) {
			RoleAuthorizer ra = new RoleAuthorizer();
			ra.getAuthorizedRoles().addAll(roles);
			ra.setNext(finder.finderOf(resource));
			attachment = ra;
		} else {
			attachment = finder.finderOf(resource);
		}
		return attach(path, base, attachment, (Authenticator) null);
	}

	protected Route attach(String path, Router base, Class<? extends ServerResource> resource, Authenticator authenticator) {
		return attach(path, base, finder.finderOf(resource), authenticator);
	}

	protected Route attach(String path, Router base, Restlet restlet) {
		return attach(path, base, restlet, (Authenticator) null);
	}

	protected Route attach(String path, Router base, Restlet restlet, Authenticator authenticator) {
		Route result;
		if (authenticator != null) {
			authenticator.setNext(restlet);
			result = base.attach(path, authenticator);
		} else {
			result = base.attach(path, restlet);
		}
		if (path.equals("")) {
			path = "/";
		}
		paths.add(path);
		return result;
	}

	protected void redir(URI uri, Router base, String attach, String path, Role[] roles) {
		Redirector redir = new Redirector(router.getContext(), uri.toString() + path, 6);
		Restlet attachment;
		if ((roles != null) && (roles.length > 0))
			attachment = getRoleAuthorizer(redir, roles);
		else {
			attachment = redir;
		}
		attach(attach, base, attachment);
	}

	protected RoleAuthorizer getRoleAuthorizer(Restlet next, Role[] roles) {
		RoleAuthorizer ra = new RoleAuthorizer();
		ra.getAuthorizedRoles().addAll(Arrays.asList(roles));
		ra.setNext(next);
		return ra;
	}

	protected abstract void initAttachments();

	public abstract String getAPIRoot();

	public abstract String getAPIName();

	public void stop() throws Exception {
		if (router != null)
			router.stop();
	}

	public Collection<String> getEndpoints() {
		return paths;
	}

	public Map<String, Object> getStatistics() {
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("numRequests", Long.valueOf(numRequests));
		results.put("numErrors", Long.valueOf(numErrors));
		results.put("totalTime", Long.valueOf(totalTime));
		results.put("startTime", Long.valueOf(handlerStart));
		return results;
	}

	public String getName() {
		return getAPIName();
	}
}
