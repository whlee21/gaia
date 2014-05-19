package gaia.analysis;

import java.util.ArrayList;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.lucene.util.Version;

class GaiaStemRules {
	private boolean frozen = false;
	private ArrayList<GaiaStemRule> suffixRules = new ArrayList<GaiaStemRule>();
	private CharArrayMap<String> wordMap = new CharArrayMap<String>(Version.LUCENE_45, 8, false);
	private GaiaStemRule[] rules;
	private byte[] quickCheck;

	public void add(GaiaStemRule rule) {
		if (frozen)
			throw new UnsupportedOperationException("cannot add to a frozen ruleset");
		if (rule.protectWord)
			wordMap.put(rule.suffix, rule.suffix);
		else if (rule.replaceWord)
			wordMap.put(rule.suffix, rule.newSuffix);
		else
			suffixRules.add(rule);
	}

	public void freeze() {
		rules = ((GaiaStemRule[]) suffixRules.toArray(new GaiaStemRule[suffixRules.size()]));
		frozen = true;
		suffixRules = null;
		quickCheck = new byte[''];

		for (Object key : wordMap.keySet()) {
			char[] chars = (char[]) key;
			if (chars.length > 0) {
				char finalch = chars[(chars.length - 1)];
				if (finalch < quickCheck.length) {
					quickCheck[finalch] = 1;
				}
			}
		}
		for (GaiaStemRule rule : rules)
			if (rule.suffixLen > 0) {
				char finalch = rule.suffix.charAt(rule.suffixLen - 1);
				if (finalch < quickCheck.length)
					quickCheck[finalch] = 1;
			}
	}

	public void mapWord(CharTermAttribute word) {
		char[] buffer = word.buffer();
		int len = word.length();

		if ((len == 0) || ((buffer[(len - 1)] < quickCheck.length) && (quickCheck[buffer[(len - 1)]] == 0))) {
			return;
		}
		String word2 = (String) wordMap.get(buffer, 0, len);
		if (word2 != null) {
			word.setEmpty().append(word2);
		} else {
			GaiaStemRule rule = findRule(buffer, len);
			if ((rule != null) && (!rule.protectSuffix)) {
				word.setLength(len - rule.suffixLen);

				word.append(rule.newSuffix);
			}
		}
	}

	public GaiaStemRule findRule(char[] word, int wordLen) {
		for (int i = 0; i < rules.length; i++) {
			GaiaStemRule rule = rules[i];

			if (wordLen >= rule.minLen) {
				if (rule.matchSuffix(word, wordLen)) {
					return rule;
				}
			}
		}
		return null;
	}
}
