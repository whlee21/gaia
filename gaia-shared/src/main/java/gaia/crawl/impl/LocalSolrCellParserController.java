package gaia.crawl.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;

import com.google.inject.Inject;
import gaia.crawl.ParserController;
import gaia.crawl.UpdateController;
import gaia.crawl.io.Content;
import gaia.crawl.io.ContentContentStream;

public class LocalSolrCellParserController extends ParserController {

	@Inject
	private CoreContainer cores;

	public LocalSolrCellParserController(UpdateController updateController) {
		super(updateController);
	}

	public List<SolrInputDocument> parse(Content content) throws Exception {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("extractOnly", true);
		SolrCore core = cores.getCore(state.getDataSource().getCollection());
		try {
			SolrRequestHandler handler = core.getRequestHandler("/update/extract");

			LocalSolrQueryRequest extReq = new LocalSolrQueryRequest(core, params);

			ArrayList<ContentStream> list = new ArrayList<ContentStream>();
			list.add(new ContentContentStream(content));
			extReq.setContentStreams(list);
			SolrQueryResponse rsp = new SolrQueryResponse();
			handler.handleRequest(extReq, rsp);
			if (rsp.getException() != null)
				throw new Exception(rsp.getException());
		} finally {
			core.close();
		}

		return Collections.emptyList();
	}
}
