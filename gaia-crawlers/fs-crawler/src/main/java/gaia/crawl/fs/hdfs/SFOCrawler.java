package gaia.crawl.fs.hdfs;

import gaia.crawl.fs.FSObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFOCrawler implements Iterable<FSObject> {
	static final Logger LOG = LoggerFactory.getLogger(SFOCrawler.class);
	List<FileStatus> paths;
	Path path;
	Configuration conf;
	Converter conv;
	FileSystem fs;

	public SFOCrawler(FileSystem fs, Path path, Configuration conf, Converter conv) throws IOException {
		this.conf = conf;
		this.path = path;
		this.conv = conv;
		this.fs = fs;
		boolean single = false;
		FileStatus[] fstats = fs.listStatus(path);
		if (fstats == null) {
			single = true;
			fstats = new FileStatus[] { fs.getFileStatus(path) };
		}
		paths = new LinkedList<FileStatus>();
		Boolean seqFile = null;
		for (FileStatus s : fstats) {
			Path p = s.getPath();

			if ((single) || (p.getName().startsWith("part-"))) {
				if (fs.isFile(p)) {
					if (seqFile == null)
						seqFile = Boolean.TRUE;
					else if (seqFile != Boolean.TRUE) {
						throw new IOException("Inconsistent part-XXXXX files");
					}
					paths.add(s);
				} else {
					Path data = new Path(p, "data");
					if (!fs.exists(data)) {
						LOG.warn(new StringBuilder().append("Skipping invalid MapFile partition ").append(p).toString());
					} else {
						if (seqFile == null)
							seqFile = Boolean.FALSE;
						else if (seqFile != Boolean.FALSE) {
							throw new IOException("Inconsistent part-XXXXX files");
						}
						FileStatus fst = fs.getFileStatus(data);
						paths.add(fst);
					}
				}
			}
		}
		LOG.info(new StringBuilder().append("Sub-crawling ").append(seqFile.booleanValue() ? "SequenceFile" : "MapFile")
				.append(" ").append(single ? "" : "output format ").append("in ").append(path).toString());
	}

	public Iterator<FSObject> iterator() {
		try {
			return new SFOIterator(fs, paths, conf, conv);
		} catch (IOException e) {
			LOG.warn(new StringBuilder().append("Error sub-crawling ").append(path).append(": ").append(e).toString());
		}
		return null;
	}
}
