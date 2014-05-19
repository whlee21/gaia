package gaia.crawl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

public class CrawlStatus {
	private static transient Logger LOG = LoggerFactory.getLogger(CrawlStatus.class);
	public static final String CRAWL_STARTED = "crawl_started";
	public static final String CRAWL_STOPPED = "crawl_stopped";
	public static final String CRAWL_STATE = "crawl_state";
	public static final String CRAWL_JOB_ID = "job_id";
	public static final String BATCH_JOB = "batch_job";
	public static final String DATASOURCE_ID = "id";
	public static final String MESSAGE = "message";
	public static final String EXCEPTION = "exception";
	public static final String NUM_NEW = Counter.New.toString();
	public static final String NUM_UNCHANGED = Counter.Unchanged.toString();
	public static final String NUM_UPDATED = Counter.Updated.toString();
	public static final String NUM_DELETED = Counter.Deleted.toString();
	public static final String NUM_FAILED = Counter.Failed.toString();
	public static final String NUM_TOTAL = Counter.Total.toString();
	public static final String NUM_NOT_FOUND = Counter.Not_Found.toString();
	public static final String NUM_FILTER_DENIED = Counter.Filter_Denied.toString();
	public static final String NUM_ACCESS_DENIED = Counter.Access_Denied.toString();
	public static final String NUM_ROBOTS_DENIED = Counter.Robots_Denied.toString();
	private CrawlId id;
	private DataSourceId dsId;
	private JobState state = JobState.IDLE;
	private String message;
	private long startTimeMs;
	private long endTimeMs;
	private long tookMs;
	private boolean batchJob = false;
	private boolean finished = false;
	private Exception finishedStack = null;
	private final Map<Counter, AtomicLong> counters = new TreeMap<Counter, AtomicLong>();
	private AtomicLong totalCount;
	private Throwable exception;
	private Set<FinishListener> finishListeners = new HashSet<FinishListener>();

	private CrawlStatus() {
		for (Counter c : Counter.values()) {
			counters.put(c, new AtomicLong(0L));
		}
		totalCount = ((AtomicLong) counters.get(Counter.Total));
		reset();
	}

	public CrawlStatus(CrawlId id, DataSourceId dsId) {
		this();
		this.id = id;
		this.dsId = dsId;
	}

	public synchronized void reset() {
		startTimeMs = -1L;
		endTimeMs = -1L;
		tookMs = -1L;
		exception = null;
		finished = false;
		setMessage(null);
		for (Counter c : Counter.values()) {
			((AtomicLong) counters.get(c)).set(0L);
		}
		setState(JobState.IDLE);
	}

	public synchronized void addFinishListener(FinishListener fl) {
		finishListeners.add(fl);
	}

	public long getCounter(Counter c) {
		if (c == Counter.Total) {
			updateTotalCount();
		}
		return ((AtomicLong) counters.get(c)).get();
	}

	public void setCounter(Counter c, long val) {
		AtomicLong cnt = (AtomicLong) counters.get(c);
		cnt.set(val);
	}

	public long incrementCounter(Counter c) {
		return incrementCounter(c, 1L);
	}

	public long incrementCounter(Counter c, long val) {
		AtomicLong cnt = (AtomicLong) counters.get(c);
		return cnt.addAndGet(val);
	}

	public synchronized void setState(JobState state) {
		this.state = state;
	}

	public synchronized void starting() {
		reset();
		startTimeMs = System.currentTimeMillis();
		setState(JobState.STARTING);
	}

	public void running() {
		if (state == JobState.RUNNING) {
			LOG.debug("status already set to RUNNING");
		}
		if (state != JobState.STARTING) {
			starting();
		}
		setState(JobState.RUNNING);
	}

	public boolean isRunning() {
		return (state == JobState.RUNNING) || (state == JobState.STARTING) || (state == JobState.FINISHING)
				|| (state == JobState.ABORTING) || (state == JobState.STOPPING);
	}

