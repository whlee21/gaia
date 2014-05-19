package gaia.bigdata.hbase.documents;

public interface DocumentObserverCoprocessorMBean {
	public boolean isAlive();

	public long getPutCount();

	public long getDeleteCount();

	public long getKafkaExceptionCount();

	public long getTimeSinceLastOp();

	public void resetCounters();
}
