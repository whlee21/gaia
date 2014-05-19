package gaia.api;

import gaia.Defaults;
import gaia.solr.click.log.AggregateTool;
import gaia.solr.click.log.BoostTool;
import gaia.solr.click.log.PrepareTool;
import gaia.utils.HadoopUtils;
import gaia.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickAnalysisServerResourceImpl extends ServerResource implements ClickAnalysisResource {
	private static final Logger LOG = LoggerFactory.getLogger(ClickAnalysisResource.class);

	private static Map<String, WorkerThread> workers = new HashMap<String, WorkerThread>();
	private String rootPath;
	private String logsPath;

	public ClickAnalysisServerResourceImpl(Defaults defaults) throws ResourceException {
		rootPath = defaults.getString(Defaults.Group.click, "data_path", ClickAnalysisResource.DEFAULT_DATA_PATH);
		logsPath = defaults.getString(Defaults.Group.click, "logs_path", ClickAnalysisResource.DEFAULT_LOGS_PATH);
	}

	private String getCollection() throws Exception {
		String collection = (String) getRequestAttributes().get("coll_name");
		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("collection", Error.E_MISSING_VALUE));
		}

		return collection;
	}

	private String getPath(String collection, String suffix) {
		return rootPath + "/" + collection + "/" + suffix;
	}

	public Map<String, Object> status() throws Exception {
		String collection = getCollection();
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("data_path", rootPath);
		res.put("logs_path", logsPath);
		res.put("collection", collection);
		res.put("prepare_path", getPath(collection, "prepare"));
		res.put("boost_path", getPath(collection, "boost"));
		res.put("dict_path", getPath(collection, "dict"));
		synchronized (workers) {
			WorkerThread worker = (WorkerThread) workers.get(collection);
			if (worker == null)
				res.put("status", Collections.singletonList("idle (never started)"));
			else {
				res.put("status", worker.getStatus());
			}
		}
		return res;
	}

	public Map<String, Object> process(Map<String, Object> args) throws Exception {
		String collection = getCollection();
		boolean sync = StringUtils.getBoolean(args.get("sync"), true).booleanValue();
		WorkerThread worker = null;
		synchronized (workers) {
			worker = (WorkerThread) workers.get(collection);
			if ((worker != null) && (worker.isAlive())) {
				throw ErrorUtils.statusExp(422, new Error("status", Error.E_INVALID_OPERATION, "already running"));
			}

			boolean dict = StringUtils.getBoolean(args.get("dict"), true).booleanValue();
			worker = new WorkerThread(rootPath, logsPath, collection, dict);
			workers.put(collection, worker);
		}
		if (sync)
			worker.run();
		else {
			worker.start();
		}
		return status();
	}

	public String stop() throws Exception {
		String collection = getCollection();
		WorkerThread worker = null;
		synchronized (worker) {
			worker = (WorkerThread) workers.get(collection);
		}
		if ((worker == null) || (!worker.isAlive())) {
			return "There is no running analysis to stop - ignored.";
		}
		worker.interrupt();
		return "Stopping incomplete analysis for collection " + collection;
	}

	private static class WorkerThread extends Thread {
		private Path logsPath;
		private Path prepPath;
		private Path prepOutDir;
		private Path curBoostDir;
		private Path mergedBoostDir;
		private Path curLogsDir;
		private Path boostHistory;
		private Path logsHistory;
		private Path boostDir;
		private Path dictDir;
		private boolean dict;
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		private SimpleDateFormat statFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
		private FileSystem fs;
		private Configuration conf = new Configuration();
		private List<String> status = new ArrayList<String>();
		private Phase currentPhase;
		private boolean interrupted = false;

		public WorkerThread(String rootPath, String logsPath, String collection, boolean dict) throws IOException {
			setDaemon(true);
			fs = FileSystem.get(conf);
			this.dict = dict;
			Path root = new Path(new StringBuilder().append(rootPath).append("/").append(collection).toString())
					.makeQualified(fs.getUri(), fs.getWorkingDirectory());
			this.logsPath = new Path(logsPath).makeQualified(fs.getUri(), fs.getWorkingDirectory());
			prepPath = new Path(root, "prepare");
			prepOutDir = new Path(prepPath, "out");
			boostDir = new Path(root, "boost");
			mergedBoostDir = new Path(boostDir, "current");
			boostHistory = new Path(boostDir, "boostHistory");
			logsHistory = new Path(boostDir, "logsHistory");

			File jobJar = HadoopUtils.getJobFileByName("click-tools");
			if (jobJar != null)
				conf.set("mapred.jar", jobJar.toString());
			else {
				ClickAnalysisServerResourceImpl.LOG
						.warn("Job jar 'click-tools.job' not found, click analysis won't work on a Hadoop cluster!");
			}
			logStatus(Phase.STARTED, State.RUNNING, "Analysis started.");
		}

		public List<String> getStatus() {
			return status;
		}

		private void logStatus(Phase phase, State state, String message) {
			StringBuilder sb = new StringBuilder();
			sb.append(statFormat.format(new Date()));
			sb.append(new StringBuilder().append(" phase ").append(phase).append(" state ").append(state).toString());
			if (message != null) {
				sb.append(new StringBuilder().append(" : ").append(message).toString());
			}
			status.add(sb.toString());
			currentPhase = phase;
		}

		private String prepareSourceLogs() throws Exception {
			fs.mkdirs(logsHistory);

			FileStatus[] logsStats = fs.listStatus(logsPath);

			if ((logsStats == null) || (logsStats.length == 0)) {
				return null;
			}
			Path[] logs = FileUtil.stat2Paths(logsStats);
			if (logs == null) {
				throw new Exception("Log directory does not exist.");
			}
			List<Path> rolledLogs = new ArrayList<Path>();
			for (Path p : logs) {
				rolledLogs.add(p);
			}
			if (rolledLogs.size() == 0) {
				return null;
			}
			String name = sdf.format(new Date());
			curLogsDir = new Path(logsHistory, name);
			fs.mkdirs(curLogsDir);
			curBoostDir = new Path(boostHistory, name);
			if (fs.exists(curBoostDir)) {
				throw new Exception(new StringBuilder().append("Output dir for incremental boost data exists: ")
						.append(curBoostDir).append(", aborting").toString());
			}
			for (Path p : rolledLogs) {
				if (!fs.rename(p, new Path(curLogsDir, p.getName()))) {
					throw new Exception(new StringBuilder().append("Could not move file ").append(p).append(", aborting")
							.toString());
				}
			}
			return name;
		}

		public void run() {
			try {
				fs.delete(prepPath, true);
				int res = doRun();
				switch (res) {
				case 0:
					logStatus(Phase.FINISHED, State.FINISHED, "Analysis completed OK.");
					break;
				case -1:
					logStatus(currentPhase, State.INTERRUPTED, "Analysis interrupted on user request.");
					break;
				case -2:
					logStatus(currentPhase, State.FINISHED, new StringBuilder().append("Analysis failed - no data in phase ")
							.append(currentPhase).toString());
					break;
				default:
					logStatus(currentPhase, State.FAILED, new StringBuilder().append("Analysis failed with code ").append(res)
							.toString());
				}
			} catch (Throwable t) {
				LOG.error("Exception caught", t);
				logStatus(currentPhase, State.FAILED,
						new StringBuilder().append("Analysis failed, exception caught: ").append(t.toString()).toString());
			} finally {
				try {
					fs.delete(prepPath, true);
				} catch (IOException e) {
					LOG.debug("couldn't clean up prepareDir", e);
				}
			}
		}

		private int doRun() {
			String batchName = null;
			try {
				logStatus(Phase.PREPARE, State.RUNNING, "selecting and moving logs");
				batchName = prepareSourceLogs();
				if (batchName == null) {
					return -2;
				}
				logStatus(Phase.PREPARE, State.RUNNING, new StringBuilder().append("preparing new batch: ").append(batchName)
						.toString());

				ToolRunner.run(conf, new PrepareTool(), new String[] { prepOutDir.toString(), curLogsDir.toString() });
				logStatus(Phase.PREPARE, State.FINISHED, null);
			} catch (Exception e) {
				LOG.error("Pre-processing failed", e);
				logStatus(Phase.PREPARE, State.FAILED, e.toString());
				return 20;
			}
			if (interrupted) {
				return -1;
			}
			try {
				logStatus(Phase.AGGREGATE, State.RUNNING, null);
				ToolRunner
						.run(conf, new AggregateTool(), new String[] { curBoostDir.toString(), "doc", prepOutDir.toString() });
				logStatus(Phase.AGGREGATE, State.FINISHED, null);
			} catch (Exception e) {
				LOG.error("Aggregate failed", e);
				logStatus(Phase.AGGREGATE, State.FAILED, e.toString());
				return 30;
			}
			if (interrupted) {
				return -1;
			}
			try {
				logStatus(Phase.BOOST_MERGE, State.RUNNING, null);
				Path prevDir = new Path(boostDir, "previous");
				if ((fs.exists(mergedBoostDir)) && (!fs.isFile(mergedBoostDir))) {
					if (fs.exists(prevDir)) {
						fs.delete(prevDir, true);
					}
					fs.rename(mergedBoostDir, prevDir);
				}
				fs.delete(mergedBoostDir, true);

				FileStatus[] fstats = fs.listStatus(boostHistory);
				if ((fstats == null) || (fstats.length == 0)) {
					return -2;
				}
				Path[] boosts = FileUtil.stat2Paths(fstats);
				Arrays.sort(boosts);
				Path lastBoost = boosts[(boosts.length - 1)];

				int len = 2;
				if (fs.exists(prevDir))
					len++;
				String[] args = new String[len];

				fs.mkdirs(mergedBoostDir);
				Path boostData = new Path(mergedBoostDir, batchName);
				args[0] = boostData.toString();
				args[1] = lastBoost.toString();
				if (fs.exists(prevDir)) {
					args[2] = prevDir.toString();
				}
				ToolRunner.run(conf, new BoostTool(), args);
				logStatus(Phase.BOOST_MERGE, State.FINISHED, null);
			} catch (Exception e) {
				LOG.error("Boost merging failed", e);
				logStatus(Phase.BOOST_MERGE, State.FAILED, e.toString());
				return 40;
			}

			if (interrupted) {
				return -1;
			}
			if (dict) {
				try {
					logStatus(Phase.DICTIONARY, State.RUNNING, null);
					fs.delete(dictDir, true);
					ToolRunner.run(conf, new AggregateTool(), new String[] { dictDir.toString(), "term", prepOutDir.toString() });
					logStatus(Phase.DICTIONARY, State.FINISHED, null);
				} catch (Exception e) {
					LOG.error("Dictionary aggregation failed", e);
					logStatus(Phase.DICTIONARY, State.FAILED, e.toString());
					return 50;
				}
			}
			return 0;
		}

		public void interrupt() {
			super.interrupt();
			interrupted = true;
			logStatus(currentPhase, State.INTERRUPTED, "stopping incomplete processing");
		}

		private static enum Phase {
			STARTED, PREPARE, AGGREGATE, BOOST_MERGE, DICTIONARY, FINISHED;
		}

		private static enum State {
			IDLE, RUNNING, FINISHED, INTERRUPTED, FAILED;
		}
	}
}
