package gaia.bigdata.hadoop.ingest;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.mapreduce.Job;

public abstract class AbstractJobFixture implements Closeable {
	public abstract void init(Job job) throws IOException;

	public void close() throws IOException {
	}
}
