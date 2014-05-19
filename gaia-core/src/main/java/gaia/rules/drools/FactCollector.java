package gaia.rules.drools;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.SortSpec;
import org.apache.solr.search.grouping.GroupingSpecification;
import org.apache.solr.update.AddUpdateCommand;

public class FactCollector {
	protected IndexSchema schema;
	protected NamedList initArgs;

	public void init(NamedList args, SolrCore core) {
		initArgs = args;
		schema = core.getLatestSchema();
	}

	public void addFacts(ResponseBuilder rb, Collection<Object> facts) {
		addSolrBasicFacts(rb, facts);
		addQuery(rb.getQuery(), facts);
		addFilters(rb.getFilters(), facts);
		addResults(rb.getResults(), facts);
		addSort(rb.getSortSpec(), facts);
		addGrouping(rb.getGroupingSpec(), facts);
		addFacets((NamedList) rb.rsp.getValues().get("facet_counts"), facts);
	}

	protected void addSolrBasicFacts(ResponseBuilder rb, Collection<Object> facts) {
		facts.add(rb);
		facts.add(rb.req.getSchema());
		facts.add(rb.req);
		facts.add(rb.req.getContext());
		facts.add(rb.rsp);
		if (rb.rsp.getValues() != null) {
			facts.add(rb.rsp.getValues().get("response"));
		}
		SolrParams params = rb.req.getParams();

		facts.add((ModifiableSolrParams) params);
	}

	protected void addFacets(NamedList facetCountsNL, Collection<Object> facts) {
		if (facetCountsNL != null)
			facts.add(facetCountsNL);
	}

	protected void addGrouping(GroupingSpecification groupingSpec, Collection<Object> facts) {
		if (groupingSpec != null)
			facts.add(groupingSpec);
	}

	protected void addSort(SortSpec sortSpec, Collection<Object> facts) {
		if (sortSpec != null)
			facts.add(sortSpec);
	}

	protected void addFilters(List<Query> filters, Collection<Object> facts) {
		if (filters != null) {
			facts.add(filters);
			for (Query filter : filters)
				facts.add(new FilterWrapper(filter));
		}
	}

	protected void addResults(DocListAndSet results, Collection<Object> facts) {
		if (results != null)
			facts.add(results);
	}

	protected void addQuery(Query theQuery, Collection<Object> facts) {
		if (theQuery != null) {
			facts.add(theQuery);

			if ((theQuery instanceof BooleanQuery)) {
				BooleanQuery bq = (BooleanQuery) theQuery;
				for (BooleanClause clause : bq) {
					QueryRelationship rel = new QueryRelationship(theQuery, clause.getQuery());
					rel.setOccurrence(clause.getOccur());
					rel.setType(RelationshipType.PARENT_CHILD);
					facts.add(rel);
					addQuery(clause.getQuery(), facts);
				}
			} else {
				DisjunctionMaxQuery dmq;
				if ((theQuery instanceof DisjunctionMaxQuery)) {
					dmq = (DisjunctionMaxQuery) theQuery;
					for (Query sub : dmq) {
						QueryRelationship rel = new QueryRelationship(dmq, sub);
						rel.setRelationship(RelationshipType.PARENT_CHILD);
						facts.add(rel);
						addQuery(sub, facts);
					}
				} else if ((theQuery instanceof BoostedQuery)) {
					BoostedQuery bq = (BoostedQuery) theQuery;
					addQuery(bq.getQuery(), facts);
				} else {
					Set<Term> terms = new HashSet<Term>();
					try {
						theQuery.extractTerms(terms);
					} catch (UnsupportedOperationException e) {
					}
					if (!terms.isEmpty())
						for (Term term : terms)
							facts.add(term);
				}
			}
		}
	}

	public void addFacts(AddUpdateCommand cmd, Collection<Object> facts) {
		facts.add(cmd);
		facts.add(cmd.getSolrInputDocument());
		facts.add(schema);
	}

	public void addFacts(SolrDocument doc, int docId, Collection<Object> facts) {
		facts.add(doc);
		facts.add(Integer.valueOf(docId));
		facts.add(schema);
	}
}