	public boolean isOneOf(JobState[] states) {
		for (JobState s : states) {
			if (state == s) {
				return true;
			}
		}
		return false;
	}

	public synchronized void end(JobState status) {
		if (finished) {
			LOG.error(new StringBuilder().append("Called end(").append(status).append(") but already called end(")
					.append(state).append(")").toString());
		}
		if (status == JobState.EXCEPTION) {
			LOG.warn("Job ended with exception", exception);
		}
		finished = true;

		JobState oldState = state;
		if ((status != JobState.FINISHED)
				|| ((oldState != JobState.ABORTED) && (oldState != JobState.EXCEPTION) && (oldState != JobState.STOPPED))) {
			state = status;
		}
		endTimeMs = System.currentTimeMillis();
		if (startTimeMs == -1L) {
			startTimeMs = endTimeMs;
		}
		SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		isoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		tookMs = (endTimeMs - startTimeMs);
		long tookHours = tookMs / 3600000L;
		long tookMinutes = tookMs % 3600000L / 60000L;
		long tookSeconds = tookMs % 60000L / 1000L;
		long tookMilliseconds = tookMs % 1000L;
		String took = new StringBuilder().append(tookHours < 10L ? "0" : "").append(tookHours).append(":")
				.append(tookMinutes < 10L ? "0" : "").append(tookMinutes).append(":").append(tookSeconds < 10L ? "0" : "")
				.append(tookSeconds).append(".").append(tookMilliseconds < 100L ? "0" : "")
				.append(tookMilliseconds < 10L ? "0" : "").append(tookMilliseconds).toString();

		LOG.info(new StringBuilder()
				.append("end job id: ")
				.append(id)
				.append(" took ")
				.append(took)
				.append(" counters: ")
				.append(countersString())
				.append(" state: ")
				.append(status.toString())
				.append(message != null ? new StringBuilder().append(" message: ").append(message).toString() : "")
				.append(
						exception != null ? new StringBuilder().append(" exception: ").append(exception.toString()).toString() : "")
				.toString());

		if ((message == null) && (exception != null)) {
			message = exception.toString();
		}
		fireFinishListeners();
	}

	private void updateTotalCount() {
		long total = 0L;
		for (Map.Entry<Counter, AtomicLong> e : counters.entrySet())
			if (!((Counter) e.getKey()).equals(Counter.Total)) {
				total += ((AtomicLong) e.getValue()).get();
			}
		totalCount.set(total);
	}

