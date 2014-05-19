package gaia.feedback;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.DocList;

import gaia.utils.SortedStringList;

public abstract class AbstractRelevanceFeedback {
	public abstract Query generateQuery(Query paramQuery, DocList paramDocList1, DocList paramDocList2,
			FeedbackHelper paramFeedbackHelper, BooleanClause.Occur paramOccur, SolrParams paramSolrParams, int paramInt);

	public abstract Query generateQuery(Query paramQuery, DocList paramDocList1, DocList paramDocList2,
			FeedbackHelper paramFeedbackHelper, BooleanClause.Occur paramOccur, SolrParams paramSolrParams, int paramInt,
			SortedStringList paramSortedStringList1, SortedStringList paramSortedStringList2);
}
