package gaia.update;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;

import gaia.common.params.FieldMappingParams;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;

public class FieldMappingRequest extends SolrRequest {
	private static final long serialVersionUID = 5174506562652484991L;
	ModifiableSolrParams params = new ModifiableSolrParams();
	Map<String, ContentStream> streams = null;

	public FieldMappingRequest(String path) {
		super(SolrRequest.METHOD.GET, path);
	}

	public void setParam(String name, String value) {
		params.set(name, new String[] { value });
	}

	public void addParam(String name, String value) {
		params.add(name, new String[] { value });
	}

	public void setAction(FieldMappingParams.Action action) throws Exception {
		String oldAction = params.get("fm.action");

		if ((oldAction != null) && (!oldAction.equals(action.toString()))) {
			throw new Exception("fm.action param already set to " + oldAction);
		}
		params.set("fm.action", new String[] { action.toString() });
		if (action.equals(FieldMappingParams.Action.DEFINE)) {
			setMethod(SolrRequest.METHOD.POST);
			streams = new HashMap<String, ContentStream>();
		} else {
			setMethod(SolrRequest.METHOD.GET);
			streams = null;
		}
	}

	public void defineMapping(FieldMapping mapping, String dsId) throws Exception {
		setAction(FieldMappingParams.Action.DEFINE);
		if (mapping == null) {
			streams.remove(dsId);
			return;
		}
		String json = FieldMappingUtil.toJSON(mapping);
		ContentStreamBase.StringStream stream = new ContentStreamBase.StringStream(json);

		if (streams.size() > 0)
			params.remove("fm.ds");
		else {
			params.set("fm.ds", new String[] { dsId });
		}
		stream.setName(dsId);
		streams.put(dsId, stream);
	}

	public void deleteMapping(String dsId) throws Exception {
		setAction(FieldMappingParams.Action.DELETE);
		params.set("fm.ds", new String[] { dsId });
	}

	public void clearAllMappings() throws Exception {
		setAction(FieldMappingParams.Action.CLEAR);
	}

	public SolrParams getParams() {
		return params;
	}

	public Collection<ContentStream> getContentStreams() throws IOException {
		return streams != null ? streams.values() : null;
	}

	public SolrResponse process(SolrServer server) throws SolrServerException, IOException {
		NamedList<Object> res = server.request(this);
		SolrResponse rsp = new SolrResponseBase();
		rsp.setResponse(res);
		return rsp;
	}
}
