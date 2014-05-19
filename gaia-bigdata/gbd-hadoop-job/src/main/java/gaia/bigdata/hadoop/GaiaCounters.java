package gaia.bigdata.hadoop;

public enum GaiaCounters {
	CLUSTER_JOINER_MAPPED_DOCUMENTS,
	CLUSTER_JOINER_MAPPED_CLUSTERIDS,
	CLUSTER_JOINER_MISSING_CLUSTERID,
	CLUSTER_JOINER_MISSING_DOCUMENT,
	TIKA_EXTRACT_FAILED,
	TIKA_FORCED_GC,
	BEHEMOTH_NO_TEXT,
	UNKNOWN_LOG_TYPE,
	BEHEMOTH_TO_SOLR_INDEXED,
	BEHEMOTH_TO_SOLR_FAILED,
	MATRIX_SIZE_COL,
	MATRIX_SIZE_ROW,
	DOC_KEY_NOT_FOUND,
	SIPS_TO_SOLR_INDEXED,
	SIPS_TO_SOLR_FAILED;
}
