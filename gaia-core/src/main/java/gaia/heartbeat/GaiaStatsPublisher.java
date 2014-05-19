package gaia.heartbeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Constants;
import gaia.utils.VersionUtil;

public class GaiaStatsPublisher {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaStatsPublisher.class);
	private static final String URI = "https://heartbeat.demo.lucidworks.io/heartbeat.rb";
	private static final String MAC = "voghhulechutspedovow";
	private final String serverId;
	private final Map<String, String> info = new HashMap<String, String>();

	public GaiaStatsPublisher() {
		String productName = "gaia";
		if (System.getProperty("heartbeatServerId") != null) {
			String val = System.getProperty("heartbeatServerId");
			if (val.contains("/")) {
				String[] arr = val.split("/");
				productName = arr[0];
				serverId = arr[1];
			} else {
				serverId = val;
			}
		} else {
			serverId = null;
		}

		Map<String, String> versionInfo = VersionUtil.getGaiaWorksVersionInfo();
		info.put("product", productName);
		info.put("current_product_version", versionInfo.get("version"));
		info.put("lwe_git_sha", versionInfo.get("git.commit"));
		info.put("is_cloudy", Boolean.toString(Constants.IS_CLOUDY));
		info.put("os_version", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
		info.put("num_cpu_cores", Integer.toString(Runtime.getRuntime().availableProcessors()));
		info.put("java_version", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
	}

	public void sendVersionInfo(Map<String, String> additionalInfo) {
		Map<String, String> data = new HashMap<String, String>();
		data.putAll(info);
		data.putAll(additionalInfo);
		publishStats(data);
	}

	public void publishStats(Map<String, String> map) {
		if (map == null) {
			return;
		}
		LOG.info("Sending heartbeat stats: uuid='{}', data='{}'", serverId, map);

		AbstractHttpClient httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(URI);
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("uuid", serverId));
		data.add(new BasicNameValuePair("mac", MAC));

		for (Map.Entry<String, String> entry : map.entrySet()) {
			data.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

		HttpResponse response = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));
			response = httpClient.execute(post);
			if (response.getStatusLine().getStatusCode() != 200)
				LOG.warn("Could not send heartbeat statistics to Gaia server: {}", response.getStatusLine());
		} catch (Exception e) {
			LOG.warn("Could not send heartbeat statistics to Gaia server", e);
		}
	}

	public boolean isEnabled() {
		return serverId != null;
	}
}
