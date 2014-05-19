package gaia.crawl;

import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Defaults;
import gaia.crawl.impl.TikaParserController;
import gaia.crawl.io.Content;

public class TikaCrawlProcessor extends CrawlProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(TikaCrawlProcessor.class);
	private TikaParserController parser;
	private boolean commitOnFinish = true;

	public TikaCrawlProcessor(UpdateController updateController) {
		super(updateController);
		parser = new TikaParserController(updateController);
	}

	public void init(CrawlState state) throws Exception {
		super.init(state);
		parser.init(state);
		if (state.getDataSource() != null)
			commitOnFinish = state.getDataSource().getBoolean("commit_on_finish",
					Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "commit_on_finish"));
	}

	public void finish() throws Exception {
		if ((updateController != null) && (updateController.isStarted()))
			updateController.finish(commitOnFinish);
	}

	public TikaParserController getParserController() {
		return parser;
	}

	public void process(Content content) throws Exception {
		List<SolrInputDocument> docs = null;
		try {
			docs = parser.parse(content);
		} catch (Exception e) {
			LOG.debug("Error: ", e);

			SolrInputDocument doc = new SolrInputDocument();
			Content.fill(doc, state.getDataSource().getFieldMapping(), content);
			doc.addField("parsing", "failed: " + e.getMessage());

			docs = Collections.singletonList(doc);
			state.status.incrementCounter(CrawlStatus.Counter.Failed);
		}

		for (SolrInputDocument doc : docs)
			try {
				processParsedDocument(doc);
			} catch (Exception e) {
				LOG.warn("Exception adding document", e);
			}
	}

	public void delete(String key) throws Exception {
		updateController.delete(key);
		state.status.incrementCounter(CrawlStatus.Counter.Deleted);
	}

	public void processParsedDocument(SolrInputDocument doc) throws Exception {
		updateController.add(doc);

		state.status.incrementCounter(CrawlStatus.Counter.New);
	}

	public void start() throws Exception {
		if (updateController != null)
			updateController.start();
	}

	public void setUpdateController(UpdateController updateController) throws Exception {
		super.setUpdateController(updateController);
		parser = new TikaParserController(updateController);
		parser.init(state);
		updateController.init(state.ds);
	}

	public String getBatchId() {
		return parser.getBatchId();
	}
}
