package gaia.parser.gaia;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.ReversedWildcardFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.schema.DateField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.TrieDateField;
import org.apache.solr.schema.TrieField;

class GaiaSchemaFieldType {
	public GaiaQueryParser parser = null;
	public Version version;
	public GaiaSchemaFields schemaFields = null;
	public FieldType fieldType;
	public boolean isText = false;

	public boolean isDate = false;

	public boolean isTrie = false;

	boolean queriedAccentRemovalFilterFactory = false;

	public TokenFilterFactory accentRemovalFilterFactory = null;

	boolean queriedSynonymFilterFactory = false;

	public TokenFilterFactory synonymFilterFactory = null;

	public GaiaQuerySynonym querySynonym = null;

	boolean queriedStemFilterFactory = false;

	public TokenFilterFactory stemFilterFactory = null;

	boolean queriedStopFilterFactory = false;

	public StopFilterFactory stopFilterFactory = null;

	public GaiaStopwords stopwords = null;

	public Boolean directlyAccessStopwords = Boolean.valueOf(false);

	public Boolean useStopwordsSet = Boolean.valueOf(true);

	public CharArraySet stopwordsSet = null;

	boolean queriedWordDelimiterFilterFactory = false;

	public TokenFilterFactory wordDelimiterFilterFactory = null;

	public boolean reversedWildcardsIndexed = false;

	public ReversedWildcardFilterFactory reversedWildcardFilterFactory = null;

	public boolean stopwordsIndexed = true;

	public boolean stopwordPositionsIncremented = false;

	public void setIsText(boolean b) {
		isText = b;
	}

	public GaiaSchemaFieldType(GaiaQueryParser parser, GaiaSchemaFields qf, FieldType ft) {
		this.parser = parser;
		version = parser.core.getSolrConfig().luceneMatchVersion;
		schemaFields = qf;
		fieldType = ft;
		checkWildcardReversalSupport();
		checkStopwordsIndexed();
	}

	public void checkStopwordsIndexed() {
		stopwordsIndexed = true;

		if (fieldType != null) {
			Analyzer a = fieldType.getAnalyzer();
			if ((a instanceof TokenizerChain)) {
				TokenizerChain tc = (TokenizerChain) a;
				TokenFilterFactory[] factories = tc.getTokenFilterFactories();
				for (TokenFilterFactory factory : factories) {
					if ((factory instanceof StopFilterFactory)) {
						stopwordsIndexed = false;

						StopFilterFactory sf = (StopFilterFactory) factory;
						stopwordPositionsIncremented = sf.isEnablePositionIncrements();

						break;
					}
				}
			}
		}
	}

	public void checkWildcardReversalSupport() {
		reversedWildcardsIndexed = false;

		if (fieldType != null) {
			Analyzer a = fieldType.getAnalyzer();
			if ((a instanceof TokenizerChain)) {
				TokenizerChain tc = (TokenizerChain) a;
				TokenFilterFactory[] factories = tc.getTokenFilterFactories();
				for (TokenFilterFactory factory : factories) {
					if ((factory instanceof ReversedWildcardFilterFactory)) {
						reversedWildcardsIndexed = true;

						reversedWildcardFilterFactory = ((ReversedWildcardFilterFactory) factory);
						break;
					}
				}
			}
		}
	}

