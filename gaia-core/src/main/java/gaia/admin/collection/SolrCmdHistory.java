package gaia.admin.collection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import gaia.crawl.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SolrCmdHistory extends History {
	private static transient Logger LOG = LoggerFactory.getLogger(SolrCmdHistory.class);

	@Inject
	public SolrCmdHistory(@Named("cmdhistory-filename") String filename) {
		super(filename);
	}
}
