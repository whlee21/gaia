package gaia.crawl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawlStateManager {
	protected Map<CrawlId, CrawlState> jobStates = new HashMap<CrawlId, CrawlState>();

	public synchronized CrawlState get(CrawlId id) {
		return (CrawlState) jobStates.get(id);
	}

	public synchronized List<CrawlState> getJobStates() {
		return new ArrayList<CrawlState>(jobStates.values());
	}

	public synchronized void add(CrawlState state) throws Exception {
		if (jobStates.get(state.getId()) != null)
			throw new Exception("State with job id " + state.getId() + " already exists");

		jobStates.put(state.getId(), state);
	}

	public synchronized boolean delete(CrawlId jobId) {
		return jobStates.remove(jobId) != null;
	}

	public synchronized void clear() throws Exception {
		jobStates.clear();
	}

	public synchronized void shutdown() throws Exception {
		for (CrawlState jobState : jobStates.values()) {
			jobState.close();
		}
		clear();
	}
}
