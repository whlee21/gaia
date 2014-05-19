define load_query_logs(inPath) returns query_logs {
    query_logs_1 = load '$inPath' using PigStorage('\u001F') 
        as (type:chararray, collection:chararray, timestamp:long, query:chararray, id:chararray, hits:int);

    query_logs_2 = filter query_logs_1 by 
        (collection is not null) and (timestamp is not null) and 
        (query is not null) and (id is not null);

    $query_logs = foreach query_logs_1 generate
        collection,
        com.lucid.sda.pig.EpochToCalendar(timestamp, 'YEAR', 'MONTH', 'DATE') as timetuple,
        timestamp,
        query,
        id,
        hits;
};

define load_click_logs(inPath) returns click_logs {
    click_logs_1 = load '$inPath' using PigStorage('\u001F') 
        as (type:chararray, collection:chararray, timestamp:long, id:chararray, url:chararray, position:int);

    click_logs_2 = filter click_logs_1 by 
        (collection is not null) and (timestamp is not null) and
        (position is not null) and (id is not null);

    $click_logs = foreach click_logs_2 generate
        collection,
        com.lucid.sda.pig.EpochToCalendar(timestamp, 'YEAR', 'MONTH', 'DATE') as timetuple,
        timestamp,
        id,
        url,
        position;
};

define has_hits(query_logs_in) returns query_logs_out {
    $query_logs_out = filter $query_logs_in by (hits is not null) and (hits > 0);
};


define to_hbase(relation, table, metric) returns void {
    prep = foreach $relation {
        timestamp = com.lucid.sda.pig.CalendarToEpoch(timetuple, 'YEAR', 'MONTH', 'DATE');
        rowkey = com.lucid.sda.pig.ToMetricRowKey(collection, '$metric', timestamp);
        generate
            rowkey, -- row
            flatten(timetuple), -- date dims (year, month, date)
            value; -- the metric value
    }

    ok = store prep into 'hbase://$table'
      using org.apache.pig.backend.hadoop.hbase.HBaseStorage(
        'date_dims:year date_dims:month date_dims:date metrics:$metric',
        '-caster com.lucid.sda.pig.HBaseMetricConverter'
  );
};

define to_hbase_old(relation, table) returns void {
    prep = foreach $relation {
        timestamp = com.lucid.sda.pig.CalendarToEpoch(timetuple, 'YEAR', 'MONTH', 'DATE');
        rowkey = com.lucid.sda.pig.ToMetricRowKey(collection, timestamp);
        generate
            rowkey, -- row
            flatten(timetuple), -- date dims (year, month, date)
            columnMap; -- metrics
    }

    ok = store prep into 'hbase://$table'
      using org.apache.pig.backend.hadoop.hbase.HBaseStorage(
        'date_dims:year date_dims:month date_dims:date metrics:*',
        '-caster com.lucid.sda.pig.HBaseMetricConverter'
  );
};
