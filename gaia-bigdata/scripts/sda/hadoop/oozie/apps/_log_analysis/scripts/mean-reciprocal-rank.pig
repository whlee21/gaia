-- Mean Reciprocal Rank
-- Params
--  input_q: Path to query logs
--  input_c: Path to click logs

import 'common.pig';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hadoop-*.jar';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hbase-lib-*.jar';


query_logs_1 = load_query_logs('$input_q');

click_logs_1 = load_click_logs('$input_c');

click_logs_2 = foreach click_logs_1 generate 
    id,
    collection,
    timestamp,
    timetuple,
    1.0/(position+1) as reciprocal_rank:double;

-- Implictly join the two sets and group by (collection, id)
group_queries_with_clicks = cogroup query_logs_1 by (collection, timetuple, id), click_logs_2 by (collection, timetuple, id);

-- Calculate reciprocal rank for each query (using the first click)
query_with_first_click = foreach group_queries_with_clicks {
    ordered_clicks = order click_logs_2 by timestamp;
    top_click = limit ordered_clicks 1;
    generate 
        group.$0 as collection,
        group.$1 as timetuple,
        flatten(query_logs_1.query) as query:chararray, 
        flatten(top_click.reciprocal_rank) as reciprocal_rank:double;
};

-- Calculate the mean reciprocal rank across all queries
mean_reciprocal_rank = foreach (group query_with_first_click by (collection, timetuple)) {
  generate 
    group.$0 as collection,
    group.$1 as timetuple,
    AVG(query_with_first_click.reciprocal_rank) as value; 
};

-- Output to HBase
to_hbase(mean_reciprocal_rank, 'daily_collection_metrics', 'mrr');
