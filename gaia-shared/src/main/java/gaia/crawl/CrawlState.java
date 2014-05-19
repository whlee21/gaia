package gaia.crawl;

import gaia.crawl.datasource.DataSource;

public class CrawlState {
	protected CrawlId id;
	protected CrawlStatus status;
	protected CrawlProcessor processor;
	protected DataSource ds;
	protected String description;

	public synchronized void init(DataSource ds, CrawlProcessor processor, final HistoryRecorder historyRecorder)
			throws Exception {
		assert (ds != null);
		this.ds = ds;

		id = new CrawlId(ds.getDataSourceId());

		description = ("Job " + id + " (" + ds.getDisplayName() + ")");

		status = new CrawlStatus(id, ds.getDataSourceId());
		final CrawlStatus fStatus = status;
		status.addFinishListener(new CrawlStatus.FinishListener() {
			public void finished() {
				if (historyRecorder != null)
					historyRecorder.record(fStatus);
			}
		});
		this.processor = processor;
		if (processor != null)
			processor.init(this);
	}

	public void close() throws CrawlException {
	}

	public String toString() {
		return "id=" + id + ",ds=" + ds + ",proc=" + processor + ",status=" + status;
	}

	public synchronized void setId(CrawlId id) {
		this.id = id;
	}

	public synchronized CrawlId getId() {
		return id;
	}

	public synchronized CrawlStatus getStatus() {
		return status;
	}

	public synchronized CrawlProcessor getProcessor() {
		return processor;
	}

	public synchronized String getDescription() {
		return description;
	}

	public synchronized void setDescription(String description) {
		this.description = description;
	}

	public synchronized DataSource getDataSource() {
		return ds;
	}

	public synchronized void setDataSource(DataSource ds) throws Exception {
		this.ds = ds;
		if (processor != null)
			processor.init(this);
	}
}
