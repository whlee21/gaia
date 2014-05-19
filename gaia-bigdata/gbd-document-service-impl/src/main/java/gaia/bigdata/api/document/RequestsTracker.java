package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RequestsTracker {
	private static transient Logger log = LoggerFactory.getLogger(RequestsTracker.class);
	List<DocServiceRequest> supported;
	List<String> unsupported;

	public static String createUnsupportedMessage(RequestsTracker requests) {
		StringBuilder bldr = new StringBuilder("The system is unable to service any requests from the current request: ");
		for (String unsup : requests.unsupported) {
			bldr.append(unsup).append(' ');
		}
		return bldr.toString().trim();
	}

	public static RequestsTracker getRequests(Map<ResultType, DocumentService> dsMap, Map<String, Object> request) {
		RequestsTracker result = new RequestsTracker();
		result.supported = new ArrayList<DocServiceRequest>();
		if ((request != null) && (!request.isEmpty()))
			for (Map.Entry<String, Object> entry : request.entrySet()) {
				ResultType rt = ResultType.valueOf(((String) entry.getKey()).toUpperCase());
				if (rt != null) {
					DocumentService ds = (DocumentService) dsMap.get(rt);
					if (ds != null) {
						DocServiceRequest req = new DocServiceRequest(ds, (Map) entry.getValue(), rt);
						result.supported.add(req);
					} else {
						log.info(new StringBuilder().append("No DocumentService for ResultType: ").append(rt).toString());
						result.unsupported.add(rt.toString());
					}
				} else {
					log.info(new StringBuilder().append("No support for ResultType: ").append((String) entry.getKey()).toString());
					result.unsupported.add(entry.getKey());
				}
			}
		else {
			log.info("request is empty");
		}
		return result;
	}
}
