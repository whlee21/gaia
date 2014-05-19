package gaia.bigdata.hadoop.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.digitalpebble.behemoth.BehemothDocument;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class RawBehemothSeqFileWriter extends BaseSeqFileWriter {
	@Inject
	public RawBehemothSeqFileWriter(@Named("prefix") String prefix, FileSystem fs, Path parent) {
		super(prefix, fs, parent);
	}

	public void write(S3Object object) throws IOException {
		S3ObjectInputStream is = object.getObjectContent();

		BehemothDocument bDoc = new BehemothDocument();

		ByteArrayOutputStream baos = new ByteArrayOutputStream((int) object.getObjectMetadata().getContentLength());
		IOUtils.copyBytes(is, baos, config.bufferSize, true);
		bDoc.setContent(baos.toByteArray());
		bDoc.setContentType(object.getObjectMetadata().getContentType());
		bDoc.setUrl("s3://" + object.getBucketName() + "/" + object.getKey());
		writer.append(new Text(object.getBucketName() + "_" + object.getKey()), bDoc);
	}
}
