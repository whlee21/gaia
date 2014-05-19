package gaia.solr.click;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.solr.common.util.Hash;

public class Utils {
	private static ThreadLocal<MessageDigest> DIGESTER_FACTORY = new ThreadLocal<MessageDigest>() {
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };
	private static final String JOB_HISTORY = "hadoop.job.history.user.location";
	private static final String SUCCESS_MARKER = "mapreduce.fileoutputcommitter.marksuccessfuljobs";

	public static final String toHexString(byte[] data) {
		StringBuffer buf = new StringBuffer(data.length * 2);
		for (int i = 0; i < data.length; i++) {
			int b = data[i];
			buf.append(HEX_DIGITS[(b >> 4 & 0xF)]);
			buf.append(HEX_DIGITS[(b & 0xF)]);
		}
		return buf.toString();
	}

	public static final String createRequestId(String userId, long timestamp, String query) {
		StringBuilder res = new StringBuilder();
		if ((userId == null) || (userId.isEmpty())) {
			userId = "none";
		}
		res.append(userId.replaceAll("[\\s~]+", "_"));
		res.append('~');

		int tstamp = (int) (timestamp & 0xFFFFFFFF);
		String stamp = Integer.toHexString(tstamp);
		int len = stamp.length();
		for (int i = 0; i < 8 - len; i++) {
			res.append('0');
		}
		res.append(stamp);
		return new StringBuilder().append(res.toString())
				.append(Integer.toHexString(Hash.lookup3ycs(query, 0, query.length(), 0))).toString();
	}

	public static final Configuration createConfiguration() {
		Configuration conf = new Configuration();

		if (StringUtils.isBlank(conf.get(JOB_HISTORY))) {
			conf.set(JOB_HISTORY, "none");
		}
		conf.setBoolean(SUCCESS_MARKER, false);
		return conf;
	}
}
