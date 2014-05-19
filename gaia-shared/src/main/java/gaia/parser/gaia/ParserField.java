package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.util.TokenFilterFactory;

class ParserField {
	public static final String allFieldName = "ALL";
	public static final String allFieldNameLower = "all";
	public static final String defaultFieldName = "DEFAULT";
	public static final String defaultFieldNameLower = "default";
	public static final String valFieldName = "_val_";
	public GaiaQueryParser parser = null;

	public GaiaSchemaField schemaField = null;
	public static final float defaultAllQueryBoost = 1.0F;
	public float allQueryBoost = 1.0F;
	public static final float defaultQueryBoost = 1.0F;
	public float queryBoost = 1.0F;
	public static final float defaultBigramBoost = 1.0F;
	public float bigramRelevancyBoost = 1.0F;
	public static final float defaultTrigramBoost = 1.0F;
	public float trigramRelevancyBoost = 1.0F;

	public boolean defaultIsText = false;

	public boolean isAll = false;
	public boolean isDefault = false;
	public boolean isVal = false;

	public boolean isAllField = false;
	public boolean isQueryField = false;

	public boolean expandSynonyms = false;

	public boolean processStopwords = false;

	public void setAllQueryBoost(float f) {
		allQueryBoost = f;
	}

	public void setQueryBoost(float f) {
		queryBoost = f;
	}

	public void setBigramRelevancyBoost(float f) {
		bigramRelevancyBoost = f;
	}

	public void setTrigramRelevancyBoost(float f) {
		trigramRelevancyBoost = f;
	}

	public void setExpandSynonyms(boolean b) {
		expandSynonyms = b;
	}

	public void setProcessStopwords(boolean b) {
		processStopwords = b;
	}

	public void determineFieldType() {
		if (schemaField != null)
			schemaField.determineFieldType();
	}

	public ParserField(GaiaQueryParser p, String fieldName) {
		parser = p;

		if (fieldName.equals("ALL")) {
			isAll = true;
		} else if (fieldName.equals("DEFAULT")) {
			isDefault = true;
		} else if (fieldName.equals("_val_")) {
			isVal = true;
		} else {
			schemaField = parser.schemaFields.get(fieldName);
			determineFieldType();
		}
	}

	public List<String> analyze(String text) throws IOException {
		return schemaField.analyze(text);
	}

	public String fieldName() {
		if (isAll)
			return "ALL";
		if (isDefault)
			return "DEFAULT";
		if (isVal)
			return "_val_";
		if (schemaField != null) {
			return schemaField.getName();
		}
		return null;
	}

	public String filterTerm(String text, boolean keepStopWords, boolean keepWildcards) throws IOException {
		return filterTerm(text, keepStopWords, keepWildcards, true, true);
	}

	public String filterTerm(String text, boolean keepStopWords, boolean keepWildcards, boolean stem,
			boolean keepEmbeddedPunctuation) throws IOException {
		if (schemaField == null) {
			return "";
		}
		return schemaField.filterTerm(text, keepStopWords, keepWildcards, stem, keepEmbeddedPunctuation);
	}

	public TokenFilterFactory getQueryFilter(String tag) {
		if (schemaField == null) {
			return null;
		}
		return schemaField.getQueryFilter(tag);
	}

	public List<String> getSynonym(String term) {
		if (schemaField == null) {
			return null;
		}
		return schemaField.getSynonym(term);
	}

	public int getSynonymIndex(String term) {
		if (schemaField == null) {
			return -1;
		}
		return schemaField.getSynonymIndex(term);
	}

	public boolean getSynonymReplacementType(int i) {
		if (schemaField == null) {
			return false;
		}
		return schemaField.getSynonymReplacementType(i);
	}

	public List<String> getSynonymTargetTerms(int i) {
		if (schemaField == null) {
			return null;
		}
		return schemaField.getSynonymTargetTerms(i);
	}

	public String getTypeName() {
		if (schemaField == null) {
			return null;
		}
		return schemaField.getTypeName();
	}

	public boolean isText() {
		if ((isDefault) || (isAll))
			return defaultIsText;
		if (schemaField == null) {
			return false;
		}
		return schemaField.isText();
	}

	public boolean isDate() {
		if (schemaField == null) {
			return false;
		}
		return schemaField.isDate();
	}

	public boolean isStopWord(String word) throws IOException {
		if (schemaField == null) {
			return false;
		}
		return schemaField.isStopWord(word);
	}

	public ArrayList<String> splitWords(String text, boolean stem) throws IOException {
		if (schemaField == null) {
			ArrayList<String> words = new ArrayList<String>();
			words.add(text);
			return words;
		}

		return schemaField.splitWords(text, stem);
	}

	public String stemWord(String words) throws IOException {
		if (schemaField == null) {
			return words;
		}
		return schemaField.stemWord(words);
	}
}
