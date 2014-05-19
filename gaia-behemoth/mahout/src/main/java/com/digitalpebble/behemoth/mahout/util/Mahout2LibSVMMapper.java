package com.digitalpebble.behemoth.mahout.util;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;

public class Mahout2LibSVMMapper extends Mapper<Text, VectorWritable, Text, Text> {

	@Override
	protected void map(Text key, VectorWritable value, Context context) throws IOException, InterruptedException {
		Vector v = value.get();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < v.size(); i++) {
			Element el = v.getElement(i);
			int index = el.index();
			// increment index so that starts at 1
			index++;
			double weight = el.get();
			if (weight != 0)
				buffer.append(" ").append(index).append(":").append(weight);
		}
		String rep = buffer.toString();
		if (rep.length() > 0)
			context.write(key, new Text("VECTOR_" + rep));
	}

}
