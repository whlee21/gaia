package com.lucid.crawl.behemoth;

import com.digitalpebble.behemoth.BehemothConfiguration;
import com.digitalpebble.behemoth.util.CorpusGenerator.Counters;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectAccessCorpusGenerator extends Configured implements Tool {
	private static transient Logger log = LoggerFactory.getLogger(DirectAccessCorpusGenerator.class);
	private Path input;
	private Path output;
	private Reporter reporter;
	public static String unpackParamName = "CorpusGenerator-unpack";

	private volatile boolean stop = false;

	public DirectAccessCorpusGenerator() {
	}

	public DirectAccessCorpusGenerator(Path input, Path output) {
		setInput(input);
		setOutput(output);
	}

	public DirectAccessCorpusGenerator(Path input, Path output, Reporter reporter) {
		this.input = input;
		this.output = output;
		this.reporter = reporter;
	}

	public void setInput(Path input) {
		this.input = input;
	}

	public void setOutput(Path output) {
		this.output = output;
	}

	public long generate(boolean recurse) throws IOException {
		long result = 0L;

		Text key = new Text();
		Text value = new Text("file");
		SequenceFile.Writer writer = null;
		try {
			Configuration conf = getConf();
			FileSystem fs = this.output.getFileSystem(conf);
			writer = SequenceFile.createWriter(fs, conf, this.output, key.getClass(), value.getClass());

			PerformanceFileFilter pff = new PerformanceFileFilter(writer, key, value, conf, this.reporter);

			result = processFiles(conf, this.input, recurse, pff);
		} finally {
			IOUtils.closeStream(writer);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(BehemothConfiguration.create(), new DirectAccessCorpusGenerator(), args);

		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		Options options = new Options();

		HelpFormatter formatter = new HelpFormatter();

		CommandLineParser parser = new GnuParser();

		options.addOption("h", "help", false, "print this message");
		options.addOption("i", "input", true, "input file or directory");
		options.addOption("o", "output", true, "output Behemoth corpus");
		options.addOption("r", "recurse", true, "processes directories recursively (default true)");

		options.addOption("u", "unpack", true, "unpack content of archives (default true)");

		options.addOption("md", "metadata", true,
				"add document metadata separated by semicolon e.g. -md source=internet;label=public");

		CommandLine line = null;
		try {
			line = parser.parse(options, args);
			if (line.hasOption("help")) {
				formatter.printHelp("CorpusGenerator", options);
				return 0;
			}
			if (!line.hasOption("i")) {
				formatter.printHelp("CorpusGenerator", options);
				return -1;
			}
			if (!line.hasOption("o")) {
				formatter.printHelp("CorpusGenerator", options);
				return -1;
			}
		} catch (ParseException e) {
			formatter.printHelp("CorpusGenerator", options);
		}

		boolean recurse = true;
		if ((line.hasOption("r")) && ("false".equalsIgnoreCase(line.getOptionValue("r"))))
			recurse = false;
		boolean unpack = true;
		if ((line.hasOption("u")) && ("false".equalsIgnoreCase(line.getOptionValue("u")))) {
			unpack = false;
		}
		getConf().setBoolean(unpackParamName, unpack);

		Path inputDir = new Path(line.getOptionValue("i"));
		Path output = new Path(line.getOptionValue("o"));

		if (line.hasOption("md")) {
			String md = line.getOptionValue("md");
			getConf().set("md", md);
		}

		setInput(inputDir);
		setOutput(output);

		long start = System.currentTimeMillis();
		long count = generate(recurse);
		long finish = System.currentTimeMillis();
		if (log.isInfoEnabled()) {
			log.info("CorpusGenerator completed. Timing: " + (finish - start) + " ms");
		}
		log.info(count + " docs converted");
		return 0;
	}

	private long processFiles(Configuration conf, Path input, boolean recurse, PerformanceFileFilter pff)
			throws IOException {
		if (this.stop) {
			return pff.counter;
		}

		FileSystem fs = input.getFileSystem(conf);
		FileStatus[] statuses = fs.listStatus(input, pff);
		for (int i = 0; i < statuses.length; i++) {
			FileStatus status = statuses[i];
			if (recurse == true) {
				processFiles(conf, status.getPath(), recurse, pff);
			}
		}
		return pff.counter;
	}

	public void stop() {
		this.stop = true;
	}

	public boolean isStopped() {
		return this.stop;
	}

	public static String pathToStringEncoded(Path p, FileSystem fs) {
		if (p == null) {
			return null;
		}
		String url = null;
		if (fs != null)
			url = p.makeQualified(fs).toString();
		else {
			url = p.toString();
		}
		if (url == null)
			return null;
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		return url;
	}

	static class PerformanceFileFilter implements PathFilter {
		long counter = 0L;
		PathFilter defaultIgnores = new PathFilter() {
			public boolean accept(Path file) {
				String name = file.getName();
				return !name.startsWith(".");
			}
		};
		private SequenceFile.Writer writer;
		private Text key;
		private Text value;
		private Configuration conf;
		private Reporter reporter;

		public PerformanceFileFilter(SequenceFile.Writer writer, Text key, Text value, Configuration conf, Reporter reporter) {
			this.writer = writer;
			this.key = key;
			this.value = value;
			this.conf = conf;
			this.reporter = reporter;
		}

		public boolean accept(Path file) {
			try {
				FileSystem fs = file.getFileSystem(this.conf);
				boolean unpack = this.conf.getBoolean(DirectAccessCorpusGenerator.unpackParamName, true);

				if ((this.defaultIgnores.accept(file)) && (!fs.getFileStatus(file).isDir())) {
					String uri = DirectAccessCorpusGenerator.pathToStringEncoded(file, fs);
					int processed = 0;

					if (processed == 0) {
						try {
							this.key.set(uri);

							this.value.set("file");
							this.writer.append(this.key, this.value);
							this.counter += 1L;
							if (this.reporter != null)
								this.reporter.incrCounter(CorpusGenerator.Counters.DOC_COUNT, 1L);
						} catch (FileNotFoundException e) {
							log.warn("File not found " + file + ", skipping: " + e);
						} catch (IOException e) {
							log.warn("IO error reading file " + file + ", skipping: " + e);
						}

					}

				}

				return fs.getFileStatus(file).isDir();
			} catch (IOException e) {
				log.error("Exception", e);
			}
			return false;
		}
	}
}
