package gaia.spellchecking.teragram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.spelling.SolrSpellChecker;
import org.apache.solr.spelling.SpellingOptions;
import org.apache.solr.spelling.SpellingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teragram.spelling.QSpeller;
import com.teragram.spelling.QSpellerContext;
import com.teragram.spelling.QSpellerIterator;
import com.teragram.spelling.QSpellerResult;

public class TeragramSolrSpeller extends SolrSpellChecker {
	private static transient Logger LOG = LoggerFactory.getLogger(TeragramSolrSpeller.class);

	private static final boolean JNI_LIB_FOUND;
	private static final Throwable JNI_LOAD_PROBLEM;
	private File configFile;
	private File listFile;
	private File freqFile;
	public static final String CONFIG_FILE = "configFile";
	public static final String THRESHOLD = "threshold";
	public static final String FIELD_NAME = "field";
	int minFreq;
	int minTermLength;
	static final String INPUT_LIST_FNAME = "input_list.txt";
	static final String INPUT_FREQ_FNAME = "input_freq.txt";
	private boolean enabled;
	private static final int DEFAULT_THRESHOLD = 80;
	private int threshold;
	private QSpeller qspeller;
	private String fieldName;
	private float maxFreq;
	public static final String MIN_TERM_LENGTH = "minTermLength";
	public static final String MIN_FREQ = "minFreq";
	private static Comparator<SpellHolder> spellHolderComparator = new Comparator<SpellHolder>() {
		public int compare(TeragramSolrSpeller.SpellHolder spellHolder, TeragramSolrSpeller.SpellHolder spellHolder1) {
			int result = spellHolder1.freq - spellHolder.freq;
			return result != 0 ? result : spellHolder1.suggestion.compareToIgnoreCase(spellHolder.suggestion);
		}
	};

	public TeragramSolrSpeller() {
		enabled = false;

		threshold = DEFAULT_THRESHOLD;
	}

	public static boolean isTeragramAvailable() {
		return JNI_LIB_FOUND;
	}

	public long writeFiles(IndexReader reader, Writer listWriter, Writer freqWriter) throws IOException {
		List<MyTermFreq> lines = new ArrayList<MyTermFreq>();
		List<Integer> freqs = new ArrayList<Integer>();
		try {
			Fields f = MultiFields.getFields(reader);
			Terms terms = f.terms(fieldName);
			TermsEnum termsEnum = terms.iterator(null);
			while (termsEnum.next() != null) {
				BytesRef termByte = termsEnum.term();
				int freq = termsEnum.docFreq();
				String termText = termByte.utf8ToString().trim();
				if ((freq >= minFreq) && (termText.length() >= minTermLength)) {
					freqs.add(Integer.valueOf(freq));
					MyTermFreq tf = new MyTermFreq();
					tf.term = termText;
					tf.freq = freq;
					lines.add(tf);
				}
			}
		} catch (IOException e) {
			throw new IOException("Failure reading terms for field " + fieldName, e);
		}

		int maxFreq = getMaxInNthPercentile(freqs, 97);
		try {
			for (MyTermFreq tf : lines) {
				listWriter.write(tf.term + "\n");
				freqWriter.write(tf.term + "," + Math.round(Math.min(maxFreq, tf.freq) / maxFreq * 10.0F) + "\n");
			}
		} catch (IOException e) {
			throw new IOException("Failure writing term list and freq", e);
		}

		return lines.size();
	}

	public void build(SolrCore core, SolrIndexSearcher searcher) {
		if (enabled == true) {
			LOG.info("Building Teragram Dictionary");
			IndexReader reader = searcher.getIndexReader();
			Writer inputListWriter = null;
			Writer inputFreqListWriter = null;
			try {
				inputListWriter = new BufferedWriter(new FileWriter(listFile));
				inputFreqListWriter = new BufferedWriter(new FileWriter(freqFile));

				long numTermsWritten = writeFiles(reader, inputListWriter, inputFreqListWriter);

				inputListWriter.close();
				inputFreqListWriter.close();

				qspeller = new QSpeller(configFile);
				LOG.info("Done building Teragram Dictionary.  Wrote: " + numTermsWritten + " to " + listFile);
			} catch (Exception e) {
				LOG.error("Exception", e);
			} finally {
				try {
					if (inputListWriter != null) {
						inputListWriter.close();
					}
					if (inputFreqListWriter != null)
						inputFreqListWriter.close();
				} catch (IOException e) {
					LOG.warn("Exception", e);
				}
			}
		}
	}

	private int getMaxInNthPercentile(List<Integer> freqs, int n) throws IOException {
		int count = freqs.size();
		if (count > 1) {
			int[] valuesCopy = new int[count];
			for (int i = 0; i < count; i++) {
				valuesCopy[i] = ((Integer) freqs.get(i)).intValue();
			}
			Arrays.sort(valuesCopy);
			double percent = n;
			double topPercentile = (100.0D - percent) / 100.0D;
			count -= (int) Math.ceil(count * topPercentile);
			if (count > 0) {
				return valuesCopy[(count - 1)];
			}
		}

		return 0;
	}

