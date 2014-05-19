package gaia.bigdata.hbase.documents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;

public class WdTableInputFormat extends TableInputFormat {
	public List<InputSplit> getSplits(JobContext context) throws IOException {
		List<InputSplit> allSplits = new ArrayList<InputSplit>();
		Scan originalScan = getScan();
		DistributedScanner scanner = new DistributedScanner(getHTable(),
				originalScan);

		Scan[] scans = scanner.getDistributedScans(originalScan);

		for (Scan scan : scans) {
			setScan(scan);
			List<InputSplit> splits = super.getSplits(context);
			allSplits.addAll(splits);
		}

		setScan(originalScan);

		scanner.close();

		return allSplits;
	}
}
