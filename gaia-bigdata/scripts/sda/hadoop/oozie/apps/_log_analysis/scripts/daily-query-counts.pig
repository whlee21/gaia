-- Query counts
-- Params:
--  input: Path to query logs

import 'common.pig';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hadoop-*.jar';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hbase-lib-*.jar';

-- Load query logs, keep only records with hits
query_logs_1 = load_query_logs('$input');
query_logs_2 = has_hits(query_logs_1);

query_groups = group query_logs_2 by (timetuple, collection);

-- Distinct queries
distinct_counts = foreach query_groups {
  distinct_queries = distinct query_logs_2.query;
  generate
    group.$0 as timetuple,
    group.$1 as collection:chararray,
    COUNT(distinct_queries) as value;
};

-- All queries
total_counts = foreach query_groups {
  generate
    group.$0 as timetuple,
    group.$1 as collection:chararray,
    COUNT(query_logs_2.query) as value;
};  

-- Store in HBase
to_hbase(distinct_counts, 'daily_collection_metrics', 'distinct-queries');
to_hbase(total_counts, 'daily_collection_metrics', 'total-queries');

