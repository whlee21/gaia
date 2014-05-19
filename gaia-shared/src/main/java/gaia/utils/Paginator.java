package gaia.utils;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

public class Paginator {
	public static PageInfo getPageInfo(QueryResponse response) {
		int resultsPerPage = 10;
		long numFound = 0L;
		long start = 0L;

		NamedList<?> header = response.getResponseHeader();
		if (header != null) {
			NamedList<?> params = (NamedList) header.get("params");

			if (params != null) {
				String rows = (String) params.get("rows");

				if (rows != null) {
					resultsPerPage = new Integer(rows).intValue();
				}

				SolrDocumentList documentList = response.getResults();

				if (documentList != null) {
					numFound = documentList.getNumFound();
					start = documentList.getStart();
				}
			}
		}

		return new PageInfo(numFound, resultsPerPage, start);
	}
}
