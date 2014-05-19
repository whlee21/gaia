package gaia.crawl;

import gaia.Constants;
import gaia.Defaults;
import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceAPI;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerUtils {
	private static final Logger LOG = LoggerFactory.getLogger(CrawlerUtils.class);

	public static CrawlStatus crawl(CrawlerController controller, DataSource ds, CrawlProcessor processor)
			throws Exception {
		CrawlId crawlJobId = new CrawlId(ds.getDataSourceId());

		CrawlStatus status = controller.getStatus(crawlJobId);
		if (status == null) {
			if (processor == null) {
				processor = CrawlProcessor.create(controller, ds);
			}
			controller.defineJob(ds, processor);
		}

		controller.startJob(crawlJobId);
		waitJobStarted(controller, crawlJobId);
		return controller.getStatus(crawlJobId);
	}

	public static List<Error> waitAllJobs(CrawlerControllerRegistry ccr, String crawler, String collection, long timeLimit) {
		List<Error> errors = new ArrayList<Error>();
		long currentTime = System.currentTimeMillis();
		long endTime = currentTime + timeLimit;
		List<CrawlId> jobs = new ArrayList<CrawlId>();
		Collection<CrawlerController> ccs;
		if (crawler != null) {
			CrawlerController cc = ccr.get(crawler);
			if (cc == null) {
				errors.add(new Error(crawler, Error.E_NOT_FOUND, "Crawler type '" + crawler + "' not found."));
				return errors;
			}
			ccs = Collections.singleton(cc);
		} else {
			ccs = ccr.getControllers().values();
		}
		while (currentTime < endTime) {
			jobs.clear();
			for (CrawlerController cc : ccs) {
				List<CrawlStatus> stats;
				try {
					stats = cc.listJobs();
				} catch (Exception e) {
					errors.add(new Error(cc.getClass().getName(), Error.E_EXCEPTION, "Error listing jobs for crawler "
							+ cc.getClass().getName() + ": " + e.toString()));
					continue;
				}

				for (CrawlStatus s : stats)
					if (collection != null) {
						DataSource ds = cc.getDataSourceRegistry().getDataSource(s.getDataSourceId());
						if (ds == null) {
							errors.add(new Error("id", Error.E_NOT_FOUND, "can't find datasource for a job " + s));
						} else if (!collection.equals(ds.getCollection()))
							;
					} else if (cc.jobIsActive(s.getId())) {
						jobs.add(s.getId());
					}
			}
			if (jobs.isEmpty()) {
				break;
			}
		}
		if (currentTime > endTime) {
			errors.add(new Error("waitAllJobs", Error.E_TIMED_OUT, "Time out waiting for jobs to finish, running jobs: "
					+ jobs));
		}
		return errors;
	}

	public static List<Error> finishAllJobs(CrawlerControllerRegistry ccr, String crawler, String collection,
			boolean abort, long timeLimit) {
		List<Error> errors = new ArrayList<Error>();
		Collection<CrawlerController> ccs;
		if (crawler != null) {
			CrawlerController cc = ccr.get(crawler);
			if (cc == null) {
				errors.add(new Error(crawler, Error.E_NOT_FOUND, "Crawler type '" + crawler + "' not found."));
				return errors;
			}
			ccs = Collections.singleton(cc);
		} else {
			ccs = ccr.getControllers().values();
		}
		for (CrawlerController cc : ccs) {
			List<CrawlStatus> stats;
			try {
				stats = cc.listJobs();
			} catch (Exception e) {
				errors.add(new Error(cc.getClass().getName(), Error.E_EXCEPTION, "Error listing jobs for crawler "
						+ cc.getClass().getName() + ": " + e.toString()));
				continue;
			}

			for (CrawlStatus s : stats)
				if (collection != null) {
					DataSource ds = cc.getDataSourceRegistry().getDataSource(s.getDataSourceId());
					if (ds == null) {
						errors.add(new Error("id", Error.E_NOT_FOUND, "can't find datasource for a job " + s));
					} else if (!collection.equals(ds.getCollection()))
						;
				} else if (cc.jobIsActive(s.getId())) {
					try {
						if (abort)
							cc.abortJob(s.getId());
						else
							cc.stopJob(s.getId());
					} catch (Exception e) {
						errors.add(new Error(s.getId().toString(), Error.E_EXCEPTION, "exception stopping job " + s.getId() + ": "
								+ e.toString()));
					}
				}
		}
		if ((timeLimit > 0L) && (errors.isEmpty())) {
			return waitAllJobs(ccr, crawler, collection, timeLimit);
		}
		return errors;
	}

	public static void waitJob(CrawlerController cc, CrawlId id) throws Exception {
		waitJob(cc, id, -1L, 500);
	}

	public static void waitJob(CrawlerController cc, CrawlId id, long timeLimit) throws Exception {
		waitJob(cc, id, timeLimit, 500);
	}

	public static void waitJob(CrawlerController cc, CrawlId id, long timeLimit, int sleepInterval) throws Exception {
		long startTimeLimit = 5000L;
		cc.waitJobStarted(id, startTimeLimit);
		CrawlStatus status = cc.getStatus(id);
		CrawlStatus.JobState jobState = status.getState();
		if (!status.isRunning()) {
			LOG.debug("Job already finished without waiting: " + id + " state: " + jobState);
			if (jobState == CrawlStatus.JobState.EXCEPTION) {
				String message = "Job aborted with exception: " + status.getException();
				LOG.error(message, status.getException());

				return;
			}
			return;
		}
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit;

		while ((timeLimit <= 0L) || (currentTime < endTime)) {
			jobState = status.getState();
			if (status.isRunning()) {
				LOG.debug("Job " + id + " state: " + jobState.toString());
			} else {
				LOG.debug("Job " + id + " completed with status " + jobState.toString());
				if (jobState == CrawlStatus.JobState.EXCEPTION) {
					String message = "Job aborted with exception: " + status.getException();
					LOG.error(message, status.getException());

					return;
				}
				return;
			}
			LOG.debug("Sleeping for " + sleepInterval + " ms waiting for job " + id + " to complete");
			Thread.sleep(sleepInterval);
			currentTime = System.currentTimeMillis();
		}
		String message = "Job " + id + " has timed out after " + timeLimit + " ms";
		LOG.error(message);
		throw new Exception(message);
	}

	public static void waitJobStarted(CrawlerController cc, CrawlId id) throws Exception {
		waitJobStarted(cc, id, 10000L);
	}

	public static void waitJobStarted(CrawlerController cc, CrawlId id, long timeLimit) throws Exception {
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit;
		long sleepInterval = 10L;
		while ((timeLimit <= 0L) || (currentTime < endTime)) {
			if (cc.jobHasStarted(id))
				return;
			LOG.debug("Sleeping for " + sleepInterval + " ms waiting for job " + id + " to start");
			Thread.sleep(sleepInterval);
			currentTime = System.currentTimeMillis();
		}
		String message = "Job " + id + " did not start within " + timeLimit + " ms";
		LOG.error(message);
		throw new Exception(message);
	}

	public static String msgDocFailed(String collection, DataSource ds, String url, String message) {
		String s = "Doc failed:  [" + collection + "]";
		if (ds != null) {
			s = s + " dsId=" + ds.getDataSourceId();
		}
		if (url != null) {
			s = s + " url=" + url;
		}
		if (message != null) {
			s = s + " msg=" + message;
		}
		return s;
	}

	public static String msgDocSuccess(String collection, DataSource ds, String url, String message) {
		String s = "Doc succeeded:  [" + collection + "]";
		if (ds != null) {
			s = s + " dsId=" + ds.getDataSourceId();
		}
		if (url != null) {
			s = s + " url=" + url;
		}
		if (message != null) {
			s = s + " msg=" + message;
		}
		return s;
	}

	public static String msgDocSkipped(String collection, DataSource ds, String url, String message) {
		String s = "Doc skipped:  [" + collection + "]";
		if (ds != null) {
			s = s + " dsId=" + ds.getDataSourceId();
		}
		if (url != null) {
			s = s + " url=" + url;
		}
		if (message != null) {
			s = s + " msg=" + message;
		}
		return s;
	}

	public static void fileReachabilityCheck(String path, List<Error> errors) {
		File f = resolveRelativePath(path);
		File parent = f.getParentFile();

		if (!f.exists()) {
			String msg;
			String err;
			if ((parent != null) && (parent.exists())) {
				if (!parent.canExecute()) {
					err = Error.E_FORBIDDEN_VALUE;
					msg = "wrong permissions: parent path " + parent + " cannot be listed (canExecute == false)";
				} else {
					String fname = f.getName();
					String[] fnames = parent.list();
					Set<String> names = fnames != null ? new HashSet<String>(Arrays.asList(fnames)) : Collections
							.<String> emptySet();
					if (names.contains(fname)) {
						if (!f.canRead()) {
							err = Error.E_FORBIDDEN_VALUE;
							msg = "wrong premissions: root path " + path + " exists but cannot be read (canRead == false)";
						} else {
							if ((f.isDirectory()) && (!f.canExecute())) {
								err = Error.E_FORBIDDEN_VALUE;
								msg = "wrong permissions: root path " + path + " exists but cannot be listed (canExecute == false)";
							} else {
								err = Error.E_NOT_FOUND;
								msg = "root path " + path + " doesn't exist";
							}
						}
					} else {
						err = Error.E_NOT_FOUND;
						msg = "root path " + path + " doesn't exist";
					}
				}
			} else {
				err = Error.E_NOT_FOUND;
				msg = "root path " + path + " doesn't exist";
			}
			errors.add(new Error("path", err, msg));
			return;
		}
		if (!f.canRead()) {
			errors.add(new Error("path", Error.E_FORBIDDEN_VALUE, "wrong premissions: root path " + path
					+ " exists but cannot be read (canRead == false)"));
			return;
		}
		if ((f.isDirectory()) && (!f.canExecute())) {
			errors.add(new Error("path", Error.E_FORBIDDEN_VALUE, "wrong permissions: root path " + path
					+ " exists but cannot be listed (canExecute == false)"));
			return;
		}
	}

	public static boolean isMatchingType(String ccType, String dsType, DataSource other) {
		String otherType = other.getType();
		String otherCCType = other.getCrawlerType();
		if ((otherType == null) || (otherCCType == null)) {
			return false;
		}
		return (ccType.equals(otherCCType)) && (dsType.equals(otherType));
	}

	public static File resolveRelativePath(String f) {
		if (f == null) {
			return null;
		}
		if (f.startsWith("file://")) {
			f = f.substring(7);
			try {
				f = URLDecoder.decode(f, "UTF-8");
			} catch (Exception e) {
				LOG.debug("Exception decoding path '" + f + "'", e);
			}
		} else if (f.startsWith("file:")) {
			f = f.substring(5);
			try {
				f = URLDecoder.decode(f, "UTF-8");
			} catch (Exception e) {
				LOG.debug("Exception decoding path '" + f + "'", e);
			}
		}

		File file = new File(f);
		if (!file.isAbsolute())
			file = new File(Defaults.INSTANCE.getString(Defaults.Group.crawlers, DataSourceAPI.FS_CRAWL_HOME,
					Constants.GAIA_DATA_HOME), f);
		try {
			file = file.getCanonicalFile();
		} catch (Exception e) {
			LOG.debug("Exception getting canonical file '" + file + "'", e);
			file = file.getAbsoluteFile();
		}
		return file;
	}
}
