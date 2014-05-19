package gaia.bigdata.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HadoopUtil {
	private static final Logger log = LoggerFactory.getLogger(HadoopUtil.class);

	public static MiniCluster startMiniCluster(File logDir, int nameNodePort, int jobTrackerPort, int taskTrackerPort,
			int numDataNodes, int numTaskTrackers, int numDir) throws Exception {
		Configuration conf = new Configuration();
		conf.set("dfs.webhdfs.enabled", "true");
		conf.set("fs.default.name", new StringBuilder().append("hdfs://localhost:").append(nameNodePort).toString());
		conf.set("mapred.job.tracker", new StringBuilder().append("hdfs://localhost:").append(jobTrackerPort).toString());
		conf.set("jobclient.completion.poll.interval", "100");
		conf.set("jobclient.progress.monitor.poll.interval", "50");
		MiniDFSCluster hdfsCluster = null;
		MiniMRCluster mrCluster = null;
		// hdfsCluster = new MiniDFSCluster(nameNodePort, conf, numDataNodes,
		// true, true, null, null);
		MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
		builder = builder.nameNodePort(nameNodePort);
		builder = builder.numDataNodes(numDataNodes);
		builder = builder.format(true);
		builder = builder.manageNameDfsDirs(true);
		builder = builder.manageDataDfsDirs(true);
		hdfsCluster = builder.build();

		hdfsCluster.waitClusterUp();
		File tmpLogDir = new File(logDir, "hadoopLogs");
		tmpLogDir.deleteOnExit();
		System.setProperty("hadoop.log.dir", tmpLogDir.getAbsolutePath());
		FileSystem fileSys = hdfsCluster.getFileSystem();
		mrCluster = new MiniMRCluster(jobTrackerPort, taskTrackerPort, numTaskTrackers, fileSys.getUri().toString(),
				numDir, null, null, null, new JobConf(conf));

		return new MiniCluster(hdfsCluster, mrCluster, conf);
	}

	public static File getJobFile(File dir, String toMatch) throws IOException {
		File result = null;
		if (dir.exists()) {
			File[] jobs = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return (file.getName().endsWith("job.jar")) || (file.getName().endsWith(".job"));
				}
			});
			for (int i = 0; i < jobs.length; i++) {
				File job = jobs[i];
				JarFile jf = null;
				try {
					jf = new JarFile(job);
					Enumeration<JarEntry> entries = jf.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = (JarEntry) entries.nextElement();
						String name = entry.getName();
						if (name.contains(toMatch))
							return job;
					}
				} finally {
					if (jf != null)
						jf.close();
				}
			}
		} else {
			throw new IOException(new StringBuilder().append("No Hadoop Job Home directory: ").append(dir).toString());
		}
		return result;
	}

	public static File getJobFileByName(File dir, final String toMatch) throws IOException {
		File result = null;

		if (dir.exists()) {
			File[] jobs = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					String name = file.getName();
					return ((name.endsWith("job.jar")) || (name.endsWith(".job"))) && (name.contains(toMatch));
				}
			});
			if (jobs.length > 0)
				return jobs[0];
		} else {
			throw new IOException(new StringBuilder().append("No Hadoop Job Home directory: ").append(dir).toString());
		}
		return result;
	}

	public static Configuration createConfig(String jobTracker, String fsName) throws IOException {
		Configuration conf = new Configuration();
		if (jobTracker != null) {
			conf.set("mapred.job.tracker", jobTracker);
		}
		if (fsName != null) {
			conf.set("fs.default.name", fsName);
		}
		return conf;
	}

	public static Configuration createConfig(File confDir) throws IOException {
		Configuration conf = new Configuration();
		if ((confDir.exists()) && (confDir.isDirectory())) {
			File[] files = confDir.listFiles(new FilenameFilter() {
				public boolean accept(File file, String name) {
					return (name.equals("core-site.xml")) || (name.equals("mapred-site.xml")) || (name.equals("hdfs-site.xml"));
				}
			});
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				conf.addResource(file.toURI().toURL());
			}
		} else {
			log.warn(new StringBuilder().append("Couldn't find ").append(confDir).append(".  Using default configuration.")
					.toString());
		}

		return conf;
	}

	public static Job prepareJob(Path inputPath, Path outputPath, Class<? extends InputFormat> inputFormat,
			Class<? extends Mapper> mapper, Class<? extends Writable> mapperKey, Class<? extends Writable> mapperValue,
			Class<? extends OutputFormat> outputFormat, Configuration conf) throws IOException {
		Job job = new Job(new Configuration(conf));
		Configuration jobConf = job.getConfiguration();

		if (mapper.equals(Mapper.class)) {
			throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
		}
		job.setJarByClass(mapper);

		job.setInputFormatClass(inputFormat);
		jobConf.set("mapred.input.dir", inputPath.toString());

		job.setMapperClass(mapper);
		job.setMapOutputKeyClass(mapperKey);
		job.setMapOutputValueClass(mapperValue);
		job.setOutputKeyClass(mapperKey);
		job.setOutputValueClass(mapperValue);
		jobConf.setBoolean("mapred.compress.map.output", true);
		job.setNumReduceTasks(0);

		job.setOutputFormatClass(outputFormat);
		jobConf.set("mapred.output.dir", outputPath.toString());

		return job;
	}

	public static Job prepareJob(Path inputPath, Path outputPath, Class<? extends InputFormat> inputFormat,
			Class<? extends Mapper> mapper, Class<? extends Writable> mapperKey, Class<? extends Writable> mapperValue,
			Class<? extends Reducer> reducer, Class<? extends Writable> reducerKey, Class<? extends Writable> reducerValue,
			Class<? extends OutputFormat> outputFormat, Configuration conf) throws IOException {
		Job job = new Job(new Configuration(conf));
		Configuration jobConf = job.getConfiguration();

		if (reducer.equals(Reducer.class)) {
			if (mapper.equals(Mapper.class)) {
				throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
			}
			job.setJarByClass(mapper);
		} else {
			job.setJarByClass(reducer);
		}

		job.setInputFormatClass(inputFormat);
		jobConf.set("mapred.input.dir", inputPath.toString());

		job.setMapperClass(mapper);
		if (mapperKey != null) {
			job.setMapOutputKeyClass(mapperKey);
		}
		if (mapperValue != null) {
			job.setMapOutputValueClass(mapperValue);
		}

		jobConf.setBoolean("mapred.compress.map.output", true);

		job.setReducerClass(reducer);
		job.setOutputKeyClass(reducerKey);
		job.setOutputValueClass(reducerValue);

		job.setOutputFormatClass(outputFormat);
		jobConf.set("mapred.output.dir", outputPath.toString());

		return job;
	}

	public static String getCustomJobName(String className, JobContext job, Class<? extends Mapper> mapper,
			Class<? extends Reducer> reducer) {
		StringBuilder name = new StringBuilder(100);
		String customJobName = job.getJobName();
		if ((customJobName == null) || (customJobName.trim().isEmpty()))
			name.append(className);
		else {
			name.append(customJobName);
		}
		name.append('-').append(mapper.getSimpleName());
		name.append('-').append(reducer.getSimpleName());
		return name.toString();
	}

	public static void delete(Configuration conf, Iterable<Path> paths) throws IOException {
		if (conf == null) {
			conf = new Configuration();
		}
		for (Path path : paths) {
			FileSystem fs = path.getFileSystem(conf);
			if (fs.exists(path)) {
				log.info("Deleting {}", path);
				fs.delete(path, true);
			}
		}
	}

	public static void delete(Configuration conf, Path[] paths) throws IOException {
		delete(conf, Arrays.asList(paths));
	}

	public static InputStream openStream(Path path, Configuration conf) throws IOException {
		FileSystem fs = FileSystem.get(path.toUri(), conf);
		return fs.open(path.makeQualified(fs.getUri(), fs.getWorkingDirectory()));
	}

	public static void cacheFiles(Path fileToCache, Configuration conf) {
		DistributedCache.setCacheFiles(new URI[] { fileToCache.toUri() }, conf);
	}

	public static Path cachedFile(Configuration conf) throws IOException {
		return new Path(DistributedCache.getCacheFiles(conf)[0].getPath());
	}

	public static void setSerializations(Configuration conf) {
		conf.set("io.serializations",
				"org.apache.hadoop.io.serializer.JavaSerialization,org.apache.hadoop.io.serializer.WritableSerialization");
	}
}
