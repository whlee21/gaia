package gaia.parser.gaia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.schema.SchemaField;

class GaiaSchemaField {
	public SchemaField schemaField;
	public GaiaSchemaFieldType type;
	public GaiaSchemaFields schemaFields = null;

	public GaiaSchemaField(GaiaSchemaFields qf, SchemaField sf) {
		schemaFields = qf;
		schemaField = sf;

		type = schemaFields.schemaFieldTypes.get(sf.getType());
	}

	public List<String> analyze(String text) throws IOException {
		List<String> terms = null;

		if (type == null) {
			terms = new ArrayList<String>();
			terms.add(text);
		} else {
			terms = type.analyze(getName(), text);
		}
		return terms;
	}

	public void determineFieldType() {
		if (type != null) {
			type.determineFieldType();

			if (getQueryFilter("WordDelimiter") == null)
				type.setIsText(false);
		}
	}

	public String filterTerm(String text, boolean keepStopWords, boolean keepWildcards, boolean stem,
			boolean keepEmbeddedPunctuation) throws IOException {
		if (type == null) {
			return "";
		}
		return type.filterTerm(this, text, keepStopWords, keepWildcards, stem, keepEmbeddedPunctuation);
	}

	public String getName() {
		if (schemaField == null) {
			return null;
		}
		return schemaField.getName();
	}

	public List<String> getSynonym(String term) {
		if (type == null) {
			return null;
		}
		return type.getSynonym(term);
	}

	public int getSynonymIndex(String term) {
		if (type == null) {
			return -1;
		}
		return type.getSynonymIndex(term);
	}

	public boolean getSynonymReplacementType(int i) {
		if (type == null) {
			return false;
		}
		return type.getSynonymReplacementType(i);
	}

	public List<String> getSynonymTargetTerms(int i) {
		if (type == null) {
			return null;
		}
		return type.getSynonymTargetTerms(i);
	}

	public TokenizerChain getQueryTokenizerChain() {
		if (type == null) {
			return null;
		}
		return type.getQueryTokenizerChain();
	}

	public TokenFilterFactory getQueryFilter(String tag) {
		if (type == null) {
			return null;
		}
		return type.getQueryFilter(tag);
	}

	public GaiaSchemaFieldType getType() {
		return type;
	}

	public String getTypeName() {
		if (type == null) {
			return null;
		}
		return type.getName();
	}

	public boolean isDate() {
		if (type == null) {
			return false;
		}
		return type.isDate;
	}

	public boolean isStopWord(String word) throws IOException {
		if (type == null) {
			return false;
		}
		return type.isStopWord(word);
	}

	public boolean isText() {
		if (type == null) {
			return false;
		}
		return type.isText;
	}

	public ArrayList<String> splitWords(String text, boolean stem) throws IOException {
		if (type == null) {
			ArrayList<String> words = new ArrayList<String>();
			words.add(text);
			return words;
		}
		return type.splitWords(text, stem);
	}

	public String stemWord(String words) throws IOException {
		if (type == null) {
			return words;
		}
		return type.stemWord(words);
	}
}
