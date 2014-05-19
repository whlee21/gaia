package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.index.Term;

class ParserTerm {
	public GaiaQueryParser parser;
	public ParserField field;
	public String text;
	public ParserTermModifiers modifiers;
	public FilteredParserTerm filteredTerm = null;

	public ParserTerm(GaiaQueryParser p, ParserTermModifiers o, String t) {
		parser = p;
		field = o.field;
		modifiers = o.clone();
		setText(t);
	}

	public void setText(String s) {
		text = s;
		filteredTerm = null;

		modifiers.hyphenated = false;
		if (s != null) {
			int termLen = s.length();
			for (int ci = 1; ci < termLen - 1; ci++)
				if (s.charAt(ci) == '-') {
					if ((Character.isLetterOrDigit(s.charAt(ci - 1))) && (Character.isLetterOrDigit(s.charAt(ci + 1)))) {
						modifiers.hyphenated = true;
					} else {
						modifiers.hyphenated = false;
						break;
					}
				}
		}
	}

	public String fieldName() {
		return field.fieldName();
	}

	public ParserTerm filterTerm(boolean keepStopWords, boolean keepWildcards) throws IOException {
		return filterTerm(keepStopWords, keepWildcards, true, true);
	}

	public ParserTerm filterTerm(boolean keepStopWords, boolean keepWildcards, boolean stem,
			boolean keepEmbeddedPunctuation) throws IOException {
		if ((filteredTerm == null) || (filteredTerm.keepStopWords != keepStopWords)
				|| (filteredTerm.keepWildcards != keepWildcards) || (filteredTerm.stem != stem)
				|| (filteredTerm.keepEmbeddedPunctuation != keepEmbeddedPunctuation)) {
			String newText = field.filterTerm(text, keepStopWords, (keepWildcards) && (modifiers.isWild), stem,
					keepEmbeddedPunctuation);
			filteredTerm = new FilteredParserTerm(parser, modifiers, text, newText, keepStopWords, keepWildcards, stem,
					keepEmbeddedPunctuation);
		}

		return filteredTerm.filteredTerm;
	}

	public Term getTerm() {
		return parser.getQueryGenerator().generateTerm(field.fieldName(), text);
	}

	public boolean isDate() {
		return field.isDate();
	}

	public boolean isStopWord(ParserField field, String s) throws IOException {
		return (parser.processStopwords) && (field.processStopwords) && (field.isStopWord(s));
	}

	public boolean isText() {
		return field.isText();
	}

	public ArrayList<String> splitWords(ParserField field, String text) throws IOException {
		return splitWords(field, text, true);
	}

	public ArrayList<String> splitWords(ParserField field, String text, boolean stem) throws IOException {
		if (field == null) {
			ArrayList<String> words = new ArrayList<String>();
			words.add(text);
			return words;
		}

		return field.splitWords(text, stem);
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

	public String stripWildcards(String s) {
		if ((s.indexOf('*') < 0) && (s.indexOf('?') < 0)) {
			return s;
		}
		return s.replace("*", "").replace("?", "");
	}

	public String toString() {
		return field.fieldName() + ":" + text;
	}
}
