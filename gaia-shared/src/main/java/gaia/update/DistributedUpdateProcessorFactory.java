package gaia.update;

import java.util.List;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

public class DistributedUpdateProcessorFactory extends UpdateRequestProcessorFactory {
	NamedList args;
	List<String> shards;
	String selfStr;
	String shardsString;

	public void init(NamedList args) {
		selfStr = ((String) args.get("self"));
		Object o = args.get("shards");
		if ((o != null) && ((o instanceof List))) {
			shards = ((List) o);
			shardsString = StrUtils.join((List) o, ',');
		} else if ((o != null) && ((o instanceof String))) {
			shards = StrUtils.splitSmart((String) o, ",", true);
			shardsString = ((String) o);
		}
	}

	public List<String> getShards() {
		return shards;
	}

	public String getShardsString() {
		return shardsString;
	}

	public String getSelf() {
		return selfStr;
	}

	public DistributedUpdateProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
		String shardStr = req.getParams().get("shards");
		if ((shards == null) && (shardStr == null))
			return null;
		return new DistributedUpdateProcessor(shardStr, req, rsp, this, next);
	}
}
