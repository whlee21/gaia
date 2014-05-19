package com.digitalpebble.behemoth.mahout.util;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;

public class ClusterDocIDMapper extends Mapper<IntWritable, WeightedVectorWritable, Text, Text> {
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
	}

	@Override
	protected void map(IntWritable key, WeightedVectorWritable value, Context context) throws IOException,
			InterruptedException {
		Vector v = value.getVector();
		if (v instanceof NamedVector) {
			String name = ((NamedVector) v).getName();
			if (name != null & name.length() > 2)
				context.write(new Text(name), new Text(key.toString()));
			else
				context.getCounter("ClusterDocIDDumper", "Missing name").increment(1);
		} else
			context.getCounter("ClusterDocIDDumper", "Unnamed vector").increment(1);
	}
}