	public SpellingResult getSuggestions(SpellingOptions options) throws IOException {
		SpellingResult result = null;
		if (enabled == true) {
			QSpellerContext ctx = null;
			try {
				ctx = qspeller.newContext();
				result = new SpellingResult();
				result.setTokens(options.tokens);
				Term term = null;
				if ((options.extendedResults == true) && (options.reader != null) && (fieldName != null)) {
					term = new Term(fieldName, "");
				}
				for (Token token : options.tokens) {
					String termText = new String(token.buffer(), 0, token.length());
					int docFreq = 0;
					if (term != null) {
						term = new Term(fieldName, termText);
						docFreq = options.reader.docFreq(term);
						docFreq = Math.round(docFreq / maxFreq * 10.0F);
						result.addFrequency(token, docFreq);
					}
					SortedSet<SpellHolder> tmp = new TreeSet<SpellHolder>(spellHolderComparator);
					try {
						QSpellerIterator iter = ctx.suggestForCorrect(termText);
						if (iter != null) {
							while (iter.hasNext()) {
								QSpellerResult spelling = (QSpellerResult) iter.next();

								int numSuggestions = spelling.getNbSuggestions();
								if (numSuggestions != 0) {
									for (int i = 0; i < numSuggestions; i++) {
										int freq = spelling.getNthSuggestionFrequency(i);
										if ((spelling.getNthSuggestionWeight(i) >= threshold) && (freq >= docFreq)) {
											tmp.add(new SpellHolder(spelling.getNthSuggestion(i), termText, freq));
										}
									}
								}
							}
						}

						int limit = Math.min(options.count, tmp.size());
						int i = 0;
						if (tmp.size() > 1) {
							for (Iterator<SpellHolder> holderIterator = tmp.iterator(); (holderIterator.hasNext()) && (i < limit); i++) {
								SpellHolder spellHolder = holderIterator.next();

								if (!spellHolder.orig.equalsIgnoreCase(spellHolder.suggestion))
									result.add(token, spellHolder.suggestion, spellHolder.freq);
							}
						} else if (tmp.size() == 1) {
							SpellHolder spellHolder = (SpellHolder) tmp.iterator().next();
							if (!spellHolder.orig.equalsIgnoreCase(spellHolder.suggestion))
								result.add(token, spellHolder.suggestion, spellHolder.freq);
						}
					} catch (Exception e) {
						LOG.error("Couldn't spell check: " + termText, e);
					}
				}
			} catch (Exception e) {
				LOG.error("Couldn't create a spelling context, disabling spelling", e);
				enabled = false;
				return null;
			}
		}
		return result;
	}

	public String init(NamedList config, SolrCore core) {
		super.init(config, core);

		SolrParams params = SolrParams.toSolrParams(config);
		configFile = new File(params.get("configFile", "./plugins/espell/simple.config"));

		minFreq = params.getInt("minFreq", 0);
		minTermLength = params.getInt("minTermLength", 0);
		threshold = params.getInt("threshold", DEFAULT_THRESHOLD);
		fieldName = params.get("field", null);

		if (isTeragramAvailable()) {
			if (!configFile.exists()) {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, configFile + "does not exist");
			}

			File workingDir = configFile.getParentFile();
			listFile = new File(workingDir, "input_list.txt");
			try {
				listFile.createNewFile();
			} catch (Exception e) {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, listFile + "can't be created", e);
			}

			freqFile = new File(workingDir, "input_freq.txt");
			try {
				freqFile.createNewFile();
			} catch (Exception e) {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, freqFile + "can't be created", e);
			}

			enableQSpeller();
		} else {
			LOG.warn("Teregram libraries can not be found, this Speller will not be usable", JNI_LOAD_PROBLEM);
		}

		return name;
	}

	private void enableQSpeller() {
		if (isTeragramAvailable()) {
			try {
				qspeller = new QSpeller(configFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			enabled = true;
		} else {
			enabled = false;
		}
	}

	public File getConfigFile() {
		return configFile;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getFieldName() {
		return fieldName;
	}

	public int getThreshold() {
		return threshold;
	}

	public int getMinFreq() {
		return minFreq;
	}

	public int getMinTermLength() {
		return minTermLength;
	}

	public void reload(SolrCore core, SolrIndexSearcher searcher) throws IOException {
		enableQSpeller();
	}

	static {
		Throwable problem = null;
		try {
			System.loadLibrary("tgjni_spelling");
		} catch (Throwable t) {
			problem = t;
		}
		JNI_LOAD_PROBLEM = problem;
		JNI_LIB_FOUND = problem == null;
	}

	private class SpellHolder {
		String orig;
		String suggestion;
		int freq;

		private SpellHolder(String suggestion, String orig, int freq) {
			this.suggestion = suggestion;
			this.orig = orig;
			this.freq = freq;
		}
	}

	private class MyTermFreq {
		String term;
		int freq;
	}
}
