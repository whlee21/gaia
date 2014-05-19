package gaia.bigdata.analytics;

public abstract class MetricAggregator<T> {
	private T result;
	private Boolean isDouble;

	public MetricAggregator() {
		reset();
	}

	public void reset() {
		result = null;
		isDouble = null;
		doReset();
	}

	public void feed(Number value) {
		if (isDouble == null) {
			if (((value instanceof Byte)) || ((value instanceof Short)) || ((value instanceof Integer))
					|| ((value instanceof Long)))
				isDouble = Boolean.valueOf(false);
			else {
				isDouble = Boolean.valueOf(true);
			}
		}
		doFeed(value);
	}

	protected boolean isDouble() {
		if ((isDouble == null) || (isDouble.booleanValue() == true)) {
			return true;
		}
		return false;
	}

	public T get() {
		if (result == null) {
			result = getResult();
		}
		return result;
	}

	public abstract String getName();

	protected abstract void doReset();

	protected abstract void doFeed(Number paramNumber);

	protected abstract T getResult();
}
