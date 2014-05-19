package gaia.crawl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultHistoryRecorder implements HistoryRecorder {

	@Inject
	private DataSourceHistory dsHistory;

	public void record(CrawlStatus status) {
		synchronized (dsHistory) {
			dsHistory.addHistory(status.getId().toString(), status.toMap());
			dsHistory.save();
		}
	}
}
