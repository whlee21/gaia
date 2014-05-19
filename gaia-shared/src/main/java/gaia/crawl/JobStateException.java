package gaia.crawl;

import java.util.Collections;
import java.util.List;

public class JobStateException extends CrawlException {
	private static final long serialVersionUID = -104820054945119358L;
	private List<CrawlId> jobs;

	public JobStateException(String message, CrawlId job) {
		super(message);
		jobs = Collections.singletonList(job);
	}

	public JobStateException(List<CrawlId> jobs) {
		this("some job(s) are running", jobs);
	}

	public JobStateException(String message, List<CrawlId> jobs) {
		super(message);
		this.jobs = jobs;
	}

	public List<CrawlId> getJobs() {
		return jobs;
	}

	public String toString() {
		return super.toString() + " " + jobs;
	}
}
