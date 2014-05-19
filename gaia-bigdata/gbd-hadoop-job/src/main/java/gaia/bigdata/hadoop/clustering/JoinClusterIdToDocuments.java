package gaia.bigdata.hadoop.clustering;

import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentKey;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import gaia.bigdata.hbase.documents.DocumentTable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.classify.WeightedVectorWritable;
import org.apache.mahout.clustering.iterator.ClusterWritable;
import org.apache.mahout.common.ClassUtils;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.iterator.sequencefile.PathFilters;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinClusterIdToDocuments extends Mapper<IntWritable, WeightedVectorWritable, NullWritable, NullWritable> {
	private static transient Logger log = LoggerFactory.getLogger(JoinClusterIdToDocuments.class);
	private final DocumentKeySerializer keySerializer;
	protected DocumentTable table;
	private HTableInterface hTable;
	protected HTablePool pool;
	private Configuration hbaseConf;
	private DistanceMeasure measure;
	private Map<Integer, Cluster> centroidsMap;

	public JoinClusterIdToDocuments() {
		keySerializer = new DocumentKeySerializer();

		centroidsMap = new HashMap<Integer, Cluster>();
	}

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		System.setProperty("regions.number", conf.get(ClusterLoader.NUMBER_OF_REGIONS));

		initHbasePool(conf.get(ClusterLoader.ZK_CONNECT));
		if (table == null)
			table = new DocumentTable(pool, hbaseConf);
		hTable = pool.getTable(DocumentTable.TABLE);

		String clustersDir = conf.get("centroids");
		if (clustersDir != null) {
			loadCentroids(conf, clustersDir);
		}

		String distMeasure = conf.get("distance");
		if (distMeasure != null)
			measure = ((DistanceMeasure) ClassUtils.instantiateAs(distMeasure, DistanceMeasure.class));
	}

	private void initHbasePool(String zkConnect) {
		hbaseConf = HBaseConfiguration.create();
		hbaseConf.set("hbase.zookeeper.quorum", zkConnect);
		if (pool == null)
			pool = new HTablePool(hbaseConf, 8, new HTableFactory() {
				public HTableInterface createHTableInterface(Configuration conf, byte[] tableName) {
					HTableInterface table = super.createHTableInterface(conf, tableName);
					if ((table instanceof HTable)) {
						((HTable) table).setAutoFlush(false);
					}
					return table;
				}
			});
	}

	private void loadCentroids(Configuration conf, String clustersDir) {
		Path clustersPath = new Path(clustersDir);
		try {
			FileSystem fileSystem = clustersPath.getFileSystem(conf);
			FileStatus[] statuses = fileSystem.listStatus(clustersPath);
			Path centroids = null;
			log.debug("Looking for centroids in {}", clustersPath);
			for (FileStatus status : statuses) {
				Path path = status.getPath();
				if ((status.isDirectory()) && (path.toString().contains("clusters-")) && (path.toString().contains("final"))) {
					centroids = path;
					break;
				}
			}
			if (centroids != null) {
				log.debug("Loading centroids from: {}", centroids);
				SequenceFileDirIterable<Writable, Writable> centIter = new SequenceFileDirIterable<Writable, Writable>(
						centroids, PathType.LIST, PathFilters.partFilter(), null, false, conf);
				for (Pair pair : centIter) {
					Cluster cluster = ((ClusterWritable) pair.getSecond()).getValue();
					centroidsMap.put(Integer.valueOf(cluster.getId()), cluster);
				}
				log.debug("Loaded {} centroids from {}", Integer.valueOf(centroidsMap.size()), centroids);
			}
		} catch (IOException e) {
			log.error("Exception", e);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException {
		hTable.close();
		table.close();
	}

	@Override
	protected void map(IntWritable clusterId, WeightedVectorWritable weightedVector, Context context) throws IOException {
		Vector vector = weightedVector.getVector();
		DocumentKey docKey;
		if ((vector instanceof NamedVector)) {
			String docKeyAsString = ((NamedVector) vector).getName();
			byte[] docKeyBytes = Base64.decode(docKeyAsString);
			log.debug("Got docKey String: {}", docKeyAsString);
			log.debug("Got docKey bytes: {}", Bytes.toStringBinary(docKeyBytes));
			docKey = keySerializer.toObject(docKeyBytes);
		} else {
			context.getCounter(Counters.NOT_NAMED_VECTOR).increment(1L);
			return;
		}
		double distance = (-1.0D / 0.0D);
		if ((measure != null) && (centroidsMap != null)) {
			Cluster theCluster = (Cluster) centroidsMap.get(Integer.valueOf(clusterId.get()));
			if (theCluster != null)
				distance = measure.distance(vector, theCluster.getCenter());
			else {
				context.getCounter(Counters.INVALID_CLUSTER_ID).increment(1L);
			}
		}
		log.debug("Got document {}", docKey);
		Document doc = table.getDocument(docKey.id, docKey.collection, 4, hTable);
		if (doc == null) {
			context.getCounter(Counters.MISSING_DOCUMENT).increment(1L);
			return;
		}

		doc.fields.put("clusterId", Integer.valueOf(clusterId.get()));
		if (distance != (-1.0D / 0.0D)) {
			doc.fields.put("distanceToCentroid", Double.valueOf(distance));
		}
		table.putDocument(doc, hTable);
		context.getCounter(Counters.CLUSTER_ID_MAPPED).increment(1L);
	}

	public static enum Counters {
		CLUSTER_ID_MAPPED, MISSING_DOCUMENT, NOT_NAMED_VECTOR, INVALID_CLUSTER_ID;
	}
}
