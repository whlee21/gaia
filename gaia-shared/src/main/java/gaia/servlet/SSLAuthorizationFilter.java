package gaia.servlet;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import gaia.Defaults;
import gaia.ssl.SSLAuthorizer;
import gaia.ssl.SSLConfigManager;

@Singleton
public class SSLAuthorizationFilter implements Filter {
	private static transient Logger LOG = LoggerFactory.getLogger(SSLAuthorizationFilter.class);

	public void destroy() {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		X509Certificate[] certificateChain = (X509Certificate[]) servletRequest
				.getAttribute("javax.servlet.request.X509Certificate");

		if (SSLAuthorizer.authorizeRequest(servletRequest.isSecure(), certificateChain, servletRequest.getRemoteAddr())) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Reguest authorized.");
			}
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
			httpServletResponse.sendError(403, "Not authorized.");

			if (LOG.isDebugEnabled())
				LOG.debug("Reguest not authorized.");
		}
	}

	public void init(FilterConfig arg0) throws ServletException {
		Defaults.injector.getInstance(SSLConfigManager.class);
	}
}
