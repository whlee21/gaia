package gaia.solr.click;

import java.io.File;
import java.io.FilenameFilter;

public class BoostDataFileFilter implements FilenameFilter {
	public static final BoostDataFileFilter INSTANCE = new BoostDataFileFilter();

	public boolean accept(File file, String name) {
		if (((name.startsWith("boost")) || (name.startsWith("part-r-"))) && (!name.endsWith(".crc"))) {
			return true;
		}
		return false;
	}
}