	private String countersString() {
		StringBuilder sb = new StringBuilder();
		updateTotalCount();
		for (Map.Entry<Counter, AtomicLong> e : counters.entrySet()) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(new StringBuilder().append(e.getKey()).append("=").append(e.getValue()).toString());
		}
		return sb.toString();
	}

	public synchronized void failed(Throwable throwable) {
		exception = throwable;

		end(JobState.EXCEPTION);
	}

	public void setException(Throwable throwable) {
		exception = throwable;
		if (exception != null)
			exception.fillInStackTrace();
	}

	public synchronized String toString() {
		return new StringBuilder().append("id=").append(id).append(",dsId=").append(dsId).append(",state=").append(state)
				.append(",startTime=").append(startTimeMs).append(",endTime=").append(endTimeMs).append(",message=")
				.append(message).append(",batchJob=").append(batchJob).append(",counters=").append(countersString()).toString();
	}

	public synchronized JobState getState() {
		return state;
	}

	public synchronized Throwable getException() {
		return exception;
	}

	public synchronized long getStartTime() {
		return startTimeMs;
	}

	public synchronized long getEndTime() {
		return endTimeMs;
	}

	public synchronized String getMessage() {
		return message;
	}

	public synchronized void setMessage(String message) {
		this.message = message;
	}

	public CrawlId getId() {
		return id;
	}

	public DataSourceId getDataSourceId() {
		return dsId;
	}

	public void setBatchJob(boolean batchJob) {
		this.batchJob = batchJob;
	}

	public boolean isBatchJob() {
		return batchJob;
	}

	public synchronized HashMap<String, Object> toMap() {
		updateTotalCount();
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (getId() != null) {
			map.put("job_id", getId().toString());
		}
		if (getDataSourceId() != null) {
			map.put("id", getDataSourceId().toString());
		}
		map.put("batch_job", Boolean.valueOf(batchJob));
		map.put("message", message);
		for (Map.Entry<CrawlStatus.Counter, AtomicLong> e : counters.entrySet()) {
			map.put(e.getKey().toString(), Long.valueOf(e.getValue().get()));
		}
		map.put("crawl_state", getState().toString());
		if (startTimeMs > 0L)
			map.put("crawl_started", StringUtils.formatDate(new Date(startTimeMs)));
		else {
			map.put("crawl_started", null);
		}
		if (endTimeMs > 0L)
			map.put("crawl_stopped", StringUtils.formatDate(new Date(endTimeMs)));
		else {
			map.put("crawl_stopped", null);
		}
		if (exception != null) {
			Throwable t = exception;
			List<Object> stack = new LinkedList<Object>();
			while (t != null) {
				Map<String, Object> tm = new HashMap<String, Object>();
				if (stack.size() > 0)
					tm.put("message", new StringBuilder().append("Caused by: ").append(t.getMessage()).toString());
				else {
					tm.put("message", t.getMessage());
				}
				List<String> stm = new LinkedList<String>();
				for (StackTraceElement ste : t.getStackTrace()) {
					stm.add(ste.toString());
				}
				tm.put("stack", stm);
				stack.add(tm);
				t = t.getCause();
			}
			map.put("exception", stack);
		}
		return map;
	}

	public static CrawlStatus fromMap(Map<String, Object> map) {
		CrawlStatus res = new CrawlStatus();
		String id = (String) map.get("job_id");
		if (id == null) {
			id = "?";
		}
		res.id = new CrawlId(id);
		res.batchJob = StringUtils.getBoolean(map.get("batch_job"), true).booleanValue();
		Object o = map.get("id");
		if (o == null) {
			o = "?";
		}
		res.dsId = new DataSourceId(String.valueOf(o));
		res.message = ((String) map.get("message"));
		for (Counter c : Counter.values()) {
			String key = c.toString();
			if (map.containsKey(c.toString())) {
				res.counters.put(c, new AtomicLong(Long.parseLong(String.valueOf(map.get(key)))));
			}
		}
		String state = (String) map.get("crawl_state");
		if (state != null)
			try {
				JobState s = JobState.valueOf(state);
				res.state = s;
			} catch (Exception e) {
			}
		String time = (String) map.get("crawl_started");
		if (time != null) {
			try {
				Date d = StringUtils.parseDate(time);
				res.startTimeMs = d.getTime();
			} catch (Exception e) {
				res.startTimeMs = -1L;
			}
		}
		time = (String) map.get("crawl_stopped");
		if (time != null) {
			try {
				Date d = StringUtils.parseDate(time);
				res.endTimeMs = d.getTime();
			} catch (Exception e) {
				res.endTimeMs = -1L;
			}
		}
		return res;
	}

	private void fireFinishListeners() {
		for (FinishListener fl : finishListeners)
			fl.finished();
	}

	public static interface FinishListener {
		public abstract void finished();
	}

	public static enum Counter {
		New, Unchanged, Updated, Deleted, Failed, Total, Not_Found, Filter_Denied, Access_Denied, Robots_Denied;

		public String toString() {
			return "num_" + name().toLowerCase(Locale.US);
		}
	}

	public static enum JobState {
		IDLE, STARTING, RUNNING, FINISHING, FINISHED, STOPPING, STOPPED, ABORTING, ABORTED, EXCEPTION, UNKNOWN;
	}
}
