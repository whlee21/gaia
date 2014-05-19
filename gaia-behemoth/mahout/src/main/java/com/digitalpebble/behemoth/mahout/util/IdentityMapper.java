package com.digitalpebble.behemoth.mahout.util;

import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Mapper;

public class IdentityMapper extends Mapper<Writable, Writable, Writable, Writable> {
	@Override
	protected void map(Writable key, Writable value, Context context) throws IOException, InterruptedException {
		context.write(key, value);
	}
}
