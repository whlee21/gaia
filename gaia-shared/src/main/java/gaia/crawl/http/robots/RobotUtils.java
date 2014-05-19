package gaia.crawl.http.robots;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.http.protocol.HttpContentUtils;
import gaia.crawl.http.protocol.HttpProtocol;
import gaia.crawl.http.protocol.ProtocolOutput;
import gaia.crawl.http.protocol.ProtocolStatus;

public class RobotUtils {
	private static final Logger LOG = LoggerFactory.getLogger(RobotUtils.class);
	private static final int MAX_ROBOTS_SIZE = 131072;
	private static final int MAX_CONNECTIONS_PER_HOST = 20;
	private static final int ROBOTS_CONNECTION_TIMEOUT = 10000;
	private static final int ROBOTS_SOCKET_TIMEOUT = 10000;
	private static final int ROBOTS_RETRY_COUNT = 2;
	private static final long MAX_FETCH_TIME = 40000L;

	public static long getMaxFetchTime() {
		return MAX_FETCH_TIME;
	}

	public static BaseRobotRules getRobotRules(HttpProtocol http, RobotsCache cache, BaseRobotsParser parser,
			URL robotsUrl) throws IOException {
		ProtocolOutput output = null;
		output = http.getProtocolOutput(robotsUrl, 0L, true, MAX_ROBOTS_SIZE, true, HttpProtocol.Method.GET);

		if (output.getStatus().code != ProtocolStatus.Code.OK) {
			throw new IOException(output.getStatus().message);
		}
		String contentType = output.getMetadata().get("Content-Type");
		boolean isPlainText = (contentType != null) && (contentType.startsWith("text/plain"));
		if ((HttpContentUtils.getNumRedirects(output.getMetadata()) > 0) && (!isPlainText)) {
			return parser.failedFetch(410);
		}

		BaseRobotRules rules = parser.parseContent(robotsUrl.toExternalForm(), output.getData(), contentType, http
				.getConfig().getUserAgent().getAgentName());

		if ((rules != null) && (cache != null)) {
			cache.put(robotsUrl.getHost(), rules);

			ArrayList<String> hosts = new ArrayList<String>();
			HttpContentUtils.getRedirects(null, hosts, output.getMetadata());
			for (String s : hosts) {
				cache.put(s, rules);
			}
		}
		return rules;
	}
}
