package gaia.feedback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.PriorityQueue;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.utils.SortedStringList;

public class Rocchio extends AbstractRelevanceFeedback {
	private static transient Logger LOG = LoggerFactory.getLogger(Rocchio.class);
	public static final float DEFAULT_ALPHA = 1.0F;
	public static final float DEFAULT_BETA = 1.0F;
	public static final float DEFAULT_GAMMA = 1.0F;
	public static final String PREFIX = "fdbk.rocchio.";
	public static final String ALPHA = "fdbk.rocchio.alpha";
	public static final String BETA = "fdbk.rocchio.beta";
	public static final String GAMMA = "fdbk.rocchio.gamma";
	public static final String SEARCH_FIELD = "fdbk.rocchio.searchField";
	public static final String DEFAULT_SEARCH_FIELD = "body";
	public static final String FEEDBACK_EMPHASIS = "feedback.emphasis";
	public static final String DEFAULT_FEEDBACK_EMPHASIS = "relevancy";
	private static Comparator<TermQuery> termQComparator = new Comparator<TermQuery>() {
		public int compare(TermQuery termQuery, TermQuery termQuery1) {
			int result = (int) (termQuery1.getBoost() - termQuery.getBoost());

			return result != 0 ? result : termQuery1.getTerm().text().compareTo(termQuery.getTerm().text());
		}
	};

	public Query generateQuery(Query originalQuery, DocList positives, DocList negatives, FeedbackHelper helper,
			BooleanClause.Occur operator, SolrParams sp, int maxTotalClauses) {
		float alpha = sp.getFloat("fdbk.rocchio.alpha", 1.0F);
		float beta = sp.getFloat("fdbk.rocchio.beta", 1.0F);
		float gamma = sp.getFloat("fdbk.rocchio.gamma", 1.0F);
		String feedbackField = sp.get("fdbk.rocchio.searchField", "body");
		String emphasis = sp.get("feedback.emphasis", "relevancy");
		return generateQuery(alpha, originalQuery, beta, positives, gamma, negatives, helper, operator, feedbackField,
				maxTotalClauses, emphasis, null, null);
	}

	public Query generateQuery(Query originalQuery, DocList positives, DocList negatives, FeedbackHelper helper,
			BooleanClause.Occur operator, SolrParams sp, int maxTotalClauses, SortedStringList originalTerms,
			SortedStringList feedbackTerms) {
		float alpha = sp.getFloat("fdbk.rocchio.alpha", 1.0F);
		float beta = sp.getFloat("fdbk.rocchio.beta", 1.0F);
		float gamma = sp.getFloat("fdbk.rocchio.gamma", 1.0F);
		String feedbackField = sp.get("fdbk.rocchio.searchField", "body");
		String emphasis = sp.get("feedback.emphasis", "relevancy");
		return generateQuery(alpha, originalQuery, beta, positives, gamma, negatives, helper, operator, feedbackField,
				maxTotalClauses, emphasis, originalTerms, feedbackTerms);
	}

	public Query generateQuery(Query originalQuery, DocList positives, DocList negatives, FeedbackHelper mlt,
			String feedbackField) {
		return generateQuery(1.0F, originalQuery, 1.0F, positives, 1.0F, negatives, mlt, BooleanClause.Occur.SHOULD,
				feedbackField, BooleanQuery.getMaxClauseCount(), "should", null, null);
	}

	public Query generateQuery(Query originalQuery, DocList positives, DocList negatives, FeedbackHelper mlt,
			String feedbackField, int maxTotalClauses, String emphasis) {
		return generateQuery(1.0F, originalQuery, 1.0F, positives, 1.0F, negatives, mlt, BooleanClause.Occur.SHOULD,
				feedbackField, maxTotalClauses, emphasis, null, null);
	}

	public Query generateQuery(float alpha, Query originalQuery, float beta, DocList positives, float gamma,
			DocList negatives, FeedbackHelper mlt, String feedbackField, int maxTotalClauses) {
		return generateQuery(alpha, originalQuery, beta, positives, gamma, negatives, mlt, BooleanClause.Occur.SHOULD,
				feedbackField, maxTotalClauses, "must", null, null);
	}

