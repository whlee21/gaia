package gaia.bigdata.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.commons.lang.StringUtils;

public class MetricAggregators {
	public static MetricAggregator<?> get(String name) {
		if (name.equalsIgnoreCase("sum"))
			return new Sum();
		if (name.equalsIgnoreCase("min"))
			return new Min();
		if (name.equalsIgnoreCase("max"))
			return new Max();
		if (name.equalsIgnoreCase("mean"))
			return new Mean();
		if (name.equalsIgnoreCase("count"))
			return new Count();
		if (name.equalsIgnoreCase("median"))
			return new Median();
		if (name.equalsIgnoreCase("mode"))
			return new Mode();
		if (name.equalsIgnoreCase("var")) {
			return new Variance();
		}

		throw new UnsupportedOperationException("Unknown aggregate function: " + name);
	}

	public static MetricAggregator<?>[] get(String[] names) {
		MetricAggregator<?>[] aggregators = new MetricAggregator<?>[names.length];
		for (int i = 0; i < names.length; i++) {
			aggregators[i] = get(names[i]);
		}
		return aggregators;
	}

	public static class Variance extends MetricAggregator<Number> {
		private double mean = 0.0D;
		private double acc = 0.0D;
		private long cnt = 0L;

		public String getName() {
			return "var";
		}

		protected void doReset() {
			mean = 0.0D;
			acc = 0.0D;
			cnt = 0L;
		}

		protected void doFeed(Number value) {
			cnt += 1L;
			double delta = value.doubleValue() - mean;
			mean += delta / cnt;
			acc += delta * (value.doubleValue() - mean);
		}

		protected Number getResult() {
			if (cnt == 0L)
				return null;
			if (cnt == 1L) {
				return Double.valueOf(0.0D);
			}
			return Double.valueOf(acc / (cnt - 1L));
		}
	}

	public static class Mode extends MetricAggregator<List<Number>> {
		Map<Number, Integer> occ;
		int maxOcc;

		public String getName() {
			return "mode";
		}

		protected void doReset() {
			occ = new HashMap<Number, Integer>();
			maxOcc = 0;
		}

		protected void doFeed(Number value) {
			Integer cnt = (Integer) occ.get(value);
			int inc;
			if (cnt == null)
				inc = 1;
			else {
				inc = cnt.intValue() + 1;
			}
			if (inc > maxOcc) {
				maxOcc = inc;
			}
			occ.put(value, Integer.valueOf(inc));
		}

		protected List<Number> getResult() {
			int max = 0;
			List<Number> maxValues = new ArrayList<Number>();
			for (Map.Entry<Number, Integer> entry : occ.entrySet()) {
				Integer cnt = (Integer) entry.getValue();
				if (cnt.intValue() == max) {
					maxValues.add(entry.getKey());
				} else if (cnt.intValue() > max) {
					max = cnt.intValue();
					maxValues.clear();
					maxValues.add(entry.getKey());
				}
			}
			return maxValues;
		}
	}

	public static class Median extends MetricAggregator<Number> {
		private PriorityQueue<Number> heap;
		private int cnt;

		public String getName() {
			return "median";
		}

		protected void doReset() {
			heap = new PriorityQueue<Number>();
			cnt = 0;
		}

		protected void doFeed(Number value) {
			cnt += 1;
			heap.add(value);
		}

		protected Number getResult() {
			if (cnt == 0) {
				return null;
			}
			int middle = cnt / 2;
			int i = 0;
			while (i < middle - 1) {
				heap.remove();
				i++;
			}
			if (cnt % 2 == 1) {
				heap.remove();
				return (Number) heap.remove();
			}
			return Double
					.valueOf((((Number) heap.remove()).doubleValue() + ((Number) heap.remove()).doubleValue()) / 2.0D);
		}
	}

	public static class Mean extends MetricAggregator<Number> {
		private double acc = 0.0D;
		private long cnt = 0L;

		public String getName() {
			return "mean";
		}

		protected void doReset() {
			cnt = 0L;
			acc = 0.0D;
		}

		protected void doFeed(Number value) {
			acc += value.doubleValue();
			cnt += 1L;
		}

		protected Number getResult() {
			if (cnt == 0L) {
				return null;
			}
			return Double.valueOf(acc / cnt);
		}
	}

	public static class Count extends MetricAggregator<Number> {
		private long cnt = 0L;

		public String getName() {
			return "cnt";
		}

		protected void doReset() {
			cnt = 0L;
		}

		protected void doFeed(Number value) {
			cnt += 1L;
		}

		protected Number getResult() {
			return Long.valueOf(cnt);
		}
	}

	public static class Max extends MetricAggregator<Number> {
		private Number max = null;

		public String getName() {
			return "max";
		}

		public void doReset() {
			max = null;
		}

		public void doFeed(Number value) {
			if ((max == null) || (value.doubleValue() > max.doubleValue()))
				max = value;
		}

		public Number getResult() {
			return max;
		}
	}

	public static class Min extends MetricAggregator<Number> {
		private Number min = null;

		public String getName() {
			return "min";
		}

		public void doReset() {
			min = null;
		}

		public void doFeed(Number value) {
			if ((min == null) || (value.doubleValue() < min.doubleValue()))
				min = value;
		}

		public Number getResult() {
			return min;
		}
	}

	public static class Sum extends MetricAggregator<Number> {
		private Double acc = null;

		public String getName() {
			return "sum";
		}

		public void doReset() {
			acc = null;
		}

		public void doFeed(Number value) {
			if (acc == null)
				acc = Double.valueOf(value.doubleValue());
			else
				acc = Double.valueOf(acc.doubleValue() + value.doubleValue());
		}

		public Number getResult() {
			if (acc == null) {
				return null;
			}
			if (isDouble()) {
				return acc;
			}
			return Long.valueOf(acc.longValue());
		}
	}

	public static class ChainedAggregator extends MetricAggregator<Map<String, Number>> {
		private final MetricAggregator<Number>[] aggregators;

		public ChainedAggregator(MetricAggregator<Number>[] aggregators) {
			this.aggregators = aggregators;
		}

		public void doReset() {
			if (aggregators != null)
				for (MetricAggregator<Number> aggregator : aggregators)
					aggregator.doReset();
		}

		public void feed(Number value) {
			for (MetricAggregator<Number> aggregator : aggregators)
				aggregator.feed(value);
		}

		protected void doFeed(Number value) {
		}

		public Map<String, Number> getResult() {
			Map<String, Number> results = new HashMap<String, Number>(aggregators.length);
			for (MetricAggregator<Number> aggregator : aggregators) {
				results.put(aggregator.getName(), aggregator.getResult());
			}
			return results;
		}

		public String getName() {
			String[] names = new String[aggregators.length];
			for (int i = 0; i < aggregators.length; i++) {
				names[i] = aggregators[i].getName();
			}
			return "chain [" + StringUtils.join(names, ",") + "]";
		}
	}
}