	public List<String> analyze(String fieldName, String text) throws IOException {
		List<String> terms = new ArrayList<String>();

		Analyzer a = fieldType.getQueryAnalyzer();
		TokenStream ts = null;
		try {
			ts = a.tokenStream(fieldName, new StringReader(text));
			if (ts == null)
				return terms;
			ts.reset();
		} catch (Exception e) {
			GaiaQueryParser.LOG.warn(getClass().getSimpleName()
					+ ".analyze: schema query analyzer tokenStream method got exception for field <<" + fieldName
					+ ">> of type <<" + getName() + ">>: " + e.getMessage() + " - term will be ignored for this field");
		}

		if (isTrie) {
			TermToBytesRefAttribute bytesAtt = (TermToBytesRefAttribute) ts.addAttribute(TermToBytesRefAttribute.class);
			BytesRef termBytes = bytesAtt.getBytesRef();
			String termText = "";
			try {
				if (ts.incrementToken()) {
					bytesAtt.fillBytesRef();
					termText = termBytes.utf8ToString();
				}
			} catch (Exception e) {
				GaiaQueryParser.LOG.warn(getClass().getSimpleName()
						+ ".analyze: schema query analyzer incrementToken method got exception for field <<" + fieldName
						+ ">> of type <<" + getName() + ">>: " + e.getMessage() + " - term will be ignored for this field");
			}

			terms.add(termText);
		} else {
			CharTermAttribute termAtt = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);
			PositionIncrementAttribute posIncrAtt = (PositionIncrementAttribute) ts
					.addAttribute(PositionIncrementAttribute.class);
			while (true) {
				try {
					if (!ts.incrementToken())
						break;
				} catch (Exception e) {
					GaiaQueryParser.LOG.warn(getClass().getSimpleName()
							+ ".analyze: schema query analyzer incrementToken method got exception for field <<" + fieldName
							+ ">> of type <<" + getName() + ">>: " + e.getMessage() + " - term will be ignored for this field");

					continue;
				}

				if ((posIncrAtt.getPositionIncrement() != 0) && (termAtt.length() != 0)) {
					terms.add(termAtt.toString());
				}
			}
		}

		ts.end();
		ts.close();

