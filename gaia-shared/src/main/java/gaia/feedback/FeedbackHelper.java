package gaia.feedback;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util.Version;

public final class FeedbackHelper {
	public static final int DEFAULT_MAX_NUM_TOKENS_PARSED = 5000;
	public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer(Version.LUCENE_45);
	public static final int DEFAULT_MIN_TERM_FREQ = 2;
	public static final int DEFAULT_MIN_DOC_FREQ = 5;
	public static final boolean DEFAULT_BOOST = false;
	public static final String[] DEFAULT_FIELD_NAMES = { "contents" };
	public static final int DEFAULT_MIN_WORD_LENGTH = 0;
	public static final int DEFAULT_MAX_WORD_LENGTH = 0;
	public static final Set<Object> DEFAULT_STOP_WORDS = null;

	private Set<Object> stopWords = DEFAULT_STOP_WORDS;
	public static final int DEFAULT_MAX_QUERY_TERMS = 25;
	private Analyzer analyzer = DEFAULT_ANALYZER;

	private int minTermFreq = 2;

	private int minDocFreq = 5;

	private boolean boost = false;

	private String[] fieldNames = DEFAULT_FIELD_NAMES;

	private int maxNumTokensParsed = 5000;

	private int minWordLen = 0;

	private int maxWordLen = 0;

	private int maxQueryTerms = 25;
	private final IndexReader ir;

	public FeedbackHelper(IndexReader ir) {
		this.ir = ir;
		Collection<String> fields = MultiFields.getIndexedFields(ir);
		fieldNames = ((String[]) fields.toArray(new String[fields.size()]));
	}

	public FeedbackHelper(IndexReader ir, String[] fieldNames) {
		this.ir = ir;
		this.fieldNames = fieldNames;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public int getMinTermFreq() {
		return minTermFreq;
	}

	public void setMinTermFreq(int minTermFreq) {
		this.minTermFreq = minTermFreq;
	}

	public int getMinDocFreq() {
		return minDocFreq;
	}

	public void setMinDocFreq(int minDocFreq) {
		this.minDocFreq = minDocFreq;
	}

	public boolean isBoost() {
		return boost;
	}

	public void setBoost(boolean boost) {
		this.boost = boost;
	}

	public String[] getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	public int getMinWordLen() {
		return minWordLen;
	}

	public void setMinWordLen(int minWordLen) {
		this.minWordLen = minWordLen;
	}

	public int getMaxWordLen() {
		return maxWordLen;
	}

	public void setMaxWordLen(int maxWordLen) {
		this.maxWordLen = maxWordLen;
	}

	public void setStopWords(Set<Object> stopWords) {
		this.stopWords = stopWords;
	}

	public Set<Object> getStopWords() {
		return stopWords;
	}

	public int getMaxQueryTerms() {
		return maxQueryTerms;
	}

	public void setMaxQueryTerms(int maxQueryTerms) {
		this.maxQueryTerms = maxQueryTerms;
	}

	public int getMaxNumTokensParsed() {
		return maxNumTokensParsed;
	}

	public void setMaxNumTokensParsed(int i) {
		maxNumTokensParsed = i;
	}

	private PriorityQueue<Object> createQueue(Map<String, Int> words) throws IOException {
		int maxDoc = ir.maxDoc();
		FreqQ res = new FreqQ(words.size());

		for (Map.Entry<String, Int> entry : words.entrySet()) {
			String word = (String) entry.getKey();

			int tf = ((Int) entry.getValue()).x;
			if ((minTermFreq <= 0) || (tf >= minTermFreq)) {
				String topField = fieldNames[0];
				int docFreq = 0;
				for (int i = 0; i < fieldNames.length; i++) {
					int freq = ir.docFreq(new Term(fieldNames[i], word));
					topField = freq > docFreq ? fieldNames[i] : topField;
					docFreq = freq > docFreq ? freq : docFreq;
				}

				if (((minDocFreq <= 0) || (docFreq >= minDocFreq)) && (docFreq != 0)) {
					float idf = (float) (Math.log(maxDoc / (docFreq + 1)) + 1.0D);
					float score = tf * idf;

					res.insertWithOverflow(new Object[] { word, topField, new Float(score), new Float(idf), new Integer(docFreq),
							new Integer(tf) });
				}

			}

		}

		return res;
	}

	public String describeParams() {
		StringBuffer sb = new StringBuffer();
		sb.append("\tmaxQueryTerms  : " + maxQueryTerms + "\n");
		sb.append("\tminWordLen     : " + minWordLen + "\n");
		sb.append("\tmaxWordLen     : " + maxWordLen + "\n");
		sb.append("\tfieldNames     : ");
		String delim = "";
		for (int i = 0; i < fieldNames.length; i++) {
			String fieldName = fieldNames[i];
			sb.append(delim).append(fieldName);
			delim = ", ";
		}
		sb.append("\n");
		sb.append("\tboost          : " + boost + "\n");
		sb.append("\tminTermFreq    : " + minTermFreq + "\n");
		sb.append("\tminDocFreq     : " + minDocFreq + "\n");
		return sb.toString();
	}

	public PriorityQueue<Object> retrieveTerms(int docNum) throws IOException {
		Map<String, FeedbackHelper.Int> termFreqMap = new HashMap<String, FeedbackHelper.Int>();
		for (String fieldName : fieldNames) {
			Fields vectors = ir.getTermVectors(docNum);
			Terms vector;
			if (vectors != null)
				vector = vectors.terms(fieldName);
			else {
				vector = null;
			}

			if (vector == null) {
				Document d = ir.document(docNum);
				IndexableField[] fields = d.getFields(fieldName);
				for (int j = 0; j < fields.length; j++) {
					String stringValue = fields[j].stringValue();
					if (stringValue != null)
						addTermFrequencies(new StringReader(stringValue), termFreqMap, fieldName);
				}
			} else {
				addTermFrequencies(termFreqMap, vector);
			}
		}

		return createQueue(termFreqMap);
	}

	private void addTermFrequencies(Map<String, Int> termFreqMap, Terms vector) throws IOException {
		TermsEnum termsEnum = vector.iterator(null);
		CharsRef spare = new CharsRef();
		BytesRef text;
		while ((text = termsEnum.next()) != null) {
			UnicodeUtil.UTF8toUTF16(text, spare);
			String term = spare.toString();
			if (!isNoiseWord(term)) {
				int freq = (int) termsEnum.totalTermFreq();

				Int cnt = (Int) termFreqMap.get(term);
				if (cnt == null) {
					cnt = new Int();
					termFreqMap.put(term, cnt);
					cnt.x = freq;
				} else {
					cnt.x += freq;
				}
			}
		}
	}

	private void addTermFrequencies(Reader r, Map<String, Int> termFreqMap, String fieldName) throws IOException {
		TokenStream ts = analyzer.tokenStream(fieldName, r);
		ts.reset();
		int tokenCount = 0;
		CharTermAttribute termAttrib = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);
		while (ts.incrementToken()) {
			String word = termAttrib.toString();
			tokenCount++;
			if (tokenCount > maxNumTokensParsed) {
				break;
			}
			if (!isNoiseWord(word)) {
				Int cnt = (Int) termFreqMap.get(word);
				if (cnt == null)
					termFreqMap.put(word, new Int());
				else
					cnt.x += 1;
			}
		}
		ts.end();
		ts.close();
	}

