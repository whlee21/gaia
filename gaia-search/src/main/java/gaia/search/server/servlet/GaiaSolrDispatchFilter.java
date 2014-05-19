package gaia.search.server.servlet;

import gaia.admin.collection.AdminScheduler;
import gaia.api.ClickLoggingContext;
import gaia.crawl.ConnectorManager;
import gaia.heartbeat.HeartbeatManager;
import gaia.jmx.JmxManager;
import gaia.scheduler.DefaultSolrScheduler;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GaiaSolrDispatchFilter extends SolrDispatchFilter {
	private static transient Logger LOG = LoggerFactory.getLogger(GaiaSolrDispatchFilter.class);

	private final AdminScheduler adminScheduler;
	private final DefaultSolrScheduler solrScheduler;
	private CoreContainer cores;

	private ConnectorManager crawlerManager;
	private JmxManager jmxManager;
	private HeartbeatManager heartbeat;
	private ClickLoggingContext clickCtx;

	// @Inject
	// public GaiaSolrDispatchFilter(SSLConfigManager mgr, CoreContainer cores,
	// AdminScheduler adminScheduler,
	// DefaultSolrScheduler solrScheduler, ConnectorManager crawlerManager,
	// JmxManager jmxManager,
	// ClickLoggingContext clickCtx, HeartbeatManager heartbeat) {
	// this.adminScheduler = adminScheduler;
	// this.solrScheduler = solrScheduler;
	// this.coreContainer = cores;
	// this.crawlerManager = crawlerManager;
	// this.jmxManager = jmxManager;
	// this.clickCtx = clickCtx;
	// this.heartbeat = heartbeat;
	// }

	@Inject
	public GaiaSolrDispatchFilter(CoreContainer cores, AdminScheduler adminScheduler, DefaultSolrScheduler solrScheduler,
			ConnectorManager crawlerManager, JmxManager jmxManager, ClickLoggingContext clickCtx, HeartbeatManager heartbeat) {
		this.adminScheduler = adminScheduler;
		this.solrScheduler = solrScheduler;
		this.cores = cores;
		this.crawlerManager = crawlerManager;
		this.jmxManager = jmxManager;
		this.clickCtx = clickCtx;
		this.heartbeat = heartbeat;
	}

	public void init(FilterConfig config) throws ServletException {
		super.init(config);
	}

	public void destroy() {
		adminScheduler.stopAndRemoveAllSchedules();
		try {
			crawlerManager.shutdown();
			clickCtx.shutdown();
		} catch (Exception e) {
			LOG.error("", e);
		}
		solrScheduler.shutdown();
		jmxManager.unregisterOnShutdown();
		heartbeat.shutdown();

		super.destroy();
	}

	protected CoreContainer createCoreContainer() {
		return this.cores;
	}

}