		return terms;
	}

	public void determineFieldType() {
		if (fieldType != null) {
			String fieldTypeName = fieldType.getTypeName();
			String textPrefix = schemaFields.schemaFieldTypes.textPrefix;
			int textPrefixLength = textPrefix.length();
			isText = false;
			isDate = false;
			isTrie = false;

			if ((textPrefixLength > 0) && (fieldTypeName.length() >= textPrefixLength)
					&& (fieldTypeName.substring(0, textPrefixLength).equalsIgnoreCase(textPrefix))) {
				isText = true;
			} else if (fieldTypeName.equalsIgnoreCase("date")) {
				isDate = true;
			}
			if ((fieldType instanceof DateField))
				isDate = true;
			if ((fieldType instanceof TrieDateField)) {
				isDate = true;
				isTrie = true;
			}
			if ((fieldType instanceof TrieField)) {
				isTrie = true;
				TrieField tf = (TrieField) fieldType;
				if (tf.getType() == TrieField.TrieTypes.DATE)
					isDate = true;
			}
		}
	}

	public String filterTerm(GaiaSchemaField field, String text, boolean keepStopWords, boolean keepWildcards,
			boolean stem, boolean keepEmbeddedPunctuation) throws IOException {
		String newText = "";

		if (isText) {
			int i = 0;
			int len = text.length();
			for (i = 0; i < len; i++) {
				char ch = text.charAt(i);
				if ((ch < 'a') || (ch > 'z'))
					break;
			}
			if (i == len) {
				if ((!keepStopWords) && (isStopWord(text))) {
					return null;
				}
				if (stem) {
					return stemWord(text);
				}
				return text;
			}

			if (!keepStopWords) {
				newText = text.toLowerCase();

				newText = stripEnclosingPunctuation(newText);

				if (isStopWord(newText)) {
					return null;
				}
			}

			ArrayList<String> words = null;
			if (keepEmbeddedPunctuation)
				words = splitWordsOnWhiteSpace(text);
			else
				words = splitWords(text, stem);
			int n = words.size();
			if (n == 1) {
				newText = (String) words.get(0);

				newText = removeAccents(newText);

				if (!keepWildcards) {
					newText = stripWildcards(newText);
				}
				if (newText.length() == 0)
					return null;
			} else {
				newText = "";
				for (i = 0; i < n; i++) {
					String newWord = (String) words.get(i);

					newWord = removeAccents(newWord);

					if (!keepWildcards) {
						newWord = stripWildcards(newWord);
					}
					if (newWord.length() > 0) {
						if (newText.length() > 0)
							newText = newText + " " + newWord;
						else {
							newText = newWord;
						}
					}
				}
			}
			newText = newText.toLowerCase();
		} else {
			List<String> words = null;
			if (field == null) {
				words = new ArrayList<String>();
				words.add(text);
			} else {
				words = field.analyze(text);
			}
			String newText1 = "";
			int n = 0;
			if (words != null)
				n = words.size();
			for (int i = 0; i < n; i++) {
				String word = (String) words.get(i);
				if (newText1.length() == 0)
					newText1 = newText1 + word;
				else
					newText1 = newText1 + " " + word;
			}
			newText = newText1;
		}

		if (newText.length() < 1) {
			return null;
		}
		return newText;
	}

	public String getName() {
		if (fieldType == null) {
			return null;
		}
		return fieldType.getTypeName();
	}

	public GaiaStopwords getStopwords() {
		if (stopwords == null) {
			CharArraySet stopset = getStopwordsSet();
			if (stopset == null) {
				return null;
			}
			stopwords = new GaiaStopwords(stopset);
		}

		return stopwords;
	}

	public CharArraySet getStopwordsSet() {
		if (stopwordsSet == null) {
			if (!queriedStopFilterFactory) {
				stopFilterFactory = ((StopFilterFactory) getQueryFilter("StopFilterFactory"));
				queriedStopFilterFactory = true;
			}
			if (stopFilterFactory == null) {
				return null;
			}
			stopwordsSet = stopFilterFactory.getStopWords();
		}

		return stopwordsSet;
	}

	public TokenizerChain getQueryTokenizerChain() {
		if (fieldType == null)
			return null;
		Analyzer qa = fieldType.getQueryAnalyzer();
		return (qa instanceof TokenizerChain) ? (TokenizerChain) qa : null;
	}

	public TokenFilterFactory getQueryFilter(String tag) {
		TokenizerChain tcq = getQueryTokenizerChain();
		if (tcq == null)
			return null;
		TokenFilterFactory[] facs = tcq.getTokenFilterFactories();

		for (int i = 0; i < facs.length; i++) {
			TokenFilterFactory tf = facs[i];
			if (tf.getClass().getName().indexOf(tag) >= 0) {
				return tf;
			}
		}
		return null;
	}

	public GaiaQuerySynonym getQuerySynonym() {
		if (querySynonym == null) {
			if (!queriedSynonymFilterFactory) {
				synonymFilterFactory = getQueryFilter("Synonym");
				queriedSynonymFilterFactory = true;
			}
			if (synonymFilterFactory == null) {
				return null;
			}
			Map<String, String> args = synonymFilterFactory.getOriginalArgs();

			String synonymFileName = (String) args.get("synonyms");
			if (synonymFileName.length() == 0) {
				return null;
			}

			querySynonym = new GaiaQuerySynonym(synonymFileName, schemaFields.schema.getResourceLoader());
		}

		return querySynonym;
	}

	public int getLongestSynonymTermCount() {
		GaiaQuerySynonym qs = getQuerySynonym();
		if (qs == null) {
			return 0;
		}
		return qs.getLongestSynonymTermCount();
	}

	public List<String> getSynonym(String term) {
		GaiaQuerySynonym qs = getQuerySynonym();
		if (qs == null) {
			return null;
		}
		List<String> syns = qs.getSynonym(term);

		return syns;
	}

	public int getSynonymIndex(String term) {
		GaiaQuerySynonym qs = getQuerySynonym();
		if (qs == null) {
			return -1;
		}
		int i = qs.getSynonymIndex(term);

		return i;
	}

	public boolean getSynonymReplacementType(int i) {
		GaiaQuerySynonym qs = getQuerySynonym();
		if (qs == null) {
			return false;
		}
		boolean replace = qs.getSynonymReplacementType(i);

		return replace;
	}

	public List<String> getSynonymTargetTerms(int i) {
		GaiaQuerySynonym qs = getQuerySynonym();
		if (qs == null) {
			return null;
		}
		List<String> syns = qs.getSynonymTargetTerms(i);

		return syns;
	}

	public boolean isStopWord(String s) throws IOException {
		if (directlyAccessStopwords.booleanValue()) {
			GaiaStopwords ls = getStopwords();
			if (ls != null) {
				return ls.isStopword(s);
			}
		}
		if (useStopwordsSet.booleanValue()) {
			CharArraySet ss = getStopwordsSet();
			if (ss == null) {
				return false;
			}
			return ss.contains(s);
		}

		String s1 = removeStopWords(s);
		return (s != null) && (s.length() > 0) && (s1 != null) && (s1.equals(""));
	}

	public String removeAccents(String word) throws IOException {
		int n = word.length();
		int i = 0;
		for (i = 0; i < n; i++) {
			char ch = word.charAt(i);
			if ((ch < 'a') || (ch > 'z')) {
				if ((ch < 'A') || (ch > 'Z')) {
					if ((ch < '0') || (ch > '9'))
						break;
				}
			}
		}
		if (i == n) {
			return word;
		}
		WhitespaceTokenizer wt = new WhitespaceTokenizer(Version.LUCENE_45, new StringReader(word));

		if (!queriedAccentRemovalFilterFactory) {
			accentRemovalFilterFactory = getQueryFilter("Accent");

			if (accentRemovalFilterFactory == null)
				accentRemovalFilterFactory = getQueryFilter("Folding");
			queriedAccentRemovalFilterFactory = true;
		}
		if (accentRemovalFilterFactory == null) {
			return word;
		}
		TokenFilter ts = (TokenFilter) accentRemovalFilterFactory.create(wt);
		ts.reset();

		CharTermAttribute termAtt = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);

		char[] termBuf = null;
		int termLen = -1;
		try {
			if (ts.incrementToken()) {
				termBuf = termAtt.buffer();
				termLen = termAtt.length();
			}
		} catch (Exception exception) {
		}

		ts.end();
		ts.close();
		if ((termBuf == null) || (termLen <= 0))
			return "";
		String newWord = new String(termBuf, 0, termLen);
		return newWord;
	}

	public String removeStopWords(String words) throws IOException {
		if (words == null) {
			return "";
		}

		int n = words.length();
		int i1 = 0;
		int i2 = n - 1;
		while ((i1 < n) && (!Character.isLetterOrDigit(words.charAt(i1)))) {
			i1++;
		}

		while ((i2 >= 0) && (!Character.isLetterOrDigit(words.charAt(i2)))) {
			i2--;
		}

		if ((i1 >= n) || (i2 < 0) || (i2 < i1))
			return words;
		if ((i1 > 0) || (i2 == n - 1)) {
			words = words.substring(i1, i2 + 1);
		}
		WhitespaceTokenizer wt = new WhitespaceTokenizer(Version.LUCENE_45, new StringReader(words));

		if (!queriedStopFilterFactory) {
			stopFilterFactory = ((StopFilterFactory) getQueryFilter("StopFilterFactory"));
			queriedStopFilterFactory = true;
		}
		if (stopFilterFactory == null) {
			wt.close();
			return words;
		}
		TokenFilter ts = (TokenFilter) stopFilterFactory.create(wt);
		ts.reset();
		CharTermAttribute termAtt = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);

		StringBuffer newWords = new StringBuffer();
		while (true) {
			char[] termBuf = null;
			int termLen = -1;
			try {
				if (ts.incrementToken()) {
					termBuf = termAtt.buffer();
					termLen = termAtt.length();
				}
			} catch (Exception exception) {
			}

			if ((termBuf == null) || (termLen <= 0))
				break;
			String word = new String(termBuf, 0, termLen);
			if (newWords.length() > 0)
				newWords.append(' ');
			newWords.append(word);
		}
		ts.end();
		ts.close();
		return newWords.toString();
	}

	public ArrayList<String> splitWords(String text) throws IOException {
		return splitWords(text, true);
	}

	public ArrayList<String> splitWords(String text, boolean stem) throws IOException {
		ArrayList<String> words = new ArrayList<String>();
		int k = 0;

		int n1 = text.length();
		int i1 = 0;
		for (i1 = 0; i1 < n1; i1++) {
			char ch = text.charAt(i1);
			if ((ch < 'a') || (ch > 'z')) {
				if ((ch < 'A') || (ch > 'Z'))
					break;
			}
		}
		if (i1 == n1) {
			if (stem) {
				String word2 = stemWord(text);
				words.add(word2);
			} else {
				words.add(text);
			}
			return words;
		}

		boolean useSchema = true;
		if ((text.contains("*")) || (text.contains("?"))) {
			useSchema = false;
		}

		if (useSchema) {
			if (!queriedWordDelimiterFilterFactory) {
				wordDelimiterFilterFactory = getQueryFilter("WordDelimiter");
				queriedWordDelimiterFilterFactory = true;
			}
			if (wordDelimiterFilterFactory == null)
				useSchema = false;
		}
		if (useSchema) {
			WhitespaceTokenizer wt = new WhitespaceTokenizer(Version.LUCENE_45, new StringReader(text));

			TokenFilter ts = (TokenFilter) wordDelimiterFilterFactory.create(wt);
			ts.reset();
			CharTermAttribute termAtt = (CharTermAttribute) ts.getAttribute(CharTermAttribute.class);
			while (true) {
				char[] termBuf = null;
				int termLen = -1;
				try {
					if (ts.incrementToken()) {
						termBuf = termAtt.buffer();
						termLen = termAtt.length();
					}
				} catch (Exception exception) {
				}

				if ((termBuf == null) || (termLen <= 0))
					break;
				String tempWord = new String(termBuf, 0, termLen);

				StringBuffer word = new StringBuffer();
				int n = tempWord.length();
				for (int i = 0; i < n; i++) {
					char ch = tempWord.charAt(i);
					if ((ch >= '') && (ch <= '¿')) {
						if (word.length() > 0) {
							words.add(word.toString());
							k++;
							word = new StringBuffer();
						}
					} else
						word.append(ch);

				}

				if (word.length() > 0) {
					if (stem) {
						String word2 = stemWord(word.toString());
						words.add(word2);
					} else {
						words.add(word.toString());
					}
					k++;
				}
			}
			ts.end();
			ts.close();
		} else {
			boolean splitLettersAndDigits = true;

			if (text.endsWith("'s")) {
				text = text.substring(0, text.length() - 2);
			}
			StringBuffer word = new StringBuffer();
			int n = text.length();
			for (int i = 0; i < n; i++) {
				char ch = text.charAt(i);
				if (((Character.isLetterOrDigit(ch)) && ((ch < '') || (ch > '¿'))) || (ch == '*') || (ch == '?')) {
					if ((splitLettersAndDigits) && (word.length() > 0)) {
						char ch1 = text.charAt(i - 1);
						if (Character.isLetter(ch1)) {
							if ((!Character.isLetter(ch)) && (ch != '*') && (ch != '?')) {
								words.add(word.toString());
								k++;
								word = new StringBuffer();
							}
						} else if ((Character.isDigit(ch1)) && (!Character.isDigit(ch)) && (ch != '*') && (ch != '?')) {
							words.add(word.toString());
							k++;
							word = new StringBuffer();
						}
					}

					word.append(ch);
				} else if (word.length() > 0) {
					words.add(word.toString());
					k++;
					word = new StringBuffer();
				}

			}

			if (word.length() > 0) {
				if (stem) {
					String word2 = stemWord(word.toString());
					words.add(word2);
				} else {
					words.add(word.toString());
				}
				k++;
				word = new StringBuffer();
			}
		}

		return words;
	}

	public ArrayList<String> splitWordsOnWhiteSpace(String text) {
		ArrayList<String> words = new ArrayList<String>();
		int k = 0;

		StringBuffer word = new StringBuffer();
		int n = text.length();
		for (int i = 0; i < n; i++) {
			char ch = text.charAt(i);
			if (Character.isWhitespace(ch)) {
				if (word.length() > 0) {
					words.add(word.toString());
					k++;
					word = new StringBuffer();
				}
			} else
				word.append(ch);

		}

		if (word.length() > 0) {
			words.add(word.toString());
			k++;
		}

		return words;
	}

	public String stemWord(String word) throws IOException {
		if (!queriedStemFilterFactory) {
			stemFilterFactory = getQueryFilter("Stem");
			if (stemFilterFactory == null)
				stemFilterFactory = getQueryFilter("Porter");
			queriedStemFilterFactory = true;
		}
		if (stemFilterFactory == null) {
			return word;
		}

		OneTermTokenStream ts = new OneTermTokenStream(word);

		TokenFilter tf = (TokenFilter) stemFilterFactory.create(ts);
		tf.reset();
		CharTermAttribute termAtt = (CharTermAttribute) tf.addAttribute(CharTermAttribute.class);

		char[] termBuf = null;
		int termLen = -1;
		try {
			if (tf.incrementToken()) {
				termBuf = termAtt.buffer();
				termLen = termAtt.length();
			}
		} catch (Exception exception) {
		}

		tf.end();
		tf.close();
		if ((termBuf == null) || (termLen <= 0)) {
			return null;
		}
		String stemmedWord = new String(termBuf, 0, termLen);
		return stemmedWord;
	}

	public String stripEnclosingPunctuation(String s) {
		int n = s.length();
		if ((s == null) || (n == 0)) {
			return s;
		}

		int i = 0;
		for (i = 0; i < n; i++) {
			char ch = s.charAt(i);
			if (!Character.isLetterOrDigit(ch))
				break;
		}
		if (i == n) {
			return s;
		}
		for (i = 0; i < n; i++) {
			char ch = s.charAt(i);
			if ((Character.isLetterOrDigit(ch)) || (ch == '*') || (ch == '?'))
				break;
		}
		if (i == n) {
			return "";
		}
		s = s.substring(i);
		n = s.length();
		if (n == 0) {
			return s;
		}
		for (i = n - 1; i >= 0; i--) {
			char ch = s.charAt(i);
			if ((Character.isLetterOrDigit(ch)) || (ch == '*') || (ch == '?'))
				break;
		}
		if (i < 0) {
			return "";
		}

		if (i < n - 1) {
			char ch = s.charAt(i + 1);
			if ((ch == '#') && (i >= 0) && (Character.isLetter(s.charAt(i)))) {
				i++;
			} else if ((ch == '+') && (i >= 0) && (Character.isLetter(s.charAt(i)))) {
				int j = 0;
				for (j = i + 2; j < n; j++) {
					ch = s.charAt(j);
					if (ch != '+') {
						break;
					}
				}
				if (j <= n) {
					i = j - 1;
				}
			}
		}
		String s1 = s.substring(0, i + 1);
		int n1 = s1.length();

		if (((s1.contains(".")) || ((n1 == 1) && (Character.isLetter(s1.charAt(0))))) && (n > i + 1)
				&& (s.charAt(i + 1) == '.')) {
			s1 = s1 + ".";
		}
		return s1;
	}

	public String stripWildcards(String s) {
		if (s == null)
			return null;
		if ((s.indexOf(42) < 0) && (s.indexOf(63) < 0)) {
			return s;
		}
		return s.replace("*", "").replace("?", "");
	}

	private class OneTermTokenStream extends TokenStream {
		private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
		private String word;

		public OneTermTokenStream(String word) {
			this.word = word;
		}

		public boolean incrementToken() throws IOException {
			if (word == null) {
				return false;
			}
			clearAttributes();
			termAtt.setEmpty().append(word);
			word = null;
			return true;
		}
	}
}
