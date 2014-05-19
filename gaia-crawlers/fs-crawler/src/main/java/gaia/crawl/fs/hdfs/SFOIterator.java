package gaia.crawl.fs.hdfs;

import gaia.crawl.fs.FSObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileRecordReader;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;

class SFOIterator implements Iterator<FSObject> {
	Configuration conf;
	FileSystem fs;
	RecordReader rr;
	List<FileStatus> paths;
	Converter conv;
	int ridx;
	boolean hasNextCalled = false;
	boolean hasNext = false;
	Object curKey;
	Object curVal;
	FSObject curFSObject;

	SFOIterator(FileSystem fs, List<FileStatus> paths, Configuration conf, Converter conv) throws IOException {
		this.fs = fs;
		this.conf = conf;
		this.paths = paths;
		this.conv = conv;
		ridx = -1;
		initNextReader();
	}

	private void initNextReader() {
		hasNextCalled = false;
		if (rr != null) {
			try {
				rr.close();
			} catch (IOException e) {
				SFOCrawler.LOG.warn("Error closing reader " + paths.get(ridx) + ", ignored: " + e);
			}
			rr = null;
		}
		ridx += 1;
		if (ridx >= paths.size()) {
			return;
		}
		FileStatus fst = (FileStatus) paths.get(ridx);
		InputSplit split = new FileSplit(fst.getPath(), 0L, fst.getLen(), null);
		TaskAttemptContext ctx = new TaskAttemptContextImpl(conf, new TaskAttemptID());
		try {
			FSDataInputStream is = fs.open(fst.getPath());
			boolean plain;
			switch (is.readShort()) {
			case 8075:
				plain = true;
				break;
			case 21317:
				if (is.readByte() == 81)
					plain = false;
				else {
					plain = true;
				}
				break;
			default:
				plain = true;
			}
			is.close();
			if (plain)
				rr = new LineRecordReader();
			else {
				rr = new SequenceFileRecordReader();
			}
			rr.initialize(split, ctx);
		} catch (Exception e) {
			rr = null;
		}
	}

	public boolean hasNext() {
		if (hasNextCalled) {
			return hasNext;
		}
		if (rr == null) {
			return false;
		}
		hasNextCalled = true;
		do {
			if (ridx >= paths.size()) {
				hasNext = false;
				return hasNext;
			}
			try {
				hasNext = rr.nextKeyValue();
				if (hasNext) {
					curKey = rr.getCurrentKey();
					curVal = rr.getCurrentValue();
				}
			} catch (Exception e) {
				SFOCrawler.LOG.warn("Error reading from " + paths.get(ridx) + ", skipping: " + e.getMessage());
				hasNext = false;
			}

			if (hasNext) {
				try {
					curFSObject = conv.convert((FileStatus) paths.get(ridx), curKey, curVal);
				} catch (Exception e) {
					SFOCrawler.LOG.warn(
							"Can't convert " + paths.get(ridx) + "!" + curKey.toString() + ", skipping: " + e.toString(), e);

					continue;
				}
			}
			if (!hasNext)
				initNextReader();
		} while (!hasNext);
		return hasNext;
	}

	public FSObject next() {
		hasNextCalled = false;
		return curFSObject;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
