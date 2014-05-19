package gaia.ssl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.utils.HttpClientSSLUtil;
import gaia.utils.MasterConfUtil;
import java.io.IOException;
import java.net.URL;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SolrSSLConfigManager extends HttpClientConfigurer {
	private static final Logger LOG = LoggerFactory.getLogger(SolrSSLConfigManager.class);

	boolean ssl = false;

	@Inject
	public SolrSSLConfigManager() {
		HttpClientUtil.setConfigurer(this);
		try {
			URL coreAddress = MasterConfUtil.getGaiaSearchAddress();
			LOG.info("core address from master.conf:" + coreAddress);
			ssl = "https".equals(coreAddress.getProtocol());
		} catch (IOException e) {
			LOG.warn("Could not parse core address from master.conf", e);
		}
	}

	protected void configure(DefaultHttpClient httpClient, SolrParams config) {
		if (ssl) {
			HttpClientSSLUtil.prepareClient(httpClient);
		}
		super.configure(httpClient, config);
	}
}
