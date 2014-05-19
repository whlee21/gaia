package gaia.crawl.fs.hdfs;

import gaia.crawl.fs.FSObject;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.FileStatus;

class SFObject extends FSObject {
	SFObject(FileStatus p, Object k) {
		directory = false;
		uri = (p.getPath().toUri() + "!" + k.toString());
		name = k.toString();

		acls = EMPTY_ACLS;
		group = p.getGroup();
		owner = p.getOwner();
		lastModified = p.getModificationTime();
	}

	public Iterable<FSObject> getChildren() throws IOException {
		return null;
	}

	public InputStream open() throws IOException {
		return null;
	}
}
