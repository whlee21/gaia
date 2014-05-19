package gaia.bigdata.pig;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.LoadStoreCaster;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.joda.time.DateTime;

import gaia.bigdata.hbase.BytesOrJSONSerializer;

public class HBaseMetricConverter implements LoadStoreCaster {
	private static final Log log = LogFactory.getLog(HBaseMetricConverter.class);
	private static final BytesOrJSONSerializer serializer = new BytesOrJSONSerializer();

	public DataBag bytesToBag(byte[] arg0, ResourceFieldSchema arg1) throws IOException {
		throw new UnsupportedOperationException("Sorry, can't do that");
	}

	public String bytesToCharArray(byte[] arg0) throws IOException {
		return (String) serializer.toObject(arg0);
	}

	public Double bytesToDouble(byte[] arg0) throws IOException {
		return Double.valueOf(((Number) serializer.toObject(arg0)).doubleValue());
	}

	public Float bytesToFloat(byte[] arg0) throws IOException {
		return Float.valueOf(((Number) serializer.toObject(arg0)).floatValue());
	}

	public Integer bytesToInteger(byte[] arg0) throws IOException {
		return Integer.valueOf(((Number) serializer.toObject(arg0)).intValue());
	}

	public Long bytesToLong(byte[] arg0) throws IOException {
		return Long.valueOf(((Number) serializer.toObject(arg0)).longValue());
	}

	public Map<String, Object> bytesToMap(byte[] arg0) throws IOException {
		return (Map) serializer.toObject(arg0);
	}

	public Map<String, Object> bytesToMap(byte[] arg0, ResourceSchema.ResourceFieldSchema arg1) throws IOException {
		return (Map) serializer.toObject(arg0);
	}

	public Tuple bytesToTuple(byte[] arg0, ResourceSchema.ResourceFieldSchema arg1) throws IOException {
		throw new UnsupportedOperationException("Sorry, can't do that");
	}

	public byte[] toBytes(DataBag arg0) throws IOException {
		throw new UnsupportedOperationException("Sorry, can't do that");
	}

	public byte[] toBytes(String arg0) throws IOException {
		return serializer.toBytes(arg0);
	}

	public byte[] toBytes(Double arg0) throws IOException {
		return serializer.toBytes(arg0);
	}

	public byte[] toBytes(Float arg0) throws IOException {
		return serializer.toBytes(arg0);
	}

	public byte[] toBytes(Integer arg0) throws IOException {
		return serializer.toBytes(arg0);
	}

	public byte[] toBytes(Long arg0) throws IOException {
		return serializer.toBytes(arg0);
	}

	public byte[] toBytes(Map<String, Object> arg0) throws IOException {
		for (Map.Entry<String, Object> entry : arg0.entrySet()) {
			if ((entry.getValue() instanceof Tuple)) {
				arg0.put(entry.getKey(), ((Tuple) entry.getValue()).getAll());
			}
		}
		return serializer.toBytes(arg0);
	}

	public byte[] toBytes(Tuple tuple) throws IOException {
		List<Object> objs = tuple.getAll();
		for (int i = 0; i < objs.size(); i++) {
			Object obj = objs.get(i);

			if ((obj instanceof Tuple)) {
				objs.set(i, ((Tuple) obj).getAll());
			}
		}
		return serializer.toBytes(objs);
	}

	public byte[] toBytes(DataByteArray arg0) throws IOException {
		throw new UnsupportedOperationException("Sorry, can't do that");
	}

	public Boolean bytesToBoolean(byte[] b) throws IOException {
		throw new UnsupportedOperationException("Sorry, can't do that");
	}

	public byte[] toBytes(Boolean b) throws IOException {
		throw new UnsupportedOperationException("Sorry, can't do that");
	}

	@Override
	public DateTime bytesToDateTime(byte[] arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] toBytes(DateTime arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigInteger bytesToBigInteger(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal bytesToBigDecimal(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] toBytes(BigInteger bi) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] toBytes(BigDecimal bd) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}