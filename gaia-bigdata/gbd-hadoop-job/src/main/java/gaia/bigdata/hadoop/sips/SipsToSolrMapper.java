package gaia.bigdata.hadoop.sips;

import gaia.bigdata.hadoop.GaiaCounters;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SipsToSolrMapper extends Mapper<Text, DoubleWritable, NullWritable, NullWritable> {
	private static final Logger log = LoggerFactory.getLogger(SipsToSolrMapper.class);

	private SolrServer solrServer = null;
	private SolrInputDocument solrDocument = new SolrInputDocument();

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		String zkHost = conf.get("solr.zkhost");
		if ((zkHost != null) && (!zkHost.equals(""))) {
			String collection = conf.get("solr.zk.collection", "collection1");
			log.info("Indexing to collection: " + collection + " w/ ZK host: " + zkHost);
			try {
				solrServer = new CloudSolrServer(zkHost);
			} catch (MalformedURLException e) {
				log.error("Cannot connect to ZK Solr instance for collocations: ", e);
			}
			((CloudSolrServer) solrServer).setDefaultCollection(collection + "_collocations");
		} else {
			String solrURL = conf.get("solr.server.url");
			int queueSize = conf.getInt("solr.client.queue.size", 100);
			int threadCount = conf.getInt("solr.client.threads", 1);
			solrServer = new ConcurrentUpdateSolrServer(solrURL + "_collocations", queueSize, threadCount);
		}
	}

	@Override
	protected void map(Text key, DoubleWritable value, Context context) throws IOException {
		solrDocument = new SolrInputDocument();
		solrDocument.addField("id", key.toString());
		solrDocument.addField("sip_score", value);
		try {
			solrServer.add(solrDocument);
			// reporter.incrCounter(GaiaCounters.SIPS_TO_SOLR_INDEXED, 1L);
			context.getCounter(GaiaCounters.SIPS_TO_SOLR_INDEXED).increment(1L);
		} catch (SolrServerException e) {
			log.warn("Cannot index collocations: ", e);
			// reporter.incrCounter(GaiaCounters.SIPS_TO_SOLR_FAILED, 1L);
			context.getCounter(GaiaCounters.SIPS_TO_SOLR_FAILED).increment(1L);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException {
		try {
			solrServer.commit(false, false);
			solrServer.shutdown();
		} catch (SolrServerException e) {
			log.error("Cannot close index for collocations: ", e);
		}
	}
}
