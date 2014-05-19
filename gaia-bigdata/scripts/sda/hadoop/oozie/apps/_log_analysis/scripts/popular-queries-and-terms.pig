-- Popular queries and terms
-- Params
--  input: Path to query logs
--  n: Percentage of top results (default 10)

import 'common.pig';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hadoop-*.jar';
register 'hdfs:///oozie/apps/_log_analysis/lib/sda-hbase-lib-*.jar';

%default n '10';

-- Load query logs, filter out columns we don't need
query_logs_1 = load '$input' using PigStorage('\u001F') 
	as (type:chararray, collection:chararray, timestamp:long, query:chararray, id:chararray, hits:int);

query_logs_2 = foreach query_logs_1 generate 
    com.lucid.sda.pig.EpochToCalendar(timestamp, 'YEAR', 'MONTH', 'DATE') as timetuple, 
    collection,
    query;

-- Queries with counts
groups_with_counts = foreach (group query_logs_2 by (timetuple, collection, query)) {
    generate 
        group.$0 as timetuple,
        group.$1 as collection:chararray,
        group.$2 as query:chararray,
        COUNT(query_logs_2) as cnt:long;
};

-- Sum up the counts for total number of queries
query_totals = foreach (group groups_with_counts by (timetuple, collection)) {
    generate 
        group.$0 as timetuple,
        group.$1 as collection:chararray,
        SUM(groups_with_counts.cnt) as total:long;
};

-- Join groups with counts to total counts to get top N
query_counts_and_total = cogroup groups_with_counts by (timetuple, collection), query_totals by (timetuple, collection);
popular_queries = foreach query_counts_and_total {
    --count = (int)(MAX(query_totals.total) * (((int)'$n')/ 100.0)); -- MAX is just so we get the single value
    --n = (int)((count == 0) ? 1 : count);
    top_queries = TOP(10, 3, groups_with_counts);
    generate
        group.$0 as timetuple,
        group.$1 as collection,
        com.lucid.sda.pig.Extract(top_queries.query, 0) as value;
};

to_hbase(popular_queries, 'daily_collection_metrics', 'popular-queries');

-- Use grouped query data to generate terms
terms = foreach groups_with_counts {
    split_terms = TOKENIZE(query);
    generate timetuple, collection, flatten(split_terms) as term:chararray, cnt;
};

term_totals = foreach (group terms by (timetuple, collection)) {
    generate 
        group.$0 as timetuple,
        group.$1 as collection:chararray,
        SUM(terms.cnt) as total:long;
}

term_counts_and_total = cogroup terms by (timetuple, collection), term_totals by (timetuple, collection);

popular_terms = foreach term_counts_and_total {
    --count = (int)(MAX(term_totals.total) * (((int)'$n') / 100.0)); -- MAX is just so we get the single value
    --n = (int)((count == 0) ? 1 : count);
    top_terms = TOP(10, 3, terms);
    generate
        group.$0 as timetuple,
        group.$1 as collection,
        com.lucid.sda.pig.Extract(top_terms.term, 0) as value;
};

to_hbase(popular_terms, 'daily_collection_metrics', 'popular-terms');
