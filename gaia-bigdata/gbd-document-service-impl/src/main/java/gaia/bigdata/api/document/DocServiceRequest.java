package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import java.util.Map;

public class DocServiceRequest {
	private DocumentService service;
	public Map<String, Object> request;
	public ResultType resultType;

	public DocServiceRequest(DocumentService ds, Map<String, Object> entry, ResultType rt) {
		this.service = ds;
		this.request = entry;
		this.resultType = rt;
	}

	public DocumentService getService() {
		return this.service;
	}
}
