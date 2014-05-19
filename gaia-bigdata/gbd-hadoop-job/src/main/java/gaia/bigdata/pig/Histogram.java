package gaia.bigdata.pig;

import java.io.IOException;
import java.util.Iterator;

import org.apache.pig.EvalFunc;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Histogram extends EvalFunc<Tuple> {
	private static TupleFactory tupleFactory = TupleFactory.getInstance();
	private static Logger log = LoggerFactory.getLogger(Histogram.class);

	public Tuple exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() < 4)) {
			throw new IOException("Invalid input, expecting four values: data bag, min value, max value, number of bins");
		}
		DataBag bag = (DataBag) tuple.get(0);
		double min = ((Number) tuple.get(1)).doubleValue();
		double max = ((Number) tuple.get(2)).doubleValue();
		int nbins = ((Integer) tuple.get(3)).intValue();
		return exec(bag, min, max, nbins);
	}

	public static Tuple exec(DataBag bag, double min, double max, int nbins) throws IOException {
		int width = calculateWidth(min, max, nbins);
		log.info("Histogram properties: min={}, max={}, nbins={}, width={}",
				new Object[] { Double.valueOf(min), Double.valueOf(max), Integer.valueOf(nbins), Integer.valueOf(width) });
		return binData(bag, nbins, width, min);
	}

	protected static int calculateWidth(double min, double max, int nbins) throws IOException {
		if (nbins <= 0) {
			throw new IOException("Must specify a positive number of bins. " + nbins + " is leq zero");
		}
		return (int) Math.ceil((max - min + 1.0D) / nbins);
	}

	protected static Tuple binData(DataBag values, int nbins, int width, double min) throws ExecException {
		try {
			System.err.println(values + " " + nbins + " " + width + " " + min);
			long[] bins = new long[nbins];
			Iterator<Tuple> it = values.iterator();
			double val = 0.0D;
			int bin = 0;
			while (it.hasNext()) {
				Tuple next = (Tuple) it.next();
				val = ((Number) next.get(0)).doubleValue() - min;
				bin = (int) Math.floor(val / width);
				bins[bin] += 1L;
			}
			Tuple tuple = tupleFactory.newTuple(bins.length);
			for (int i = 0; i < bins.length; i++) {
				tuple.set(i, Long.valueOf(bins[i]));
			}
			return tuple;
		} catch (Exception e) {
			log.warn("Exception while processing bins={}, width={}, values={}", new Object[] { Integer.valueOf(nbins),
					Integer.valueOf(width), values });
			throw new ExecException(e.getCause());
		}
	}

	protected static Tuple combine(DataBag bag) throws ExecException {
		Iterator<Tuple> it = bag.iterator();
		Tuple bins = (Tuple) it.next();
		while (it.hasNext()) {
			Tuple next = (Tuple) it.next();
			for (int i = 0; i < bins.size(); i++) {
				bins.set(i, Long.valueOf(((Long) bins.get(i)).longValue() + ((Long) next.get(i)).longValue()));
			}
		}
		return bins;
	}
}
