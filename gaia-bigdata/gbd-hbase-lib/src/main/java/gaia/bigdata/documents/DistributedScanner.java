package gaia.bigdata.documents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;

public class DistributedScanner implements ResultScanner {
	private final ResultScanner[] scanners;
	private final List<Result>[] nextOfScanners;
	private Result next = null;

	public DistributedScanner(HTableInterface hTable, Scan originalScan) throws IOException {
		Scan[] scans = getDistributedScans(originalScan);

		ResultScanner[] rss = new ResultScanner[scans.length];
		for (int i = 0; i < scans.length; i++) {
			rss[i] = hTable.getScanner(scans[i]);
		}

		scanners = rss;
		nextOfScanners = new List[scanners.length];
		for (int i = 0; i < nextOfScanners.length; i++)
			nextOfScanners[i] = new ArrayList<Result>();
	}

	private boolean hasNext(int nbRows) throws IOException {
		if (next != null) {
			return true;
		}

		next = nextInternal(nbRows);

		return next != null;
	}

	public Result next() throws IOException {
		if (hasNext(1)) {
			Result toReturn = next;
			next = null;
			return toReturn;
		}

		return null;
	}

	public Result[] next(int nbRows) throws IOException {
		ArrayList resultSets = new ArrayList(nbRows);
		for (int i = 0; i < nbRows; i++) {
			Result next = next();
			if (next == null)
				break;
			resultSets.add(next);
		}

		return (Result[]) resultSets.toArray(new Result[resultSets.size()]);
	}

	public void close() {
		for (int i = 0; i < scanners.length; i++)
			scanners[i].close();
	}

	public Pair<byte[], byte[]>[] getDistributedIntervals(byte[] originalStartKey, byte[] originalStopKey)
			throws IOException {
		byte[][] startKeys = DocumentKeySerializer.getAllDistributedKeys(originalStartKey);
		byte[][] stopKeys;
		if (Arrays.equals(originalStopKey, HConstants.EMPTY_END_ROW)) {
			Arrays.sort(startKeys, Bytes.BYTES_RAWCOMPARATOR);
			stopKeys = new byte[startKeys.length][];
			for (int i = 0; i < stopKeys.length - 1; i++) {
				stopKeys[i] = startKeys[(i + 1)];
			}
			stopKeys[(stopKeys.length - 1)] = HConstants.EMPTY_END_ROW;
		} else {
			stopKeys = DocumentKeySerializer.getAllDistributedKeys(originalStopKey);
			assert (stopKeys.length == startKeys.length);
		}

		Pair[] intervals = new Pair[startKeys.length];
		for (int i = 0; i < startKeys.length; i++) {
			intervals[i] = new Pair(startKeys[i], stopKeys[i]);
		}

		return intervals;
	}

	public final Scan[] getDistributedScans(Scan original) throws IOException {
		System.out.println("Origianl Scan: " + original);
		Pair[] intervals = getDistributedIntervals(original.getStartRow(), original.getStopRow());

		Scan[] scans = new Scan[intervals.length];
		for (int i = 0; i < intervals.length; i++) {
			scans[i] = new Scan(original);
			scans[i].setStartRow((byte[]) intervals[i].getFirst());
			scans[i].setStopRow((byte[]) intervals[i].getSecond());
			System.out.println("Distributed Scan: " + scans[i]);
		}
		return scans;
	}

	private Result nextInternal(int nbRows) throws IOException {
		Result result = null;
		int indexOfScannerToUse = -1;
		for (int i = 0; i < nextOfScanners.length; i++) {
			if (nextOfScanners[i] != null) {
				if (nextOfScanners[i].size() == 0) {
					Result[] results = scanners[i].next(nbRows);
					if (results.length == 0) {
						nextOfScanners[i] = null;
					} else {
						nextOfScanners[i].addAll(Arrays.asList(results));
					}

				} else if ((result == null)
						|| (Bytes.compareTo(DocumentKeySerializer.getOriginalKey(((Result) nextOfScanners[i].get(0)).getRow()),
								DocumentKeySerializer.getOriginalKey(result.getRow())) < 0)) {
					result = (Result) nextOfScanners[i].get(0);
					indexOfScannerToUse = i;
				}
			}
		}
		if (indexOfScannerToUse >= 0) {
			nextOfScanners[indexOfScannerToUse].remove(0);
		}

		return result;
	}

	public Iterator<Result> iterator() {
		return new Iterator<Result>() {
			Result next = null;

			public boolean hasNext() {
				if (next == null) {
					try {
						next = DistributedScanner.this.next();
						return this.next != null;
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				return true;
			}

			public Result next() {
				if (!hasNext()) {
					return null;
				}

				Result temp = this.next;
				this.next = null;
				return temp;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
