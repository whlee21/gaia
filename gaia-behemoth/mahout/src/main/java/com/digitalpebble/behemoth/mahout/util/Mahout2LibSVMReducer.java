package com.digitalpebble.behemoth.mahout.util;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mahout2LibSVMReducer extends Reducer<Text, Text, Text, Text> {

	private transient static Logger log = LoggerFactory.getLogger(Mahout2LibSVMReducer.class);

	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		// should have two values : a vector and a label
		String label = null;
		String attributes = null;
		for (Text value : values) {
			String t = value.toString();
			if (t.startsWith("VECTOR_")) {
				attributes = t.substring(7);
			} else
				label = t;
		}
		if (label == null) {
			log.info(key.toString() + " does not have label");
		} else if (attributes == null) {
			log.info(key.toString() + " does not have attributes");
		} else {
			context.write(new Text(label), new Text(attributes));
		}
	}
}