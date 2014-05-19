package gaia.rules.drools;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.schema.IndexSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DroolsHelper {
	private static transient Logger LOG = LoggerFactory.getLogger(DroolsHelper.class);
	public static final String RULES_PHASE = "rulesPhase";
	public static final String RULES_HANDLE = "rulesHandle";

	public static boolean hasPhaseMatch(ResponseBuilder builder, String expectedPhase) {
		String value = (String) builder.req.getContext().get(RULES_PHASE);
		return (value != null) && (value.equals(expectedPhase) == true);
	}

	public static boolean hasPhaseMatch(ResponseBuilder builder, String expectedPhase, String handleName) {
		Map<Object, Object> context = builder.req.getContext();
		String phase = (String) context.get(RULES_PHASE);
		String handler = (String) context.get(RULES_HANDLE);
		return (phase != null) && (phase.equals(expectedPhase) == true) && (handler != null)
				&& (handler.equals(handleName));
	}

	public static boolean hasHandlerNameMatch(ResponseBuilder builder, String handlerName) {
		Map<Object, Object> context = builder.req.getContext();
		String handler = (String) context.get(RULES_HANDLE);
		return (handler != null) && (handler.equals(handlerName));
	}

	public static void boostQuery(Query q, float boost) {
		q.setBoost(boost);
	}

	public static void addToResponse(ResponseBuilder builder, String key, Object val) {
		addToResponse(builder.rsp.getValues(), key, val);
	}

	public static void addToResponse(NamedList namedList, String key, Object val) {
		namedList.add(key, val);
	}

	public static void mergeFacets(ResponseBuilder builder, String targetField, int position, String[] facetQueries) {
		NamedList facetCounts = (NamedList) builder.rsp.getValues().get("facet_counts");
		if (facetCounts != null) {
			NamedList facetFields = (NamedList) facetCounts.get("facet_fields");
			NamedList facetQueryNL = (NamedList) facetCounts.get("facet_queries");
			SolrParams facetQueryResults = SolrParams.toSolrParams(facetQueryNL);
			if ((facetFields != null) && (facetQueryResults != null)) {
				NamedList target = (NamedList) facetFields.get(targetField);
				if (target != null) {
					LOG.info("Merging into " + targetField + " at position: " + position);
					NamedList tmpTarget = new NamedList();
					int j = 0;
					for (Iterator iter = target.iterator(); iter.hasNext();) {
						Object o = iter.next();
						Map.Entry entry = (Map.Entry) o;
						if (j == position)
							for (int i = 0; i < facetQueries.length; i++) {
								String facetQuery = facetQueries[i];
								int count = facetQueryResults.getInt(facetQuery).intValue();
								target.add(facetQuery, Integer.valueOf(count));
							}
						else {
							tmpTarget.add((String) entry.getKey(), entry.getValue());
						}
						j++;
					}
					int index = facetFields.indexOf(targetField, 0);
					facetCounts.setVal(index, tmpTarget);
				}
			}
		}
	}

	public static void addFacet(ResponseBuilder builder, String facetName, String facetValue, int facetCount, int position) {
		NamedList facetCounts = (NamedList) builder.rsp.getValues().get("facet_counts");
		if (facetCounts != null) {
			NamedList facetFields = (NamedList) facetCounts.get("facet_fields");
			if (facetFields != null) {
				NamedList target = (NamedList) facetFields.get(facetName);
				if (target != null) {
					LOG.info("Adding into " + facetName);
					target.add(facetValue, Integer.valueOf(facetCount));
				}
			}
		}
	}

	public static void modRequest(ResponseBuilder builder, String key, String[] values) {
		((ModifiableSolrParams) builder.req.getParams()).set(key, values);
	}

	public static void modRequest(ResponseBuilder builder, String key, int value) {
		((ModifiableSolrParams) builder.req.getParams()).set(key, value);
	}

	public static void modRequest(ResponseBuilder builder, String key, boolean value) {
		((ModifiableSolrParams) builder.req.getParams()).set(key, value);
	}

	public static boolean contains(String query, String value) {
		return query.toString().contains(value);
	}

	public static Collection<String> analyze(IndexSchema schema, String field, String text) throws IOException {
		Analyzer analyzer = schema.getAnalyzer();
		StringReader reader = new StringReader(text);
		TokenStream ts = analyzer.tokenStream(field, reader);
		CharTermAttribute termAtt = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);
		ts.reset();
		List<String> result = new ArrayList<String>();

		while (ts.incrementToken()) {
			result.add(termAtt.toString());
		}

		ts.end();
		ts.close();

		return result;
	}
}
