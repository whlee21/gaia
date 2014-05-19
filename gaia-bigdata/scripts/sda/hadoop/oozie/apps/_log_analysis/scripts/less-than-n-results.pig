-- Queries that produced less than N results
-- Params
--  inputPath: Path to query logs
--  n: Number of results (default 100)

import 'common.pig';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hadoop-*.jar';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hbase-lib-*.jar';

%default n '100';

-- Load query logs
query_logs_1 = load_query_logs('$input');

-- Prune columns
query_logs_2 = foreach query_logs_1 generate
    collection,
    timetuple,
    hits;

-- Filter by less than N
query_logs_3 = filter query_logs_2 by (hits is null) or (hits < (int)'$n');

-- Count and generate HBase row/cols
results_less_than_n = foreach (group query_logs_3 by (timetuple, collection)) {
    generate
        group.$0 as timetuple,
        group.$1 as collection,
        COUNT(query_logs_3) as value; --metric "less-than-$n-results
}

to_hbase(results_less_than_n, 'daily_collection_metrics', 'lt-$n-results');
