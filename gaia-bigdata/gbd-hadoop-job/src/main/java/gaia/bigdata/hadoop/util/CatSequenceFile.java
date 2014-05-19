package gaia.bigdata.hadoop.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;

public class CatSequenceFile {
	public static void main(String[] args) throws Exception {
		Path input = new Path(args[0]);
		Configuration conf = new Configuration();
		FileSystem fs = input.getFileSystem(conf);
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, input, conf);

		Class<?> keyClass = reader.getKeyClass();
		Writable key;
		if (keyClass.equals(NullWritable.class))
			key = NullWritable.get();
		else {
			key = (Writable) keyClass.newInstance();
		}

		Class<?> valueClass = reader.getValueClass();
		Writable value;
		if (valueClass.equals(NullWritable.class))
			value = NullWritable.get();
		else {
			value = (Writable) valueClass.newInstance();
		}
		while (reader.next(key, value))
			System.out.println(key.toString() + "\t" + value.toString());
	}
}
