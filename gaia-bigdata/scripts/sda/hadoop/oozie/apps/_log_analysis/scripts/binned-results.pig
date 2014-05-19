-- Histogram of query result counts 
-- Params
--  input: Path to query logs

import 'common.pig';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hadoop-*.jar';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hbase-lib-*.jar';

query_logs = load_query_logs('$input');

query_logs_with_hits = has_hits(query_logs);

-- Get distinct queries
hit_counts = foreach (group query_logs_with_hits by (timetuple, collection, query)) {
  generate
    group.$0 as timetuple,
    group.$1 as collection,
    group.$2 as query,
    (int)MAX(query_logs_with_hits.hits) as hits:int;
};

-- Determine the min/max of the data and the number of bins to use
hist_props = foreach (group hit_counts by (timetuple, collection)) {
    min = MIN(hit_counts.hits);
    max = MAX(hit_counts.hits);
    nbins = (int) CEIL(LOG(COUNT(hit_counts.hits)) + 1);
    generate flatten(group), min as min:int, max as max:int, nbins as nbins:int, hit_counts.hits as hits;
};

-- Calculate the histograms
histograms = foreach hist_props {
  generate 
    $0 as timetuple,
    $1 as collection,
    TOTUPLE(min, max, nbins, com.lucid.sda.pig.Histogram(hits, min, max, nbins)) as value; -- metric "query-results-histogram"
};

to_hbase(histograms, 'daily_collection_metrics', 'results-hist');
