package gaia.bigdata.hadoop.util;

import com.amazonaws.services.s3.model.S3Object;
import java.io.IOException;

public abstract class S3DownloadWriter {
	protected DownloadConfig config;
	protected String writerId;

	public void init(DownloadConfig config, String writerId) throws IOException {
		this.config = config;
		this.writerId = writerId;
	}

	public abstract void write(S3Object paramS3Object) throws IOException;

	public void close() throws IOException {
	}
}
