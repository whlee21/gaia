package gaia.handler;

public interface DocumentHighlighterParams {
	public static final String PREFIX = "dochl.";
	public static final String Q = "dochl.q";
	public static final String MODE = "dochl.mode";
	public static final String SOURCE = "dochl.source";
	public static final String XPATH = "dochl.xpath";
	public static final String BEGIN_MARKER = "dochl.beginMarker";
	public static final String END_MARKER = "dochl.endMarker";
	public static final String INCLUDE_ORIG_TEXT = "dochl.includeOrigText";
	public static final String FILENAME = "dochl.filename";
	public static final String DOCID = "dochl.docId";
	public static final String FL = "dochl.fl";
	public static final String FT = "dochl.ft";
	public static final String FT_PREFIX = "dochl.ft.";
	public static final String TEXT = "dochl.text";

	public static enum Source {
		SOLRCELL, STORED, XML, TEXT;
	}

	public static enum Mode {
		HIGHLIGHT, OFFSETS, BOTH;
	}
}
