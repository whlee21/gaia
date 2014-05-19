package gaia.api;

import java.text.MessageFormat;
import org.apache.log4j.Logger;

public class AuditLogger {
	private static final Logger LOG = Logger.getLogger(AuditLogger.class);

	private static void log(String template, Object[] parameters) {
		if (LOG.isInfoEnabled())
			LOG.info(MessageFormat.format(template, parameters));
	}

	public static void log(String action) {
		String user = null;

		if (user == null) {
			user = "Anonymous user";
		}
		log("username={0} action={1}", new Object[] { user, action });
	}
}
