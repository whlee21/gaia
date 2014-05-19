package gaia.crawl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DataSourceHistory extends History {
	private static transient Logger LOG = LoggerFactory.getLogger(DataSourceHistory.class);

	@Inject
	public DataSourceHistory(@Named("dshistory-filename") String filename) {
		super(filename);
	}
}
