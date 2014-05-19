package gaia.bigdata.hadoop.util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import com.digitalpebble.behemoth.BehemothDocument;
import com.google.inject.name.Named;

public abstract class BaseSeqFileWriter extends S3DownloadWriter {
	protected SequenceFile.Writer writer;
	protected FileSystem fs;
	protected Path parent;
	protected String prefix;

	public BaseSeqFileWriter(@Named("prefix") String prefix, FileSystem fs, Path parent) {
		this.prefix = prefix;
		this.fs = fs;
		this.parent = parent;
	}

	public void init(DownloadConfig config, String writerId) throws IOException {
		super.init(config, writerId);
		Configuration conf = new Configuration();
		Path file = new Path(parent, new StringBuilder()
				.append(prefix != null ? new StringBuilder().append(prefix).append("_").toString() : "")
				.append(writerId).append(".seq").toString());
		writer = SequenceFile.createWriter(fs, conf, file, Text.class, BehemothDocument.class);
	}

	public void close() throws IOException {
		writer.close();
	}
}
