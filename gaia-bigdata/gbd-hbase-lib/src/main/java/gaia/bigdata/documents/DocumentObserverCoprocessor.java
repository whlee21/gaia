package gaia.bigdata.documents;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import kafka.common.NoBrokersForPartitionException;
import kafka.javaapi.producer.Producer;
import kafka.message.Message;
import kafka.producer.ProducerConfig;

import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.hbase.ValueSerializer;

public class DocumentObserverCoprocessor extends BaseRegionObserver implements DocumentObserverCoprocessorMBean {
	private static final transient Logger log = LoggerFactory.getLogger(DocumentObserverCoprocessor.class);

	private static final ValueSerializer<DocumentKey> keySerializer = new DocumentKeySerializer();
	public static final String TOPIC = "document-updates";
	private Producer<String, Message> producer;
	private final ObjectName objName;
	private final AtomicLong putCount = new AtomicLong(0L);
	private final AtomicLong deleteCount = new AtomicLong(0L);
	private final AtomicLong kafkaExceptionCount = new AtomicLong(0L);
	private final AtomicLong timeOfLastOp = new AtomicLong(0L);
	private final AtomicBoolean isAlive = new AtomicBoolean(false);

	public DocumentObserverCoprocessor() {
		try {
			objName = new ObjectName("gaia:type=DocumentObserverCoprocessor");
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}

	public void start(CoprocessorEnvironment env) throws IOException {
		log.info("Starting DocumentObserverCoprocessor");
		try {
			Properties props = new Properties();
			props.put("zk.connect", env.getConfiguration().get("kafka.zk.connect"));
			ProducerConfig config = new ProducerConfig(props);
			producer = new Producer<String, Message>(config);
		} catch (Exception e) {
			kafkaExceptionCount.getAndIncrement();
			throw new IOException("Could not initialize Kafka producer", e);
		}
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			if (mbs.isRegistered(objName)) {
				mbs.unregisterMBean(objName);
			}
			mbs.registerMBean(this, objName);
		} catch (Exception e) {
			log.error("Unable to register mbean", e);
		}

		isAlive.set(true);
	}

	public void stop(CoprocessorEnvironment env) throws IOException {
		log.info("Stopping DocumentObserverCoprocessor");
		producer.close();
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			if (mbs.isRegistered(objName))
				mbs.unregisterMBean(objName);
		} catch (Exception e) {
			log.error("Unable to unregister mbean", e);
		}
		isAlive.set(false);
	}

	public void postPut(ObserverContext<RegionCoprocessorEnvironment> env, Put put, WALEdit edit, boolean writeToWAL)
			throws IOException {
		byte[] tableName = ((RegionCoprocessorEnvironment) env.getEnvironment()).getRegion().getRegionInfo().getTableName();
		if (!Bytes.equals(tableName, DocumentTable.TABLE)) {
			return;
		}
		log.debug("Got PUT: {}", put);
		putCount.getAndIncrement();
		sendUpdateForMutation(put);
	}

	public void postDelete(ObserverContext<RegionCoprocessorEnvironment> env, Delete delete, WALEdit edit,
			boolean writeToWAL) throws IOException {
		byte[] tableName = ((RegionCoprocessorEnvironment) env.getEnvironment()).getRegion().getRegionInfo().getTableName();
		if (!Bytes.equals(tableName, DocumentTable.TABLE)) {
			return;
		}
		log.debug("Got DELETE: {}", delete);
		deleteCount.getAndIncrement();
		sendUpdateForMutation(delete);
	}

	private void sendUpdateForMutation(Mutation mutation) throws IOException {
		timeOfLastOp.set(System.currentTimeMillis());
		Set<byte[]> families = mutation.getFamilyMap().keySet();

		if ((families.contains(DocumentTable.TEXT_CF)) || (families.contains(DocumentTable.ANNOTATION_CF))
				|| (families.contains(DocumentTable.FIELD_CF))) {
			DocumentKey key = (DocumentKey) keySerializer.toObject(mutation.getRow());
			try {
				Message msg = new Message(keySerializer.toBytes(key));
				// TODO:
				// producer.send(new KeyedMessage<String, Message>("document-updates",
				// msg));
				log.debug("Sending '{}' to topic {}", key.toString(), "document-updates");
			} catch (NoBrokersForPartitionException e) {
				kafkaExceptionCount.getAndIncrement();
				log.error("Could not send message to Kafka, no brokers available", e);
			} catch (Exception e) {
				kafkaExceptionCount.getAndIncrement();
				throw new IOException("Could not send message to Kafka", e);
			}
		}
	}

	public boolean isAlive() {
		return isAlive.get();
	}

	public long getPutCount() {
		return putCount.get();
	}

	public long getDeleteCount() {
		return deleteCount.get();
	}

	public long getKafkaExceptionCount() {
		return kafkaExceptionCount.get();
	}

	public long getTimeSinceLastOp() {
		long t = timeOfLastOp.get();
		if (t == 0L) {
			return -1L;
		}
		return System.currentTimeMillis() - t;
	}

	public void resetCounters() {
		log.info("Resetting stats for DocumentObserverCoprocessor");
		putCount.set(0L);
		deleteCount.set(0L);
		kafkaExceptionCount.set(0L);
	}
}
