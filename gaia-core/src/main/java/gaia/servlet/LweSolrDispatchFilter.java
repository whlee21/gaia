package gaia.servlet;

import gaia.admin.collection.AdminScheduler;
import gaia.api.ClickLoggingContext;
import gaia.api.RestAPI;
import gaia.crawl.ConnectorManager;
import gaia.heartbeat.HeartbeatManager;
import gaia.jmx.JmxManager;
import gaia.scheduler.DefaultSolrScheduler;
import gaia.ssl.SSLConfigManager;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class LweSolrDispatchFilter extends SolrDispatchFilter {
	private static transient Logger log = LoggerFactory.getLogger(LweSolrDispatchFilter.class);
	private final AdminScheduler adminScheduler;
	private final DefaultSolrScheduler solrScheduler;
	private CoreContainer coreContainer;
	private ConnectorManager crawlerManager;
	private JmxManager jmxManager;
	private HeartbeatManager heartbeat;
	private RestAPI restAPI;
	private ClickLoggingContext clickCtx;

	@Inject
	public LweSolrDispatchFilter(SSLConfigManager mgr, CoreContainer cores, AdminScheduler adminScheduler,
			DefaultSolrScheduler solrScheduler, ConnectorManager crawlerManager, RestAPI restAPI, JmxManager jmxManager,
			ClickLoggingContext clickCtx, HeartbeatManager heartbeat) throws SchedulerException {
		this.restAPI = restAPI;
		this.adminScheduler = adminScheduler;
		this.solrScheduler = solrScheduler;
		this.coreContainer = cores;
		this.crawlerManager = crawlerManager;
		this.jmxManager = jmxManager;
		this.clickCtx = clickCtx;
		this.heartbeat = heartbeat;
	}

	public void init(FilterConfig config) throws ServletException {
		super.init(config);
	}

	protected CoreContainer createCoreContainer() {
		return this.coreContainer;
	}

	public void destroy() {
		this.adminScheduler.stopAndRemoveAllSchedules();
		try {
			this.restAPI.stop();
			this.crawlerManager.shutdown();
			this.clickCtx.shutdown();
		} catch (Exception e) {
			log.error("", e);
		}

		this.solrScheduler.shutdown();

		this.coreContainer.shutdown();

		this.jmxManager.unregisterOnShutdown();

		this.heartbeat.shutdown();

		super.destroy();
	}
}
