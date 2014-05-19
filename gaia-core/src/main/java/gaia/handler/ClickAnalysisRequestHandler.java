package gaia.handler;

import gaia.Constants;
import gaia.solr.click.BoostDataFileFilter;
import gaia.solr.click.Utils;
import gaia.solr.click.log.AggregateTool;
import gaia.solr.click.log.BoostTool;
import gaia.solr.click.log.PrepareTool;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.util.ToolRunner;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickAnalysisRequestHandler extends RequestHandlerBase implements SolrCoreAware {
	private static final Logger LOG = LoggerFactory.getLogger(ClickAnalysisRequestHandler.class);
	public static final String REQUEST = "request";
	public static final String SYNC = "sync";
	public static final String LOG_DIR = "logDir";
	public static final String PREP_DIR = "prepDir";
	public static final String BOOST_DIR = "boostDir";
	public static final String DICT_DIR = "dictDir";
	public static final String CLICK_LOG_NAME = "clickLogName";
	private File logDir;
	private File prepDir;
	private File boostDir;
	private File dictDir;
	private String clickHome;
	private String logDirString;
	private String clickLogName;
	private String coreName;
	private WorkerThread worker;

	public ClickAnalysisRequestHandler() {
		logDir = null;
		prepDir = null;
		boostDir = null;
		dictDir = null;
		clickHome = null;
		logDirString = null;
		clickLogName = null;
		coreName = null;

		worker = null;
	}

	public void init(NamedList args) {
		super.init(args);
	}

	public String getDescription() {
		return "Processes click-through logs to produce document boost data.";
	}

	public String getVersion() {
		return "$Revision$";
	}

	public String getSource() {
		return "$URL$";
	}

	public void inform(SolrCore core) {
		String logsHome = Constants.GAIA_LOGS_HOME;
		if (!logsHome.endsWith(File.separator)) {
			logsHome = new StringBuilder().append(logsHome).append(File.separator).toString();
		}
		coreName = core.getName();
		if (coreName.equals("")) {
			coreName = core.getCoreDescriptor().getCoreContainer().getDefaultCoreName();
		}
		clickHome = new StringBuilder().append(core.getDataDir()).append(File.separator).toString();
		logDirString = ((String) initArgs.get("logDir"));
		if (StringUtils.isBlank(logDirString)) {
			logDirString = logsHome;
		}
		logDir = new File(logDirString);
		if (!logDir.exists()) {
			logDir = null;
			LOG.warn(new StringBuilder().append("no.logs.directory: ").append(logDirString).toString());
		}
		String prepDirString = new StringBuilder().append(clickHome).append("click-prepare").toString();
		String dictDirString = (String) initArgs.get("dictDir");
		if (dictDirString != null) {
			dictDir = new File(dictDirString);
		}
		clickLogName = new StringBuilder().append("click-").append(coreName).append(".log").toString();
		prepDir = new File(prepDirString);
		String boostDirString = (String) initArgs.get("boostDir");
		if (StringUtils.isBlank(boostDirString)) {
			boostDirString = "click-data";
		}
		boostDir = new File(clickHome, boostDirString);
		LOG.info(new StringBuilder().append("Query and clickthrough logs: ").append(logDir).toString());
		LOG.info(new StringBuilder().append("Intermediate data dir: ").append(prepDir).toString());
		LOG.info(new StringBuilder().append("Boost data dir: ").append(boostDir).toString());
		LOG.info(new StringBuilder().append("Frequent phrase dict output: ").append(dictDir != null ? dictDir : "(none)")
				.toString());
		core.addCloseHook(new CloseHook() {
			public void preClose(SolrCore core) {
				if (worker != null) {
					worker.interrupt();
					worker = null;
				}
			}

			public void postClose(SolrCore arg0) {
			}
		});
	}

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		SolrParams params = req.getParams();
		String op = params.get("request");
		if (op == null) {
			rsp.add("error", "missing request name");
			return;
		}
		Req r = Req.valueOf(op.toUpperCase());
		if (r == null) {
			rsp.add("error", new StringBuilder().append("Unsupported request: '").append(op).append("'").toString());
			return;
		}
		switch (r) {
		// STATUS, PROCESS, STOP;
		case STATUS:
			rsp.add("logDir", logDir);
			rsp.add("prepDir", prepDir);
			rsp.add("boostDir", boostDir);
			rsp.add("dictDir", dictDir);
			if (worker != null) {
				String status = new StringBuilder()
						.append((worker.isAlive()) && (!worker.isInterrupted()) ? "Running: " : "Stopped: ")
						.append(worker.getStatus()).toString();
				rsp.add("processing", status);
			} else {
				rsp.add("processing", "Idle.");
			}
			return;
		case PROCESS:
			process(req, rsp);
			return;
		case STOP:
			if ((worker != null) && (worker.isAlive()) && (!worker.isInterrupted())) {
				rsp.add("result", "Stopping incomplete clickthrough analysis.");
				worker.interrupt();
			} else {
				rsp.add("result", "There is no running analysis to stop - ignored.");
			}
			return;
		}
	}

	public void process(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		if ((worker != null) && (worker.isAlive()) && (!worker.isInterrupted())) {
			rsp.add("error", "Already running a click-through analysis.");
			return;
		}
		if ((logDir == null) || (!logDir.exists())) {
			rsp.add("error",
					new StringBuilder().append("logDir '").append(logDirString).append("' not set or does not exist.").toString());
			return;
		}
		boolean sync = false;
		SolrParams params = req.getParams();
		if ("true".equals(params.get("sync"))) {
			sync = true;
		}
		boolean commit = false;
		if ("true".equals(req.getParams().get("commit"))) {
			commit = true;
		}
		worker = new WorkerThread(logDir, prepDir, boostDir, dictDir, clickLogName, true, !sync, req, commit);

		worker.start();
		if (sync) {
			worker.join();
			String status = worker.getStatus();
			worker = null;
			rsp.add("result", status);
			rsp.add("status", status);
		} else {
			rsp.add("result", "Clickthrough analysis started.");
		}
	}

	private static class WorkerThread extends Thread {
		private static final int STAGE_PREPARE = 0;
		private static final int STAGE_AGG_DOC = 1;
		private static final int STAGE_BOOST = 2;
		private static final int STAGE_AGG_TERM = 3;
		private static final String[] stageName = { "prepare", "aggregate", "boost_calc", "dictionary" };

		int stage = STAGE_PREPARE;
		int maxStages;
		String[] perStageStatus = new String[4];
		private File logDir;
		private File prepDir;
		private File prepOutDir;
		private File curBoostDir;
		private File mergedBoostDir;
		private File curLogsDir;
		private File boostHistory;
		private File logsHistory;
		private File boostDir;
		private File dictDir;
		private File tmpDir;
		private String clickLogName;
		private boolean inclusive;
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		private boolean commit;
		private SolrCore core;
		private boolean interrupted = false;
		private SolrQueryRequest req;

		public WorkerThread(File logDir, File prepDir, File boostDir, File dictDir, String clickLogName, boolean inclusive,
				boolean daemon, SolrQueryRequest req, boolean commit) {
			if (daemon) {
				setDaemon(true);
			}
			if (dictDir != null)
				maxStages = 4;
			else {
				maxStages = 3;
			}
			this.logDir = logDir;
			this.prepDir = prepDir;
			prepOutDir = new File(prepDir, "out");
			this.boostDir = boostDir;
			mergedBoostDir = new File(boostDir, "current");
			boostHistory = new File(boostDir, "boostHistory");
			logsHistory = new File(boostDir, "logsHistory");
			this.dictDir = dictDir;
			this.clickLogName = clickLogName;
			this.inclusive = inclusive;
			this.core = req.getCore();
			this.req = req;
			this.commit = commit;
			String tmp = System.getProperty("java.io.tmpdir", "/tmp");
			tmpDir = new File(new StringBuilder().append(tmp).append(File.separator).append("click-")
					.append(System.nanoTime()).toString());
		}

		public String getStatus() {
			StringBuilder sb = new StringBuilder();
			if (interrupted) {
				sb.append("Interrupted (shutting down) ");
			}
			sb.append(new StringBuilder().append("Stage ").append(stage + 1).append("/").append(maxStages).append(":")
					.toString());
			for (int i = 0; i < maxStages; i++) {
				sb.append(new StringBuilder().append(" ").append(stageName[i]).append("=").toString());
				if (perStageStatus != null)
					sb.append(perStageStatus[i]);
				else {
					sb.append(State.idle);
				}
			}
			return sb.toString();
		}

		private void setState(int stageNum, State state, String msg) {
			stage = stageNum;
			perStageStatus[stageNum] = new StringBuilder().append(state.toString())
					.append(msg != null ? new StringBuilder().append(", ").append(msg).toString() : "").toString();
			LOG.info(new StringBuilder().append("Status: ").append(getStatus()).toString());
		}

		private void error(String msg) {
			perStageStatus[stage] = new StringBuilder().append(State.failed).append(": ").append(msg).toString();
			LOG.info(new StringBuilder().append("Error: ").append(getStatus()).toString());
		}

		private String prepareSourceLogs() throws Exception {
			logsHistory.mkdirs();

			File[] logs = logDir.listFiles(new ClickLogFilter(clickLogName, inclusive));
			if (logs == null) {
				throw new Exception("Log directory does not exist.");
			}
			List<File> rolledLogs = new ArrayList<File>();
			File current = null;
			for (File f : logs) {
				if (f.getName().equals(clickLogName)) {
					current = f;
				} else
					rolledLogs.add(f);
			}
			if (rolledLogs.size() == 0) {
				if (inclusive) {
					if ((current == null) || (current.length() == 0L))
						return null;
				} else {
					return null;
				}
			}
			String name = null;
			if (inclusive) {
				name = sdf.format(new Date());
			} else {
				Collections.sort(rolledLogs);
				File lastFull = (File) rolledLogs.get(rolledLogs.size() - 1);
				name = lastFull.getName();
				name = name.substring(clickLogName.length() + 1);
			}
			curLogsDir = new File(logsHistory, name);
			curLogsDir.mkdirs();
			curBoostDir = new File(boostHistory, name);
			if (curBoostDir.exists()) {
				throw new Exception(new StringBuilder().append("Output dir for incremental boost data exists: ")
						.append(curBoostDir).append(", aborting").toString());
			}
			for (File f : rolledLogs) {
				if (!f.renameTo(new File(curLogsDir, f.getName()))) {
					throw new Exception(new StringBuilder().append("Could not move file ").append(f).append(", aborting")
							.toString());
				}
			}
			if ((inclusive) && (current != null) && (current.length() > 0L)) {
				byte[] buf = new byte[1024];

				FileOutputStream fos = new FileOutputStream(new File(curLogsDir, clickLogName));
				InputStream is = new FileInputStream(current);
				int cnt;
				while ((cnt = is.read(buf)) >= 0) {
					fos.write(buf, 0, cnt);
				}
				fos.flush();
				fos.close();
				is.close();

				RandomAccessFile raf = new RandomAccessFile(current, "rw");
				raf.setLength(0L);
				raf.close();
			}
			return name;
		}

		public void run() {
			try {
				FileUtils.deleteDirectory(prepDir);
				doRun();
			} catch (Throwable t) {
				LOG.error("Exception caught", t);
			} finally {
				try {
					FileUtils.deleteDirectory(prepDir);
				} catch (IOException e) {
					LOG.debug("couldn't clean up prepareDir", e);
				}
				if ((tmpDir != null) && (tmpDir.exists())) {
					FileUtil.fullyDelete(tmpDir);
				}
			}
		}

		private void doRun() {
			Configuration conf = Utils.createConfiguration();
			conf.set("hadoop.tmp.dir", tmpDir.getAbsolutePath());
			String batchName = null;
			try {
				setState(0, State.running, null);

				setState(0, State.running, "selecting logs");
				batchName = prepareSourceLogs();
				if (batchName == null) {
					setState(0, State.finished, "no click-through logs for processing.");
					return;
				}
				setState(0, State.running, new StringBuilder().append("preparing new batch: ").append(batchName).toString());

				ToolRunner.run(conf, new PrepareTool(), new String[] { prepOutDir.toString(), curLogsDir.toString() });
				setState(0, State.finished, "ok");
			} catch (Exception e) {
				LOG.error("Pre-processing failed", e);
				error(e.toString());
				return;
			}
			if (interrupted) {
				return;
			}
			try {
				setState(STAGE_AGG_DOC, State.running, null);
				ToolRunner
						.run(conf, new AggregateTool(), new String[] { curBoostDir.toString(), "doc", prepOutDir.toString() });
				setState(STAGE_AGG_DOC, State.finished, "ok");
			} catch (Exception e) {
				LOG.error("Aggregate failed", e);
				error(e.toString());
				return;
			}
			if (interrupted) {
				return;
			}
			try {
				setState(STAGE_BOOST, State.running, null);
				File prevDir = new File(boostDir, "previous");
				if ((mergedBoostDir.exists()) && (mergedBoostDir.isDirectory())) {
					if (prevDir.exists()) {
						FileUtils.deleteDirectory(prevDir);
					}
					mergedBoostDir.renameTo(prevDir);
				}
				FileUtils.deleteDirectory(mergedBoostDir);

				File[] boosts = boostHistory.listFiles();
				if ((boosts == null) || (boosts.length == 0)) {
					setState(STAGE_BOOST, State.finished, "no data to process.");
					return;
				}
				Arrays.sort(boosts);
				File lastBoost = boosts[(boosts.length - 1)];

				int len = 2;
				if (prevDir.exists())
					len++;
				String[] args = new String[len];

				mergedBoostDir.mkdirs();
				File boostData = new File(mergedBoostDir, batchName);
				args[0] = boostData.toString();
				args[1] = lastBoost.toString();
				if (prevDir.exists()) {
					args[2] = prevDir.toString();
				}
				ToolRunner.run(conf, new BoostTool(), args);

				File indexDir = new File(core.getIndexDir());

				File outputDir = new File(new File(indexDir.getParentFile(), "boost"), batchName);

				FileUtils.copyDirectory(boostData, outputDir);

				for (File f : boostData.listFiles()) {
					if (BoostDataFileFilter.INSTANCE.accept(f, f.getName())) {
						copyFile(f, new File(indexDir, f.getName()), true);
					}
				}
				setState(STAGE_BOOST, State.finished, "ok");
			} catch (Exception e) {
				LOG.error("Boost merging failed", e);
				error(e.toString());
				return;
			}
			if (interrupted) {
				return;
			}
			if (dictDir != null) {
				try {
					setState(STAGE_AGG_TERM, State.running, null);
					FileUtils.deleteDirectory(dictDir);
					ToolRunner.run(conf, new AggregateTool(), new String[] { dictDir.toString(), "term", prepOutDir.toString() });
					setState(STAGE_AGG_TERM, State.finished, "ok");
				} catch (Exception e) {
					LOG.error("Dictionary aggregation failed", e);
					error(e.toString());
					return;
				}
			}
			if (commit) {
				LOG.info("Finished processing, calling 'commit' to load new data...");
				try {
					CommitUpdateCommand cmd = new CommitUpdateCommand(req, false);
					core.getUpdateHandler().commit(cmd);
				} catch (Exception e) {
					LOG.error("Commit failed.", e);
				}
			}
		}

		public void interrupt() {
			super.interrupt();
			interrupted = true;
		}

		private static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
			if ((destFile.exists()) && (destFile.isDirectory())) {
				throw new IOException(new StringBuilder().append("Destination '").append(destFile)
						.append("' exists but is a directory").toString());
			}

			FileInputStream input = new FileInputStream(srcFile);
			try {
				FileOutputStream output = new FileOutputStream(destFile);
				try {
					IOUtils.copy(input, output);
				} finally {
				}
			} finally {
				IOUtils.closeQuietly(input);
			}

			if (srcFile.length() != destFile.length()) {
				throw new IOException(new StringBuilder().append("Failed to copy full contents from '").append(srcFile)
						.append("' to '").append(destFile).append("'").toString());
			}

			if (preserveFileDate)
				destFile.setLastModified(srcFile.lastModified());
		}

		private static class ClickLogFilter implements FileFilter {
			private String name;

			public ClickLogFilter(String name, boolean inclusive) {
				if (inclusive)
					this.name = name;
				else
					this.name = (name + ".");
			}

			public boolean accept(File pathname) {
				return (pathname.isFile()) && (pathname.getName().startsWith(name));
			}
		}

		private static enum State {
			running, finished, idle, failed;
		}
	}

	public static enum Req {
		STATUS, PROCESS, STOP;
	}
}
