package gaia.bigdata.api.document;

import java.util.concurrent.Future;
import org.json.JSONObject;

class FutureWrapper {
	Future<JSONObject> future;
	DocServiceRequest origRequest;
}
