package gaia.api;

import gaia.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class ClickLoggingContext {
	private static final Logger LOG = LoggerFactory.getLogger(ClickLoggingContext.class);

	public ConcurrentHashMap<String, Appender> appenders = new ConcurrentHashMap<String, Appender>();
	public List<Map<String, Object>> buffer = new LinkedList<Map<String, Object>>();
	public volatile boolean buffering = false;
	public volatile int clickCount = 0;
	public volatile int queryCount = 0;

	public synchronized void flushBuffer() throws IOException {
		for (Map<String, Object> ev : buffer) {
			log((String) ev.get("coll_name"), ev);
		}
		buffer.clear();
	}

	public void log(String collection, Map<String, Object> ev) throws IOException {
		if (collection == null) {
			collection = (String) ev.get("coll_name");
		}
		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("coll_name", Error.E_MISSING_VALUE));
		}

		DailyRollingFileAppender ap = (DailyRollingFileAppender) appenders.get(collection);
		if (ap == null) {
			DailyRollingFileAppender newap = new DailyRollingFileAppender(ClickEventServerResource.layout,
					Constants.GAIA_LOGS_HOME + File.separator + "click-" + collection + ".log", "'.'yyyy-MM-dd");

			newap.setName(collection);
			ap = (DailyRollingFileAppender) appenders.putIfAbsent(collection, newap);
			if (ap == null) {
				ap = newap;
			}
		}
		String type = (String) ev.get("type");
		Boolean Buffering = (Boolean) ev.get("buffering");
		if (type == null) {
			if (Buffering == null) {
				throw ErrorUtils.statusExp(422, new Error("type", Error.E_MISSING_VALUE));
			}

			return;
		}

		Number qtime = (Number) ev.get("qt");
		Number ctime = (Number) ev.get("ct");
		Number hits = (Number) ev.get("hits");
		Number pos = (Number) ev.get("pos");
		String reqId = (String) ev.get("req");
		String docId = (String) ev.get("doc");
		String user = (String) ev.get("u");
		String query = (String) ev.get("q");

		if (reqId == null) {
			throw ErrorUtils.statusExp(422, new Error("req", Error.E_MISSING_VALUE));
		}

		if (reqId.indexOf(126) == -1) {
			if (user != null)
				reqId = user + "~" + reqId;
			else {
				reqId = "none~" + reqId;
			}
		}
		String msg = null;
		List<Error> errors = new ArrayList<Error>();
		if (type.equalsIgnoreCase("Q")) {
			if (query == null) {
				errors.add(new Error("q", Error.E_MISSING_VALUE));
			}
			if (qtime == null) {
				errors.add(new Error("qt", Error.E_MISSING_VALUE));
			}
			if (hits == null) {
				errors.add(new Error("hits", Error.E_MISSING_VALUE));
			}
			if (!errors.isEmpty()) {
				throw ErrorUtils.statusExp(422, errors);
			}
			msg = "Q\t" + qtime + "\t" + query + "\t" + reqId + "\t" + hits;

			LoggerFactory.getLogger(ClickLoggingContext.class.getName() + ".Q." + collection).info(msg);
			queryCount += 1;
		} else if (type.equalsIgnoreCase("C")) {
			if (ctime == null) {
				errors.add(new Error("ct", Error.E_MISSING_VALUE));
			}
			if (docId == null) {
				errors.add(new Error("doc", Error.E_MISSING_VALUE));
			}
			if (pos == null) {
				pos = Integer.valueOf(4);
			}
			if (!errors.isEmpty()) {
				throw ErrorUtils.statusExp(422, errors);
			}
			msg = "C\t" + ctime + "\t" + reqId + "\t" + docId + "\t" + pos;

			LoggerFactory.getLogger(ClickLoggingContext.class.getName() + ".C." + collection).info(msg);
			clickCount += 1;
		} else {
			throw ErrorUtils.statusExp(422, new Error("type", Error.E_INVALID_VALUE));
		}

		LoggingEvent le = new LoggingEvent(collection, Category.getInstance(collection), System.currentTimeMillis(),
				Priority.INFO, msg, null);

		ap.append(le);
	}

	public void shutdown() throws Exception {
		try {
			flushBuffer();
		} catch (Throwable t) {
			LOG.warn("Error flushing buffer on finalize", t);
		}
		for (Appender ap : appenders.values()) {
			try {
				ap.close();
			} catch (Exception ioe) {
				LOG.warn("error closing appender", ioe);
			}
		}
		appenders.clear();
	}
}
