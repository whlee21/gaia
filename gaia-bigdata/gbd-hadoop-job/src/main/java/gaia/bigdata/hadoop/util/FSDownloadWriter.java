package gaia.bigdata.hadoop.util;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.inject.Inject;
import java.io.IOException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

public class FSDownloadWriter extends S3DownloadWriter {
	FileSystem fs;
	Path parent;

	@Inject
	FSDownloadWriter(FileSystem fs, Path parent) {
		this.fs = fs;
		this.parent = parent;
	}

	public void write(S3Object object) throws IOException {
		S3ObjectInputStream is = object.getObjectContent();
		FSDataOutputStream outputStream = fs.create(new Path(parent, object.getBucketName() + "_"
				+ object.getKey()));
		IOUtils.copyBytes(is, outputStream, config.bufferSize, true);
	}
}
