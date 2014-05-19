package gaia.analysis;

import org.apache.lucene.analysis.util.StemmerUtil;

class GaiaStemRule {
	boolean valid = false;
	String suffix = "";
	int suffixLen = 0;
	int minLen = 0;
	boolean varLen = false;
	boolean minLenExplicit = false;
	boolean consonantRequired = false;
	String newSuffix = "";
	int newSuffixLen = 0;
	boolean protectWord = false;
	boolean protectSuffix = false;
	boolean replaceWord = false;
	boolean replaceSuffix = false;

	public GaiaStemRule(String ruleText) {
		if (ruleText == null) {
			return;
		}
		int n = ruleText.length();
		if (n == 0) {
			return;
		}

		int i = 0;
		for (i = 0; (i < n) && (Character.isWhitespace(ruleText.charAt(i))); i++)
			;
		if ((i < n) && (ruleText.charAt(i) == '*')) {
			varLen = true;
			i++;
		}

		while ((i < n) && (ruleText.charAt(i) == '?')) {
			minLenExplicit = true;
			minLen += 1;
			i++;
		}

		if ((i < n) && (ruleText.charAt(i) == '/')) {
			minLenExplicit = true;
			consonantRequired = true;
			minLen += 1;
			i++;
		}

		char ch = '\000';
		StringBuffer sb = new StringBuffer();
		while ((i < n) && (Character.isLetter(ch = ruleText.charAt(i)))) {
			ch = Character.toLowerCase(ch);
			sb.append(ch);
			minLen += 1;
			i++;
		}
		String s = sb.toString();
		int ns = s.length();

		if ((s == null) || (minLen <= 0)) {
			return;
		}

		suffix = s;
		suffixLen = ns;

		while ((i < n) && (Character.isWhitespace(ruleText.charAt(i)))) {
			i++;
		}

		if (i >= n) {
			if ((varLen) || (minLenExplicit))
				protectSuffix = true;
			else {
				protectWord = true;
			}

			valid = true;

			return;
		}

		ch = ruleText.charAt(i);
		char ch2 = '\000';
		char ch3 = '\000';
		if (i < n - 1)
			ch2 = ruleText.charAt(i + 1);
		if (i < n - 2)
			ch3 = ruleText.charAt(i + 2);
		if (ch == '>')
			i++;
		else if (ch == '=') {
			if (ch2 == '>')
				i += 2;
			else
				i++;
		} else if (ch == '-') {
			if (ch2 == '>')
				i += 2;
			else if (ch2 == '-') {
				if (ch3 == '>')
					i += 3;
				else {
					i += 2;
				}
			}
		}

		while ((i < n) && (Character.isWhitespace(ruleText.charAt(i)))) {
			i++;
		}

		for (; i < n; i++) {
			ch = ruleText.charAt(i);
			if ((ch != '*') && (ch != '?') && (ch != '/')) {
				break;
			}
		}
		ch = '\000';
		sb = new StringBuffer();
		while ((i < n) && (Character.isLetter(ch = ruleText.charAt(i)))) {
			ch = Character.toLowerCase(ch);
			sb.append(ch);
			i++;
		}
		s = sb.toString();
		ns = s.length();

		newSuffix = s;
		newSuffixLen = ns;

		if ((varLen) || (minLenExplicit))
			replaceSuffix = true;
		else {
			replaceWord = true;
		}

		valid = true;
	}

	static boolean isConsonant(char ch) {
		switch (ch) {
		case 'B':
		case 'C':
		case 'D':
		case 'F':
		case 'G':
		case 'H':
		case 'J':
		case 'K':
		case 'L':
		case 'M':
		case 'N':
		case 'P':
		case 'Q':
		case 'R':
		case 'T':
		case 'V':
		case 'W':
		case 'X':
		case 'Y':
		case 'Z':
		case 'b':
		case 'c':
		case 'd':
		case 'f':
		case 'g':
		case 'h':
		case 'j':
		case 'k':
		case 'l':
		case 'm':
		case 'n':
		case 'p':
		case 'q':
		case 'r':
		case 't':
		case 'v':
		case 'w':
		case 'x':
		case 'y':
		case 'z':
			return true;
		case 'E':
		case 'I':
		case 'O':
		case 'S':
		case 'U':
		case '[':
		case '\\':
		case ']':
		case '^':
		case '_':
		case '`':
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 's':
		case 'u':
		}
		return false;
	}

	public boolean matchSuffix(char[] word, int wordLen) {
		if ((!varLen) && (minLen > 0) && (suffixLen == 0)) {
			if (wordLen == minLen) {
				return true;
			}
			return false;
		}

		if (!StemmerUtil.endsWith(word, wordLen, suffix)) {
			return false;
		}

		if (consonantRequired) {
			if (isConsonant(word[(wordLen - suffixLen - 1)])) {
				return true;
			}
			return false;
		}
		return true;
	}
}
