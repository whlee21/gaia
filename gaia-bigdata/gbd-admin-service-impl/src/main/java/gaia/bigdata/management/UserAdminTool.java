package gaia.bigdata.management;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.commons.util.StringUtil;
import gaia.bigdata.api.SDARole;
import gaia.bigdata.api.user.User;
import gaia.bigdata.hbase.users.UserTable;

public class UserAdminTool {
	private static final Logger log = LoggerFactory.getLogger(UserAdminTool.class);

	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("h", "help", false, "Print this message");

		OptionBuilder.withLongOpt("zkConnect");
		OptionBuilder.withDescription("ZooKeeper connection string");
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("ZK-CONNECT");
		OptionBuilder.isRequired();
		options.addOption(OptionBuilder.create());

		OptionBuilder.withLongOpt("username");
		OptionBuilder.withDescription("User name");
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("USER");
		OptionBuilder.isRequired();
		options.addOption(OptionBuilder.create());

		OptionBuilder.withLongOpt("password");
		OptionBuilder.withDescription("Password");
		OptionBuilder.hasArg();
		OptionBuilder.withArgName("PASSWORD");
		OptionBuilder.isRequired();
		options.addOption(OptionBuilder.create());

		OptionBuilder.withLongOpt("admin");
		OptionBuilder.withDescription("Create as an Admin user");
		OptionBuilder.hasArg(false);
		options.addOption(OptionBuilder.create());

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		HelpFormatter formatter = new HelpFormatter();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
			formatter.printHelp("java " + UserAdminTool.class.getName(), options);
			System.exit(1);
		}

		if (cmd.hasOption("help")) {
			formatter.printHelp("java " + UserAdminTool.class.getName(), options);
			System.exit(0);
		}

		UserTable table = new UserTable(cmd.getOptionValue("zkConnect"));
		String username = cmd.getOptionValue("username");
		String password = StringUtil.md5Hash(cmd.getOptionValue("password"));
		User user = table.getUser(username);
		if (user == null) {
			user = new User(username);
		}
		user.password = password;
		user.getRoles().add(SDARole.DEFAULT.toString());
		if (cmd.hasOption("admin")) {
			log.info("Creating user {} as admin", username);
			user.getRoles().add(SDARole.ADMINISTRATOR.toString());
		} else {
			log.info("Creating user {} as non-admin", username);
		}
		table.putUser(user);
		log.info("Created user {}", username);
		table.close();
	}
}
