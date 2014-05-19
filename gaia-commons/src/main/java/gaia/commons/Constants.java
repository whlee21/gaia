package gaia.commons;

import java.util.regex.Pattern;

public interface Constants {
	public static final String ZOOKEEPER_LOCATION = "zkhost";
	public static final String SOLR_ZK_PATH = "solrZkPath";
	public static final Pattern MATCH_ALL = Pattern.compile(".*");
	public static final String SERVICE_IMPL = "service-impl";
}
