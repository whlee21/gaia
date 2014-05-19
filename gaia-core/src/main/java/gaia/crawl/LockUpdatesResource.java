package gaia.crawl;

import java.io.IOException;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockUpdatesResource extends ServerResource {
	Logger LOG = LoggerFactory.getLogger(LockUpdatesResource.class);

	// protected static final Settings settings =
	// (Settings)LWEGuiceServletConfig.injectorHack().getInstance(Settings.class);

	@Put
	public void lock() throws IOException {
		String reason = getQuery().getFirstValue("reason", null);
		LOG.warn("Locking updates: {}.", reason == null ? "No reason given" : reason);

		// settings.set(Settings.Group.control, "blockUpdates",
		// Boolean.valueOf(true));
		// settings.set(Settings.Group.control, "blockUpdatesReason", reason);
	}

	@Get
	public void check() throws IOException {
		// if (settings.getBoolean(Settings.Group.control, "blockUpdates"))
		// setStatus(Status.SUCCESS_NO_CONTENT, "locked");
		// else
		// setStatus(ResultStatus.STATUS.NOT_FOUND, "not locked");
	}

	@Delete
	public void unlock() {
		LOG.info("Unlocking updates.");
		// settings.set(Settings.Group.control, "blockUpdates",
		// Boolean.valueOf(false));
	}
}
