package gaia.parser.gaia;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.core.SolrResourceLoader;

class GaiaQuerySynonym {
	public String mappingSep = "=>";
	public String termSep = ",";

	public String synonymFileName = null;
	public List<String> rules = null;
	public ArrayList<ArrayList<String>> synonymSourceTermLists = null;
	public ArrayList<Boolean> synonymReplacement = null;
	public ArrayList<String> synonymSourceTerms = null;
	public ArrayList<ArrayList<String>> synonymTargetTerms = null;

	public int longestSynonymTermCount = 0;

	public int getLongestSynonymTermCount() {
		return longestSynonymTermCount;
	}

	public GaiaQuerySynonym(String synonymFileName, SolrResourceLoader loader) {
		load(synonymFileName, loader);
	}

	public void load(String synonymFileName, ResourceLoader loader) {
		synonymReplacement = new ArrayList<Boolean>();
		synonymSourceTermLists = new ArrayList<ArrayList<String>>();
		synonymSourceTerms = new ArrayList<String>();
		synonymTargetTerms = new ArrayList<ArrayList<String>>();

		longestSynonymTermCount = 0;

		this.synonymFileName = synonymFileName;

		if ((synonymFileName != null) && (synonymFileName.length() > 0)) {
			rules = null;
			InputStream resource = null;
			try {
				resource = loader.openResource(synonymFileName);
				rules = WordlistLoader.getLines(resource, IOUtils.CHARSET_UTF_8);
			} catch (IOException e) {
				return;
			} finally {
				IOUtils.closeWhileHandlingException(new Closeable[] { resource });
			}

			for (String rule : rules) {
				List ruleMapping = StrUtils.splitSmart(rule, mappingSep, false);

				String source = "";
				String target = "";

				int n = ruleMapping.size();
				boolean isReplacement = false;
				if (n == 2) {
					isReplacement = true;
					source = (String) ruleMapping.get(0);
					target = (String) ruleMapping.get(1);
				} else if (n == 1) {
					isReplacement = false;
					source = (String) ruleMapping.get(0);
					target = source;
				}

				List<String> sourceTerms = StrUtils.splitSmart(source, termSep, false);
				int ns = sourceTerms.size();
				for (int i = 0; i < ns; i++) {
					String sourceTerm = (String) sourceTerms.get(i);

					String cleanSourceTerm = "";
					int len = sourceTerm.length();
					boolean inWord = false;
					int wordCount = 0;
					for (int i1 = 0; i1 < len; i1++) {
						char ch = sourceTerm.charAt(i1);
						if (Character.isWhitespace(ch)) {
							inWord = false;
						} else {
							if (!inWord) {
								if (wordCount > 0)
									cleanSourceTerm = cleanSourceTerm + " ";
								inWord = true;
								wordCount++;
							}
							cleanSourceTerm = cleanSourceTerm + ch;
						}

					}

					if (wordCount > longestSynonymTermCount) {
						longestSynonymTermCount = wordCount;
					}
					sourceTerms.set(i, cleanSourceTerm);
				}
				List<String> targetTerms = StrUtils.splitSmart(target, termSep, false);
				int nt = targetTerms.size();
				for (int i = 0; i < nt; i++) {
					String targetTerm = (String) targetTerms.get(i);
					targetTerm = targetTerm.trim();
					targetTerms.set(i, targetTerm);
				}

				for (String sourceTerm : sourceTerms) {
					int i = synonymSourceTerms.indexOf(sourceTerm);
					if (i >= 0) {
						boolean wasReplacement = ((Boolean) synonymReplacement.get(i)).booleanValue();
						if ((!wasReplacement) && (isReplacement)) {
							isReplacement = false;

							targetTerms = sourceTerms;
						}

					}

				}

				ArrayList<String> newSourceTermList = new ArrayList<String>();
				ArrayList<String> newTargetTermList = new ArrayList<String>();
				for (String sourceTerm : sourceTerms) {
					int i = synonymSourceTerms.indexOf(sourceTerm);
					if (i >= 0) {
						ArrayList<String> curTermList = synonymSourceTermLists.get(i);
						for (String curTerm : curTermList) {
							int j = newSourceTermList.indexOf(curTerm);
							if (j < 0) {
								newSourceTermList.add(curTerm);
							}

						}

						ArrayList<String> curTargetTermList = synonymTargetTerms.get(i);
						boolean wasReplacement = ((Boolean) synonymReplacement.get(i)).booleanValue();
						if ((wasReplacement) && (!isReplacement)) {
							curTargetTermList = synonymSourceTermLists.get(i);

							synonymReplacement.set(i, Boolean.valueOf(false));
						}
						for (String curTerm : curTargetTermList) {
							int j = newTargetTermList.indexOf(curTerm);
							if (j < 0) {
								newTargetTermList.add(curTerm);
							}
						}
					}
				}

				for (String sourceTerm : sourceTerms) {
					if (newSourceTermList.indexOf(sourceTerm) < 0) {
						newSourceTermList.add(sourceTerm);
					}
				}
				for (String targetTerm : targetTerms) {
					if (newTargetTermList.indexOf(targetTerm) < 0) {
						newTargetTermList.add(targetTerm);
					}
				}

				for (String sourceTerm : newSourceTermList) {
					int i = synonymSourceTerms.indexOf(sourceTerm);

					if (i == -1) {
						synonymReplacement.add(Boolean.valueOf(isReplacement));

						synonymSourceTerms.add(sourceTerm);

						synonymSourceTermLists.add(newSourceTermList);

						synonymTargetTerms.add(newTargetTermList);
					} else {
						synonymSourceTerms.set(i, sourceTerm);

						synonymSourceTermLists.set(i, newSourceTermList);

						synonymTargetTerms.set(i, newTargetTermList);

						if ((((Boolean) synonymReplacement.get(i)).booleanValue()) && (!isReplacement))
							synonymReplacement.set(i, Boolean.valueOf(false));
					}
				}
			}
		}
	}

	public List<String> getSynonym(String term) {
		return getSynonym(term, false);
	}

	public List<String> getSynonym(String term, boolean matchCase) {
		int n = synonymSourceTerms.size();
		for (int i = 0; i < n; i++) {
			String synonymSourceTerm = (String) synonymSourceTerms.get(i);
			boolean matched = false;
			if (matchCase)
				matched = synonymSourceTerm.equals(term);
			else
				matched = synonymSourceTerm.equalsIgnoreCase(term);
			if (matched) {
				return (List) synonymTargetTerms.get(i);
			}
		}

		return null;
	}

	public int getSynonymIndex(String term) {
		return getSynonymIndex(term, false);
	}

	public int getSynonymIndex(String term, boolean matchCase) {
		int n = synonymSourceTerms.size();
		for (int i = 0; i < n; i++) {
			String synonymSourceTerm = (String) synonymSourceTerms.get(i);
			boolean matched = false;
			if (matchCase)
				matched = synonymSourceTerm.equals(term);
			else
				matched = synonymSourceTerm.equalsIgnoreCase(term);
			if (matched) {
				return i;
			}
		}

		return -1;
	}

	public boolean getSynonymReplacementType(int i) {
		if (i >= 0) {
			return ((Boolean) synonymReplacement.get(i)).booleanValue();
		}
		return false;
	}

	public List<String> getSynonymTargetTerms(int i) {
		if (i >= 0) {
			return (List) synonymTargetTerms.get(i);
		}
		return null;
	}
}
