package gaia.bigdata.hadoop.util;

import gaia.commons.util.CLI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;

public class S3Downloader {
	private static transient Logger log = LoggerFactory.getLogger(S3Downloader.class);
	AmazonS3 s3Client;

	public S3Downloader(String accessKey, String secretKey) throws IOException {
		s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
	}

	public long download(final DownloadConfig config) throws IOException {
		log.info("Downloading: {} to output: {}", config.bucket + " pre: " + config.prefix, config.output);

		Configuration conf = new Configuration();
		final Path outPath = new Path(config.output);
		final FileSystem fs = outPath.getFileSystem(conf);
		fs.mkdirs(outPath);
		ObjectListing files;
		if ((config.prefix != null) && (config.prefix.length() > 0))
			files = s3Client.listObjects(config.bucket, config.prefix);
		else {
			files = s3Client.listObjects(config.bucket);
		}
		Module module = new AbstractModule() {
			protected void configure() {
				bind(S3DownloadWriter.class).to(config.writer);
				bind(FileSystem.class).toInstance(fs);
				bind(Path.class).toInstance(outPath);
				bind(Integer.class).annotatedWith(Names.named("bufferSize")).toInstance(Integer.valueOf(config.bufferSize));
				bind(String.class).annotatedWith(Names.named("prefix")).toInstance(config.bucket + "_" + config.prefix);
			}
		};
		Injector injector = Guice.createInjector(new Module[] { module });
		S3DownloadWriter[] writers = new S3DownloadWriter[config.threadCount];
		for (int i = 0; i < config.threadCount; i++) {
			writers[i] = ((S3DownloadWriter) injector.getInstance(S3DownloadWriter.class));
			writers[i].init(config, String.valueOf(i));
		}
		long total = 0L;
		long count = 0L;

		long startTime = System.currentTimeMillis();
		BigInteger bytesDown = null;
		if (files != null) {
			do {
				List<S3ObjectSummary> list = files.getObjectSummaries();
				if ((list != null) && (!list.isEmpty())) {
					int totalItems = list.size();
					int pageSize = totalItems / config.threadCount;
					DownloadThread[] threads = new DownloadThread[config.threadCount];

					for (int i = 0; i < threads.length; i++) {
						int start = i * pageSize;

						int end = Math.min((i + 1) * pageSize, totalItems);
						threads[i] = new DownloadThread(list.subList(start, end), writers[i]);
						threads[i].start();
					}
					for (int i = 0; i < threads.length; i++) {
						try {
							threads[i].join();
							total += threads[i].itemNum;
							if (bytesDown == null)
								bytesDown = threads[i].downloadSize;
							else
								bytesDown = bytesDown.add(threads[i].downloadSize);
						} catch (InterruptedException e) {
						}
					}
					files = s3Client.listNextBatchOfObjects(files);
					if (count % 5L == 0L) {
						long finish = System.currentTimeMillis();
						log.info(
								"total downloaded: {}, time elapsed: {} ms, rate: {} docs / ms and size: {}",
								new Object[] { Long.valueOf(total), Long.valueOf(finish - startTime),
										Double.valueOf(total / (finish - startTime)), bytesDown.toString() });
					}
					count += 1L;
				}
			} while (files.isTruncated());
		}

		for (int i = 0; i < config.threadCount; i++) {
			writers[i].close();
		}
		log.info("Downloaded {} files from {} with size {}", new Object[] { Long.valueOf(total), config.bucket,
				bytesDown != null ? bytesDown.toString() : "-1" });
		return total;
	}

	public static void main(String[] args) throws Exception {
		CLI cli = new CLI();
		cli.addOutputOption();
		cli.addOption("threads", "t", "The number of threads to use", "8");
		cli.addOption("accessKey", "a", "The AWS Access Key", true);
		cli.addOption("secretKey", "s", "The AWS Secret Key", true);
		cli.addOption("bucket", "b", "The bucket to download");
		cli.addOption("prefix", "p", "The prefix for the bucket");
		cli.addOption("bufferSize", "bf", "Buffer size to use");
		cli.addOption("writer", "w", "The type of writer to use.  Choices are: fs|cc|beh.", "fs");
		Map argMap = cli.parseArguments(args, true, false);
		if (argMap == null) {
			System.out.println("Couldn't parse args: " + Arrays.asList(args));
			return;
		}
		String output = cli.getOption("output");
		String accessKey = cli.getOption("accessKey");
		String secretKey = cli.getOption("secretKey");
		String bucket = cli.getOption("bucket");
		String prefix = cli.getOption("prefix");
		String writerType = cli.getOption("writer");
		Class clazz;
		if (writerType.equalsIgnoreCase("cc")) {
			clazz = CommonCrawlSeqFileWriter.class;
		} else {
			if (writerType.equalsIgnoreCase("beh")) {
				clazz = RawBehemothSeqFileWriter.class;
			} else
				clazz = FSDownloadWriter.class;
		}
		int thrdCnt = Integer.parseInt(cli.getOption("threads", "8"));
		int bufferSize = Integer.parseInt(cli.getOption("bufferSize", "10000"));
		S3Downloader downloader = new S3Downloader(accessKey, secretKey);
		long start = System.currentTimeMillis();
		DownloadConfig config = new DownloadConfig(bucket, output, prefix, thrdCnt, bufferSize, clazz);
		long downloads = downloader.download(config);
		long finish = System.currentTimeMillis();
		log.info("Downloaded: {} in time: {} minutes", Long.valueOf(downloads), Long.valueOf((finish - start) / 60000L));
	}

	private class DownloadThread extends Thread {
		private List<S3ObjectSummary> files;
		public long itemNum = 0L;

		public BigInteger downloadSize = new BigInteger("0");
		private S3DownloadWriter writer;

		private DownloadThread(List<S3ObjectSummary> files, S3DownloadWriter writer) {
			this.files = files;
			this.writer = writer;
		}

		public void run() {
			for (S3ObjectSummary file : files) {
				S3Object object = s3Client.getObject(new GetObjectRequest(file.getBucketName(), file.getKey()));
				ObjectMetadata metadata = object.getObjectMetadata();
				long size = metadata.getContentLength();
				downloadSize = downloadSize.add(new BigInteger(String.valueOf(size)));
				itemNum += 1L;
				try {
					writer.write(object);
				} catch (IOException e) {
					S3Downloader.log.error("Exception, skipping itemNum: " + itemNum, e);
				}
			}
		}
	}
}
