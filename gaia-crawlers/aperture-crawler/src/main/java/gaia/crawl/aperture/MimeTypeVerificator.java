package gaia.crawl.aperture;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.http.protocol.HttpProtocol;
import gaia.crawl.http.protocol.HttpProtocolConfig;
import gaia.crawl.http.protocol.ProtocolOutput;
import gaia.crawl.http.protocol.ProtocolStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeTypeVerificator {
	private static Logger LOG = LoggerFactory.getLogger(MimeTypeVerificator.class);

	private static Map<String, String> mimetypes = new HashMap<String, String>();

	private static String getExtension(String filename) {
		int lastDotIndex = filename.lastIndexOf(46);

		if ((lastDotIndex > 0) && (lastDotIndex < filename.length() - 1)) {
			filename = filename.substring(lastDotIndex + 1);
		}

		return filename.toLowerCase();
	}

	private static String checkByExtension(String filename, String mimetype) {
		String ext = getExtension(filename);

		if ((mimetype.equals("application/zip")) && (mimetypes.containsKey(ext))) {
			mimetype = (String) mimetypes.get(ext);
		}
		return mimetype;
	}

	private static String checkByUrl(String filename, String mimetype, DataSource ds) {
		LOG.debug("checkByUrl: mimetype received -> " + mimetype);

		Map<String, Object> map = ds.getProperties();

		HttpProtocolConfig cfg = new HttpProtocolConfig(map);

		if ((cfg.getTimeout() < 0) || (cfg.getTimeout() > 60000)) {
			cfg.setTimeout(5000);
		}

		HttpProtocol proto = new HttpProtocol(cfg, null);
		try {
			ProtocolOutput out = proto.getProtocolOutput(new URL(filename), -1L, HttpProtocol.Method.HEAD);

			if (out.getStatus().code == ProtocolStatus.Code.OK) {
				String tmp = out.getMetadata().get("Content-Type");

				LOG.debug("checkByUrl: mimetype of Content-Type -> " + tmp);

				String mtExt = checkByExtension(filename, mimetype);
				String mtUrl = mimetypes.get(tmp.trim()) == null ? mimetype : (String) mimetypes.get(tmp.trim());

				LOG.debug("checkByUrl: checkByExtension -> " + mtExt);

				LOG.debug("checkByUrl: replaced mimetype -> " + mtUrl);

				if ((!mtExt.equals(mimetype)) && (mtUrl.equals(mimetype))) {
					mimetype = mtExt;
				} else if ((mimetype.equals("application/zip")) && (mimetypes.containsKey(tmp.trim())))
					mimetype = mtUrl;
			} else {
				mimetype = checkByExtension(filename, mimetype);
			}
		} catch (Throwable e) {
			Writer result = new StringWriter();
			PrintWriter printWriter = new PrintWriter(result);

			e.printStackTrace(printWriter);

			LOG.info(result.toString());
			LOG.debug(result.toString());
		}

		LOG.debug("checkByUrl: final mimetype -> " + mimetype);

		return mimetype;
	}

	public static String check(String filename, String mimetype, DataSource ds) {
		if (mimetypes.isEmpty()) {
			loadMimeTypes();
		}
		LOG.debug("enter to check with mimetype -> " + mimetype);

		String type = (String) ds.getProperty("type");

		LOG.debug("ds type -> " + type);

		if ((type.equals("web")) && ((filename.trim().startsWith("http://")) || (filename.trim().startsWith("https://")))) {
			LOG.debug("ds type is web and start with http:// or https:// -> ");

			if (mimetype.equals("application/zip")) {
				return checkByUrl(filename, mimetype, ds);
			}
			return mimetype;
		}
		return checkByExtension(filename, mimetype);
	}

	private static void loadMimeTypes() {
		mimetypes.put("doc", "application/vnd.openxmlformats-officedocument.wordprocessingml");
		mimetypes.put("xls", "application/vnd.openxmlformats-officedocument.spreadsheetml");
		mimetypes.put("ppt", "application/vnd.openxmlformats-officedocument.presentationml");

		mimetypes.put("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml");
		mimetypes.put("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml");
		mimetypes.put("application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml");
	}
}
