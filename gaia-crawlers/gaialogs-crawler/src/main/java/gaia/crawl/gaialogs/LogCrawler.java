package gaia.crawl.gaialogs;

import gaia.crawl.CrawlStatus;
import gaia.crawl.UpdateController;
import gaia.utils.FileUtils;
import gaia.utils.NameAfterFileFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.log4j.Level;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogCrawler implements Runnable {
	public static final int INFINITE_LOOP_CHECK_MAX_NUM_LOOPS = 10;
	public static final String FAILURE_INCLUDE_SUBSTR = "Doc failed: ";
	public static final String SUCCESS_INCLUDE_SUBSTR = "Doc succeeded: ";
	public static final String SKIP_INCLUDE_SUBSTR = "Doc skipped: ";
	public static final String QUERY_INCLUDE_SUBSTR = "req_type=main";
	public final int LEVEL_INCLUDE_MAX_CHAR = 27;

	public final String[] LEVEL_INCLUDE_SUBSTRS = { " " + Level.INFO + " ", " " + Level.WARN + " ",
			" " + Level.ERROR + " ", " " + Level.FATAL + " " };

	private static transient Logger log = LoggerFactory.getLogger(LogCrawler.class);
	private static final long MAX_BYTES_TO_CATCHUP = 4194304L;
	private final String nodeName;
	private final File dir;
	private final UpdateController solr;
	private final Pattern logFileNamePattern;
	private final LogCrawlerCounter status;
	private final DateFormat logDateParser = getLogDateParser();

	private boolean runUntilStopped = false;
	private boolean stopRequested = false;

	private File currentFile = null;
	private final AtomicLong currentFileByteOffset = new AtomicLong(0L);

	private int currentCrawlCurrentFileLoopCounter = 0;

	public static DateFormat getLogDateParser() {
		SimpleDateFormat r = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.US);

		return r;
	}

	private void setNewFile(File newFile) {
		currentFile = newFile;
		currentFileByteOffset.set(0L);
		currentCrawlCurrentFileLoopCounter = 0;
	}

	public boolean getRunUntilStopped() {
		return runUntilStopped;
	}

	public void setRunUntilStopped(boolean runUntilStopped) {
		this.runUntilStopped = runUntilStopped;
	}

	public void requestStop() {
		stopRequested = true;
	}

	public LogCrawler(String nodeName, LogCrawlerCounter status, File logDir, String logFileNamePattern,
			UpdateController solr) {
		this.nodeName = nodeName;
		this.logFileNamePattern = Pattern.compile(logFileNamePattern);
		this.solr = solr;
		this.dir = logDir;
		this.status = status;
	}

	public long getCurrentFileByteOffset() {
		return currentFileByteOffset.get();
	}

	public File getCurrentFile() {
		return currentFile;
	}

	public String toString() {
		return getClass().getSimpleName() + "(" + logFileNamePattern + ") [" + status.toString() + "] @ byte#"
				+ currentFileByteOffset.get() + " of: " + currentFile + " (pass:" + currentCrawlCurrentFileLoopCounter + ")";
	}

	public synchronized void run() {
		try {
			log.debug("Starting Crawl: " + toString());
			crawl();
		} catch (Exception e) {
			status.incrementCounter(CrawlStatus.Counter.Failed);
			log.error("Failed to run LogCrawler", e);
		} finally {
			log.debug("Ending Crawl: " + toString());
		}
	}

	private void crawl() {
		currentCrawlCurrentFileLoopCounter = 0;

		RandomAccessFile raf = null;

		while ((++currentCrawlCurrentFileLoopCounter <= 10) || (getRunUntilStopped())) {
			try {
				if (stopRequested) {
					if (null != raf)
						try {
							raf.close();
						} catch (Exception e) {
							log.error("Failed to close RAF in finally block - " + toString(), e);
						}
					return;
				}
				long seekTo = currentFileByteOffset.get() - 1L;

				if (null == currentFile) {
					setNewFile(pickFirstFile());
					if (null == currentFile) {
						log.debug("no files yet, nothing to do");
						return;
					}
					log.debug("starting new file :" + currentFile);
				}

				long filesize = currentFile.length();
				if (MAX_BYTES_TO_CATCHUP < filesize - currentFileByteOffset.get()) {
					long catchUpAt = filesize - MAX_BYTES_TO_CATCHUP;
					log.warn("skipping ahead from " + currentFileByteOffset.get() + " to " + catchUpAt + " on file: "
							+ currentFile);

					currentFileByteOffset.set(catchUpAt);
					seekTo = catchUpAt;
				} else if (filesize == currentFileByteOffset.get()) {
					File next = firstFileAfter(currentFile);
					if (null == next) {
						if (getRunUntilStopped()) {
							log.trace("done for now, sleeping: " + toString());
							Thread.currentThread();
							Thread.sleep(500L);

							if (null == raf)
								continue;
							try {
								raf.close();
							} catch (Exception e) {
								log.error("Failed to close RAF in finally block - " + toString(), e);
							}
							continue;
						}
						log.trace("done for now: " + toString());

						if (null == raf)
							break;
						try {
							raf.close();
						} catch (Exception e) {
							log.error("Failed to close RAF in finally block - " + toString(), e);
						}
					}
					if (!next.equals(currentFile)) {
						log.info("nothing new past byte " + currentFileByteOffset.get() + " on file: " + currentFile);

						setNewFile(next);
						log.info("switching to file: " + currentFile);

						if (null == raf)
							continue;
						try {
							raf.close();
						} catch (Exception e) {
							log.error("Failed to close RAF in finally block - " + toString(), e);
						}
						continue;
					}
					log.warn("Impossible scenerio, firstFileAfter itself: " + toString());
				}

				raf = new RandomAccessFile(currentFile, "r");
				if (0L < seekTo) {
					raf.seek(seekTo);
					raf.readLine();
				}
				currentFileByteOffset.set(raf.getFilePointer());

				if (raf.length() == currentFileByteOffset.get()) {
					log.warn("Allready at the last byte: " + toString());
					return;
				}
				try {
					String line = null;

					while (null != (line = readLine(raf))) {
						currentFileByteOffset.set(raf.getFilePointer());
						processLine(currentFile, currentFileByteOffset.get(), line);

						if (stopRequested) {
							currentFileByteOffset.set(raf.getFilePointer());
							return;
						}
					}
				} finally {
					currentFileByteOffset.set(raf.getFilePointer());
				}

				log.debug("EOF: " + toString());

				if (null != raf)
					try {
						raf.close();
					} catch (Exception e) {
						log.error("Failed to close RAF in finally block - " + toString(), e);
					}
			} catch (Exception e) {
				log.error("Ex while Tailing: " + toString(), e);
				status.incrementCounter(CrawlStatus.Counter.Failed);
				throw new RuntimeException("Ex while Tailing: " + toString(), e);
			} finally {
				if (null != raf)
					try {
						raf.close();
					} catch (Exception e) {
						log.error("Failed to close RAF in finally block - " + toString(), e);
					}
			}
		}
	}

	private boolean isLevelToProcess(String line) {
		for (String level : LEVEL_INCLUDE_SUBSTRS) {
			int pos = line.indexOf(level);
			if ((0 < pos) && (pos < 27))
				return true;
		}
		return false;
	}

	private void processLine(File file, long offset, String line) {
		if ((-1 != line.indexOf("Doc failed: ")) && (isLevelToProcess(line))) {
			index(parseDocFailLine(file, offset, line));
		} else if ((-1 != line.indexOf("req_type=main")) && (isLevelToProcess(line))) {
			index(parseQueryLine(file, offset, line));
		}
	}

	private SolrInputDocument parseDocFailLine(File file, long offset, String line) {
		SolrInputDocument doc = parseGeneralInfo(file, offset, line);

		doc.addField("logtype", "doc_failed");

		if (!doc.containsKey("title")) {
			return doc;
		}
		String message = doc.getFieldValue("title").toString();
		ParsePosition pp = new ParsePosition(0);

		if (null == parseUntil(message, "Doc failed: ", true, pp))
			return doc;

		parseCoreName(message, pp, doc);

		return doc;
	}

	private SolrInputDocument parseQueryLine(File file, long offset, String line) {
		SolrInputDocument doc = parseGeneralInfo(file, offset, line);

		doc.addField("logtype", "query");

		if (!doc.containsKey("title")) {
			return doc;
		}
		String message = doc.getFieldValue("title").toString();
		ParsePosition pp = new ParsePosition(0);

		parseCoreName(message, pp, doc);

		String path = null;
		if (null != parseUntil(message, "path=", true, pp)) {
			path = parseUntil(message, " ", true, pp);
			if (null != path)
				doc.addField("querypath", path);
		}

		if (null != parseUntil(message, "params={", true, pp)) {
			String params = parseUntil(message, "} ", true, pp);
			if (null != params)
				doc.addField("params", params);

			String handler = null;
			for (String[] pair : parseParamString(params)) {
				doc.addField("attr_" + pair[0], pair[1]);

				if ("q".equals(pair[0])) {
					doc.addField("q_length", Integer.valueOf(pair[1].length()));
				}

				if ("qt".equals(pair[0]))
					handler = pair[1];

			}

			if (null != path) {
				if (("/select/".equals(path)) || ("/select".equals(path)))
					doc.addField("handler", handler);
				else {
					doc.addField("handler", path);
				}
			}
		}

		if (null != parseUntil(message, "hits=", true, pp)) {
			String hits = parseUntil(message, " ", true, pp);
			if (null != hits)
				doc.addField("hits", hits);
		}

		if (null != parseUntil(message, "QTime=", true, pp)) {
			String qtime = parseUntil(message, " ", true, pp);
			if (null != qtime)
				doc.addField("qtime", qtime);
		}

		return doc;
	}

	private SolrInputDocument parseGeneralInfo(File file, long offset, String line) {
		SolrInputDocument doc = new SolrInputDocument();
		String id = file.toString() + "@" + offset;
		if (null != nodeName) {
			id = nodeName + "@@" + id;
			doc.addField("nodeName", nodeName);
		}
		doc.addField("id", id);
		doc.addField("body", line);

		ParsePosition pp = new ParsePosition(0);

		Date logdate = logDateParser.parse(line, pp);
		if (null == logdate)
			return doc;
		eatws(line, pp);
		String logpriority = parseUntil(line, " ", true, pp);
		if (null == logpriority)
			return doc;
		eatws(line, pp);
		String logger = parseUntil(line, " - ", true, pp);
		if (null == logger)
			return doc;
		eatws(line, pp);
		String message = line.substring(pp.getIndex());

		doc.addField("lastModified", logdate);
		doc.addField("logPriority", logpriority);
		doc.addField("logger", logger);
		doc.addField("title", message);

		return doc;
	}

	private static void parseCoreName(String input, ParsePosition pos, SolrInputDocument doc) {
		eatws(input, pos);
		if ((input.length() <= pos.getIndex()) || ('[' != input.charAt(pos.getIndex()))) {
			return;
		}
		if (null == parseUntil(input, "[", true, pos))
			return;

		String core = parseUntil(input, "]", true, pos);

		if (null != core)
			doc.addField("core", core);
	}

	private static void eatws(String input, ParsePosition pos) {
		int p = pos.getIndex();
		int end = input.length();
		while ((p < end) && (Character.isWhitespace(input.charAt(p))))
			p++;
		pos.setIndex(p);
	}

	private static String parseUntil(String input, String expected, boolean skipExpected, ParsePosition pos) {
		int expectedAt = input.indexOf(expected, pos.getIndex());
		if (-1 == expectedAt) {
			return null;
		}
		String res = input.substring(pos.getIndex(), expectedAt);
		pos.setIndex(skipExpected ? expectedAt + expected.length() : expectedAt);
		return res;
	}

	private static List<String[]> parseParamString(String params) {
		if (null == params)
			return Collections.emptyList();

		List result = new ArrayList(10);
		String[] pairs = params.split("&");
		for (String item : pairs) {
			String[] kv = item.split("=");
			try {
				String key = URLDecoder.decode(kv[0], "UTF-8");
				String val = 1 < kv.length ? URLDecoder.decode(kv[1], "UTF-8") : "";
				result.add(new String[] { key, val });
			} catch (UnsupportedEncodingException e) {
				log.error("UnsupEnc for UTF-8 when decoding URL params", e);
			}
		}
		return result;
	}

	private void index(SolrInputDocument doc) {
		try {
			solr.add(doc);
			status.incrementCounter(CrawlStatus.Counter.New);
		} catch (Exception e) {
			status.incrementCounter(CrawlStatus.Counter.Failed);
			log.error("failure indexing log record", e);
		}
	}

	private File pickFirstFile() throws Exception {
		// return FileUtils.sortAndPickNewestFile(dir.listFiles(new
		// AndFileFilter(CanReadFileFilter.CAN_READ,
		// new RegexFileFilter(logFileNamePattern))));
		Collection<File> files = org.apache.commons.io.FileUtils.listFiles(dir, new AndFileFilter(
				CanReadFileFilter.CAN_READ, new RegexFileFilter(logFileNamePattern)), null);
		return FileUtils.sortAndPickNewestFile(files.toArray(new File[0]));
	}

	private File firstFileAfter(File previous) {
		Collection<File> files = org.apache.commons.io.FileUtils.listFiles(
				dir,
				new AndFileFilter(Arrays.asList(new IOFileFilter[] { CanReadFileFilter.CAN_READ,
						new RegexFileFilter(logFileNamePattern), new NameAfterFileFilter(previous) })), null);
		return FileUtils.sortAndPickOldestFile(files.toArray(new File[0]));
	}

	public static String readLine(RandomAccessFile raf) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(512);
		long startPos = raf.getFilePointer();
		int lastByte = -1;
		while ((-1 != (lastByte = raf.read())) && (10 != lastByte)) {
			if (13 == lastByte) {
				if (10 == raf.read())
					break;
				raf.seek(raf.getFilePointer() - 1L);
				break;
			}

			buf.write(lastByte);
		}

		return raf.getFilePointer() == startPos ? null : buf.toString("UTF-8");
	}
}
