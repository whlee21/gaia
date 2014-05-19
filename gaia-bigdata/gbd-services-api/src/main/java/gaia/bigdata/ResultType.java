package gaia.bigdata;

public enum ResultType {
	DOCUMENTS,
	QUERY,
	CLUSTERS,
	TOPICS,
	FACET,
	HIGHLIGHT,
	SUGGEST,
	SPELL,
	MOST_POPULAR,
	STATISTICALLY_INTERESTING_PHRASES,
	DUPLICATES,
	NEVER_SEEN,
	FAILED;

	private String requestName;

	private ResultType() {
		requestName = name().toLowerCase();
	}

	public String getRequestName() {
		return requestName;
	}
}
