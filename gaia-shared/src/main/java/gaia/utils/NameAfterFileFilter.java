package gaia.utils;

import java.io.File;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.AbstractFileFilter;

public final class NameAfterFileFilter extends AbstractFileFilter {
	private final File base;

	public NameAfterFileFilter(File base) {
		this.base = base;
	}

	public boolean accept(File file) {
		return 0 < IOCase.INSENSITIVE.checkCompareTo(file.getName(), base.getName());
	}
}
