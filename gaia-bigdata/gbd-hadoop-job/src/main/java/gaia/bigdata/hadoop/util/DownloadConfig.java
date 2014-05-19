package gaia.bigdata.hadoop.util;

public class DownloadConfig {
	String bucket;
	String output;
	String prefix;
	int threadCount;
	int bufferSize;
	Class<? extends S3DownloadWriter> writer;
	int syncCount = 50000;

	public DownloadConfig(String bucket, String output, String prefix, int threadCount, int bufferSize,
			Class<? extends S3DownloadWriter> writer) {
		this.bucket = bucket;
		this.output = output;
		this.prefix = prefix;
		this.threadCount = threadCount;
		this.bufferSize = bufferSize;
		this.writer = writer;
	}
}
