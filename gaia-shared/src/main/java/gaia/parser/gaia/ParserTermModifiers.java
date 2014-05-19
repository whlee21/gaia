package gaia.parser.gaia;

import org.apache.solr.parser.QueryParser;

class ParserTermModifiers implements Cloneable {
	public boolean quoted = false;

	public boolean isWild = false;

	public boolean isAllStarWild = false;

	public boolean hasAllStarWildSuffix = false;

	public boolean expandSynonyms = true;

	public boolean synonymExpansion = false;

	public boolean suppressStopWords = true;

	public boolean stemWords = true;

	public ParserField field = null;

	public boolean allFields = false;

	public QueryParser.Operator defaultOperator = QueryParser.Operator.AND;

	public boolean hyphenated = false;

	public int termCountHardCutoff = 0;

	public int minMatchPercent = 0;

	public int minMatch = 0;

	public ParserTermModifiers clone() {
		try {
			return (ParserTermModifiers) super.clone();
		} catch (Exception exception) {
		}
		return null;
	}
}
