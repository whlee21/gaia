package gaia.bigdata.hadoop.simdoc;

import gaia.bigdata.hadoop.GaiaCounters;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatrixSizeMapper extends Mapper<IntWritable, VectorWritable, NullWritable, NullWritable> {
	private static final Logger log = LoggerFactory.getLogger(MatrixSizeMapper.class);

	private Configuration config;

	@Override
	protected void map(IntWritable key, VectorWritable value, Context context) throws IOException {
		try {
			Vector inputVector = value.get();
			int numberOfColumns = inputVector.size();
			context.getCounter(GaiaCounters.MATRIX_SIZE_COL).increment(numberOfColumns);
			context.getCounter(GaiaCounters.MATRIX_SIZE_ROW).increment(1L);
		} catch (Throwable e) {
			log.error("Error getting the matrix size: ", e);
		}
	}

	@Override
	protected void setup(Context context) {
		config = context.getConfiguration();
	}

	@Override
	protected void cleanup(Context context) throws IOException {
	}
}
