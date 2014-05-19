package gaia.bigdata.oozie.logs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

import kafka.etl.KafkaETLKey;
import kafka.etl.KafkaETLRequest;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;

public class OozieKafkaPrep {
	public static void main(String[] args) throws Exception {
		String dir = args[0];
		String uri = args[1];
		String topic = args[2];

		Configuration conf = new Configuration();

		Path baseDir = new Path(dir);
		Path offsetPathGlob = new Path(baseDir, "*/offsets_*");
		FileSystem fs = baseDir.getFileSystem(conf);
		FileStatus[] fstats = fs.globStatus(offsetPathGlob);
		Path offsetPath;
		if (fstats.length == 0) {
			System.err.println("No offset files found, starting consuming from the beginning");
			offsetPath = new Path(baseDir, "offsets_init");
			KafkaETLKey dummyKey = new KafkaETLKey();
			SequenceFile.setDefaultCompressionType(conf, SequenceFile.CompressionType.NONE);
			SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, offsetPath, KafkaETLKey.class,
					BytesWritable.class);

			KafkaETLRequest request = new KafkaETLRequest(topic, uri, 0);
			byte[] bytes = request.toString().getBytes("UTF-8");
			writer.append(dummyKey, new BytesWritable(bytes));
			writer.close();
		} else {
			FileStatus mrFstat = fstats[0];
			for (FileStatus fstat : fstats) {
				if (fstat.getModificationTime() > mrFstat.getModificationTime()) {
					mrFstat = fstat;
				}
			}
			offsetPath = mrFstat.getPath();
		}

		Properties kafkaProps = new Properties();
		kafkaProps.put("kafka.server.uri", uri);
		kafkaProps.put("topic", topic);
		ByteArrayOutputStream kakfaPropsOS = new ByteArrayOutputStream();
		kafkaProps.store(kakfaPropsOS, "");

		File file = new File(System.getProperty("oozie.action.output.properties"));
		Properties outProps = new Properties();
		outProps.put("inputPath", fs.makeQualified(offsetPath).toString());

		outProps.put("kafkaProps", kakfaPropsOS.toString());
		outProps.put("tag", Long.toString(new Date().getTime()));
		OutputStream os = new FileOutputStream(file);
		outProps.store(os, "");
		os.close();

		System.exit(0);
	}
}