	private boolean isNoiseWord(String term) {
		if ((stopWords != null) && (stopWords.contains(term))) {
			return true;
		}
		int len = term.length();
		if ((minWordLen > 0) && (len < minWordLen)) {
			return true;
		}
		if ((maxWordLen > 0) && (len > maxWordLen)) {
			return true;
		}
		return false;
	}

	public PriorityQueue<Object> retrieveTerms(Reader r) throws IOException {
		Map<String, Int> words = new HashMap<String, Int>();
		for (int i = 0; i < fieldNames.length; i++) {
			String fieldName = fieldNames[i];
			addTermFrequencies(r, words, fieldName);
		}
		return createQueue(words);
	}

	public String[] retrieveInterestingTerms(int docNum) throws IOException {
		ArrayList<Object> al = new ArrayList<Object>(maxQueryTerms);
		PriorityQueue<Object> pq = retrieveTerms(docNum);

		int lim = maxQueryTerms;
		Object cur;
		while (((cur = pq.pop()) != null) && (lim-- > 0)) {
			Object[] ar = (Object[]) cur;
			al.add(ar[0]);
		}
		String[] res = new String[al.size()];
		return (String[]) al.toArray(res);
	}

	public String[] retrieveInterestingTerms(Reader r) throws IOException {
		ArrayList<Object> al = new ArrayList<Object>(maxQueryTerms);
		PriorityQueue<Object> pq = retrieveTerms(r);

		int lim = maxQueryTerms;
		Object cur;
		while (((cur = pq.pop()) != null) && (lim-- > 0)) {
			Object[] ar = (Object[]) cur;
			al.add(ar[0]);
		}
		String[] res = new String[al.size()];
		return (String[]) al.toArray(res);
	}

	private static class Int {
		int x;

		Int() {
			x = 1;
		}
	}

	private static class FreqQ extends PriorityQueue<Object> {
		FreqQ(int s) {
			super(s);
		}

		protected boolean lessThan(Object a, Object b) {
			Object[] aa = (Object[]) a;
			Object[] bb = (Object[]) b;
			Float fa = (Float) aa[2];
			Float fb = (Float) bb[2];
			return fa.floatValue() > fb.floatValue();
		}
	}
}
