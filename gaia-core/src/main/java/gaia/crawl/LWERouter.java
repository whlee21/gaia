package gaia.crawl;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.routing.Router;

class LWERouter extends Router {
	public LWERouter(Context context) {
		super(context);
	}

	public void handle(Request request, Response response) {
		String lastSeg = request.getResourceRef().getLastSegment();
		if (lastSeg.endsWith(".json")) {
			request.getResourceRef().setLastSegment(lastSeg.substring(0, lastSeg.length() - ".json".length()));
		}

		super.handle(request, response);
	}
}
