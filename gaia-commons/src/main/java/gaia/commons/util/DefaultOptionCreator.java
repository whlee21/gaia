package gaia.commons.util;

import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;

public final class DefaultOptionCreator {
	public static final String INPUT_OPTION = "input";
	public static final String OUTPUT_OPTION = "output";

	public static Option helpOption() {
		return new DefaultOptionBuilder().withLongName("help").withDescription("Print out help").withShortName("h")
				.create();
	}

	public static DefaultOptionBuilder inputOption() {
		return new DefaultOptionBuilder().withLongName("input").withRequired(false).withShortName("i")
				.withArgument(new ArgumentBuilder().withName("input").withMinimum(1).withMaximum(1).create())
				.withDescription("Path to job input directory.");
	}

	public static DefaultOptionBuilder outputOption() {
		return new DefaultOptionBuilder().withLongName("output").withRequired(false).withShortName("o")
				.withArgument(new ArgumentBuilder().withName("output").withMinimum(1).withMaximum(1).create())
				.withDescription("The directory pathname for output.");
	}
}