	public Query generateQuery(float alpha, Query originalQuery, float beta, DocList positives, float gamma,
			DocList negatives, FeedbackHelper mlt, BooleanClause.Occur operator, String feedbackField, int maxTotalClauses,
			String emphasis, SortedStringList originalTerms, SortedStringList feedbackTerms) {
		Query result = null;

		List<TermQuery> feedbackTermQueries = new ArrayList<TermQuery>(200);
		Map<String, TermQuery> termQueryMap = new HashMap<String, TermQuery>(100);

		if ((positives != null) && (positives.size() > 0)) {
			processPositives(beta / positives.size(), positives, feedbackTermQueries, termQueryMap, originalTerms,
					feedbackTerms, mlt, feedbackField);
		}
		if ((negatives != null) && (negatives.size() > 0)) {
			processNegatives(gamma / negatives.size(), negatives, feedbackTermQueries, termQueryMap, mlt, feedbackField);
		}

		if (feedbackTermQueries.size() > 0) {
			BooleanQuery bq = new BooleanQuery();
			BooleanClause.Occur occur = BooleanClause.Occur.MUST;
			if (emphasis.equalsIgnoreCase("relevancy"))
				occur = BooleanClause.Occur.MUST;
			else if (emphasis.equalsIgnoreCase("recall"))
				occur = BooleanClause.Occur.SHOULD;
			bq.add(originalQuery, occur);
			originalQuery.setBoost(alpha * originalQuery.getBoost());
			Collections.sort(feedbackTermQueries, termQComparator);
			if (maxTotalClauses > BooleanQuery.getMaxClauseCount()) {
				BooleanQuery.setMaxClauseCount(maxTotalClauses);
			}
			int i = 0;
			for (Iterator<TermQuery> iterator = feedbackTermQueries.iterator(); (iterator.hasNext()) && (i < maxTotalClauses);) {
				TermQuery query = iterator.next();
				bq.add(query, operator);
				i++;
			}
			result = bq;
		}
		return result;
	}

	protected void processNegatives(float gamma, DocList negatives, List<TermQuery> feedbackTermQueries,
			Map<String, TermQuery> termQueryMap, FeedbackHelper mlt, String feedbackField) {
		DocIterator iterator;
		if ((negatives != null) && (negatives.matches() > 0)) {
			for (iterator = negatives.iterator(); iterator.hasNext();) {
				int docNum = iterator.next().intValue();
				try {
					PriorityQueue<Object> pq = mlt.retrieveTerms(docNum);

					int lim = mlt.getMaxQueryTerms();
					Object cur;
					while (((cur = pq.pop()) != null) && (lim-- > 0)) {
						Object[] ar = (Object[]) cur;
						String term = ar[0].toString();
						float adjScore = calculateScore(gamma, ((Integer) ar[5]).floatValue(), ((Float) ar[3]).floatValue() - 1.0F);
						TermQuery query = (TermQuery) termQueryMap.get(term);
						if (query == null) {
							query = new TermQuery(new Term(feedbackField, term));
							termQueryMap.put(term, query);
							feedbackTermQueries.add(query);
						}
						query.setBoost(query.getBoost() - adjScore);
					}
				} catch (IOException e) {
					LOG.warn("Couldn't get terms for: " + docNum, e);
				}
			}
		}
	}

	protected float calculateScore(float factor, float tf, float idf) {
		return factor * (tf * Math.max(idf, 1.0E-06F));
	}

	protected void processPositives(float beta, DocList positives, List<TermQuery> feedbackTermQueries,
			Map<String, TermQuery> termQueryMap, SortedStringList originalTerms, SortedStringList feedbackTerms,
			FeedbackHelper mlt, String feedbackField) {
		DocIterator iterator;
		if ((positives != null) && (positives.matches() > 0)) {
			for (iterator = positives.iterator(); iterator.hasNext();) {
				int docNum = iterator.next().intValue();
				try {
					PriorityQueue<Object> pq = mlt.retrieveTerms(docNum);

					int lim = mlt.getMaxQueryTerms();
					Object cur;
					while (((cur = pq.pop()) != null) && (lim > 0)) {
						Object[] ar = (Object[]) cur;
						String term = ar[0].toString();

						if (((term == null) || (term.length() <= 0) || (term.charAt(0) != '\001'))
								&& ((originalTerms == null) || (!originalTerms.contains(term)))) {
							float adjScore = calculateScore(beta, ((Integer) ar[5]).floatValue(), ((Float) ar[3]).floatValue() - 1.0F);
							TermQuery query = (TermQuery) termQueryMap.get(term);
							if (query == null) {
								query = new TermQuery(new Term(feedbackField, term));
								feedbackTermQueries.add(query);
								termQueryMap.put(term, query);
								if (feedbackTerms != null)
									feedbackTerms.add(term);
							}
							query.setBoost(query.getBoost() + adjScore);
							lim--;
						}
					}
				} catch (IOException e) {
					LOG.warn("Couldn't get terms for: " + docNum, e);
				}
			}
		}
	}
}
