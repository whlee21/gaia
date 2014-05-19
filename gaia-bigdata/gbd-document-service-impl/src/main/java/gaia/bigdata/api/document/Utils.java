package gaia.bigdata.api.document;

import org.apache.solr.common.util.Hash;

public class Utils {
	public static final String createRequestId(String userId, long timestamp, String query) {
		StringBuilder res = new StringBuilder();
		if ((userId == null) || (userId.isEmpty())) {
			userId = "NO_USER";
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
}
