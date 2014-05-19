package gaia.commons.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLI {
	private static transient Logger LOG = LoggerFactory.getLogger(CLI.class);
	private Option inputOption;
	private Option outputOption;
	protected Map<String, List<String>> argMap;
	private final List<Option> options;
	private Group group;

	public CLI() {
		options = new LinkedList<Option>();
	}

	public void addFlag(String name, String shortName, String description) {
		options.add(buildOption(name, shortName, description, false, false, null));
	}

	public void addOption(String name, String shortName, String description) {
		options.add(buildOption(name, shortName, description, true, false, null));
	}

	public void addOption(String name, String shortName, String description, boolean required) {
		options.add(buildOption(name, shortName, description, true, required, null));
	}

	public void addOption(String name, String shortName, String description, String defaultValue) {
		options.add(buildOption(name, shortName, description, true, false, defaultValue));
	}

	public Option addOption(Option option) {
		options.add(option);
		return option;
	}

	public Group getGroup() {
		return group;
	}

	public void addInputOption() {
		inputOption = addOption(DefaultOptionCreator.inputOption().create());
	}

	public void addOutputOption() {
		outputOption = addOption(DefaultOptionCreator.outputOption().create());
	}

	public static Option buildOption(String name, String shortName, String description, boolean hasArg, boolean required,
			String defaultValue) {
		return buildOption(name, shortName, description, hasArg, 1, 1, required, defaultValue);
	}

	public static Option buildOption(String name, String shortName, String description, boolean hasArg, int min, int max,
			boolean required, String defaultValue) {
		DefaultOptionBuilder optBuilder = new DefaultOptionBuilder().withLongName(name).withDescription(description)
				.withRequired(required);

		if (shortName != null) {
			optBuilder.withShortName(shortName);
		}

		if (hasArg) {
			ArgumentBuilder argBuilder = new ArgumentBuilder().withName(name).withMinimum(min).withMaximum(max);

			if (defaultValue != null) {
				argBuilder = argBuilder.withDefault(defaultValue);
			}

			optBuilder.withArgument(argBuilder.create());
		}

		return optBuilder.create();
	}

	public Option getCLIOption(String name) {
		for (Option option : options) {
			if (option.getPreferredName().equals(name)) {
				return option;
			}
		}
		return null;
	}

	public Map<String, List<String>> parseArguments(String[] args) throws IOException {
		return parseArguments(args, false, false);
	}

	public Map<String, List<String>> parseArguments(String[] args, boolean inputOptional, boolean outputOptional)
			throws IOException {
		Option helpOpt = addOption(DefaultOptionCreator.helpOption());
		addOption("tempDir", null, "Intermediate output directory", "temp");
		addOption("startPhase", null, "First phase to run", "0");
		addOption("endPhase", null, "Last phase to run", String.valueOf(Integer.MAX_VALUE));

		GroupBuilder gBuilder = new GroupBuilder().withName("Job-Specific Options:");

		for (Option opt : options) {
			gBuilder = gBuilder.withOption(opt);
		}

		group = gBuilder.create();
		CommandLine cmdLine;
		try {
			Parser parser = new Parser();
			parser.setGroup(group);
			parser.setHelpOption(helpOpt);
			cmdLine = parser.parse(args);
		} catch (OptionException e) {
			LOG.error(e.getMessage());

			return null;
		}

		if (cmdLine.hasOption(helpOpt)) {
			return null;
		}

		argMap = new TreeMap<String, List<String>>();
		maybePut(argMap, cmdLine, (Option[]) options.toArray(new Option[options.size()]));

		if (!hasOption("quiet")) {
			LOG.info("Command line arguments: {}", argMap);
		}
		return argMap;
	}

	protected static void maybePut(Map<String, List<String>> args, CommandLine cmdLine, Option[] opt) {
		for (Option o : opt) {
			if ((cmdLine.hasOption(o)) || (cmdLine.getValue(o) != null)
					|| ((cmdLine.getValues(o) != null) && (!cmdLine.getValues(o).isEmpty()))) {
				List<Object> vo = cmdLine.getValues(o);
				if ((vo != null) && (!vo.isEmpty())) {
					List<String> vals = new ArrayList<String>();
					for (Object o1 : vo) {
						vals.add(o1.toString());
					}
					args.put(o.getPreferredName(), vals);
				} else {
					args.put(o.getPreferredName(), null);
				}
			}
		}
	}

	public static String keyFor(String optionName) {
		return "--" + optionName;
	}

	public String getOption(String optionName) {
		List<String> list = argMap.get(keyFor(optionName));
		if ((list != null) && (!list.isEmpty())) {
			return list.get(0);
		}
		return null;
	}

	public String getOption(String optionName, String defaultVal) {
		String res = getOption(optionName);
		if (res == null) {
			res = defaultVal;
		}
		return res;
	}

	public List<String> getOptions(String optionName) {
		return argMap.get(keyFor(optionName));
	}

	public boolean hasOption(String optionName) {
		return argMap.containsKey(keyFor(optionName));
	}

	public static String getOption(Map<String, List<String>> args, String optName) {
		List<String> res = args.get(optName);
		if ((res != null) && (!res.isEmpty())) {
			return (String) res.get(0);
		}
		return null;
	}
}
