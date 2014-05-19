package gaia.crawl;

import gaia.Constants;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.OSFileWriter;
import gaia.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public abstract class History {
	private static transient Logger LOG = LoggerFactory.getLogger(History.class);
	private static final int LIST_SIZE = 10;
	public static final String totalRuns = "total_runs";
	public static final String totalTime = "total_time";
	private String filename;
	protected Map<String, List<Map<String, Object>>> history = new HashMap<String, List<Map<String, Object>>>();
	protected Map<String, Map<String, Object>> cumulativeHistory = new HashMap<String, Map<String, Object>>();

	private static final Collection<String> fldNames = new CrawlStatus(new CrawlId(""), new DataSourceId("")).toMap()
			.keySet();

	protected History(String filename) {
		String home = null;
		if (!new File(filename).isAbsolute()) {
			home = Constants.STORAGE_PATH;
		}
		File file = new File(home, filename);
		this.filename = file.getAbsolutePath();

		if (file.exists()) {
			try {
				LOG.info("Loading History state from " + file);
				FileReader reader = new FileReader(file);
				try {
					int i = 0;
					for (Object ob : new Yaml().loadAll(reader)) {
						if (i == 0)
							history = ((Map) ob);
						else if (i == 1)
							cumulativeHistory = ((Map) ob);
						else {
							throw new IllegalStateException("too many objects were stored to yaml");
						}

						i++;
					}
				} finally {
					reader.close();
				}
				LOG.info("Loaded History ");
			} catch (Throwable e) {
				LOG.warn("Error loading History from " + filename + ", history will be reset to empty: " + e);
				history.clear();
				cumulativeHistory.clear();
			}
		}

		if (!upgradeFormat())
			return;
		try {
			file.getParentFile().mkdirs();
			save(filename);
		} catch (IOException e) {
			LOG.error("Can't save YAML history in " + filename);
		}
	}

	protected boolean upgradeFormat() {
		boolean hasChanged = false;

		for (String key : history.keySet()) {
			// key = (String) i$.next();
			List<Map<String, Object>> historyItems = history.get(key);

			for (Map<String, Object> item : historyItems) {
				if ((item.get("activity_started") == null) || (item.get("activity_finished") == null)) {
					if (!item.keySet().containsAll(fldNames)) {
						CrawlStatus s = CrawlStatus.fromMap(item);
						item.clear();
						item.putAll(s.toMap());
						hasChanged = true;
					}
				}
			}
			if (cumulativeHistory.get(key) == null) {
				hasChanged = true;

				for (Map<String, Object> item : historyItems)
					aggregate(key, item);
			}
		}
		for (Map<String, Object> item : cumulativeHistory.values()) {
			for (CrawlStatus.Counter c : CrawlStatus.Counter.values()) {
				if (!item.containsKey(c.toString())) {
					item.put(c.toString(), Integer.valueOf(0));
					hasChanged = true;
				}
			}
		}
		return hasChanged;
	}

	public synchronized List<Map<String, Object>> getHistory(String key) {
		List<Map<String, Object>> list = history.get(key);
		if (list == null) {
			return null;
		}

		return Collections.unmodifiableList(list);
	}

	public synchronized Map<String, Object> getCumulativeHistory(String key) {
		Map<String, Object> res = cumulativeHistory.get(key);
		if (res == null) {
			return Collections.unmodifiableMap(initEmptyItem());
		}

		Map<String, Object> processed = new HashMap<String, Object>();
		for (String item : res.keySet()) {
			processed.put(item, Long.valueOf(((Number) res.get(item)).longValue()));
		}

		return Collections.unmodifiableMap(processed);
	}

	public synchronized void removeHistory(String key) {
		history.remove(key);
		cumulativeHistory.remove(key);
	}

	public synchronized void save(String filename) throws IOException {
		LOG.info("Dumping History state to   " + filename);
		File file = new File(filename);
		File parent = file.getParentFile();
		if ((parent != null) && (!file.getParentFile().exists())) {
			LOG.error("Cannot save history file - location does not exist:" + file.getAbsolutePath());

			return;
		}
		OSFileWriter fw = new OSFileWriter(file);
		FileOutputStream fos = new FileOutputStream(fw.getWriteFile());
		OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
		try {
			Yaml yaml = new Yaml();
			List<Object> dumpList = new ArrayList<Object>(2);
			dumpList.add(history);
			dumpList.add(cumulativeHistory);
			yaml.dumpAll(dumpList.iterator(), writer);
		} finally {
			writer.close();
			fos.close();
			fw.flush();
		}
	}

	public void save() {
		try {
			save(filename);
		} catch (IOException e) {
			LOG.error("Can't save YAML history in " + filename);
			throw new RuntimeException("Can't save YAML history in " + filename, e);
		}
	}

	public synchronized void addHistory(String key, HashMap<String, Object> info) {
		List<Map<String, Object>> list = history.get(key);
		if (list == null) {
			list = new LinkedList<Map<String, Object>>();
			history.put(key, list);
		}
		if (list.size() == LIST_SIZE) {
			list.remove(0);
		}
		list.add(info);
		aggregate(key, info);
	}

	private void aggregate(String key, Map<String, Object> item) {
		Map<String, Object> cumulativeCounters = cumulativeHistory.get(key);
		if (cumulativeCounters == null) {
			cumulativeCounters = initEmptyItem();
			cumulativeHistory.put(key, cumulativeCounters);
		}

		cumulativeCounters
				.put("total_runs", Long.valueOf(((Number) cumulativeCounters.get("total_runs")).longValue() + 1L));
		if ((item.get("crawl_started") != null) && (item.get("crawl_stopped") != null))
			try {
				long crawlStarted = StringUtils.parseDate((String) item.get("crawl_started")).getTime();
				long crawlStopped = StringUtils.parseDate((String) item.get("crawl_stopped")).getTime();
				cumulativeCounters.put("total_time",
						Long.valueOf(((Number) cumulativeCounters.get("total_time")).longValue() + crawlStopped - crawlStarted));
			} catch (ParseException e) {
			}
		if ((item.get("activity_started") != null) && (item.get("activity_finished") != null))
			try {
				long activityStarted = StringUtils.parseDate((String) item.get("activity_started")).getTime();
				long activityFinished = StringUtils.parseDate((String) item.get("activity_finished")).getTime();
				cumulativeCounters.put(
						"total_time",
						Long.valueOf(((Number) cumulativeCounters.get("total_time")).longValue() + activityFinished
								- activityStarted));
			} catch (ParseException e) {
			}
		for (CrawlStatus.Counter counter : CrawlStatus.Counter.values()) {
			String cString = counter.toString();
			if (item.get(cString) != null) {
				Number oldVal = (Number) cumulativeCounters.get(cString);
				long oldValue = oldVal != null ? oldVal.longValue() : 0L;
				Number newVal = (Number) item.get(cString);
				long newValue = newVal != null ? newVal.longValue() : 0L;
				cumulativeCounters.put(counter.toString(), Long.valueOf(oldValue + newValue));
			}
		}
	}

	public static Map<String, Object> initEmptyItem() {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("total_runs", Long.valueOf(0L));
		res.put("total_time", Long.valueOf(0L));
		for (CrawlStatus.Counter counter : CrawlStatus.Counter.values()) {
			res.put(counter.toString(), Long.valueOf(0L));
		}
		return res;
	}
}
