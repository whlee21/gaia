package gaia.bigdata.hadoop.util;

import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class RunClass extends Configured implements Tool {
	public static void main(String[] args) throws Exception {
		Configuration mahoutConfig = initActionConf();
		RunClass rc = new RunClass();
		ToolRunner.run(mahoutConfig, rc, args);
	}

	public int run(String[] args) throws Exception {
		System.out.println("<====== RunClass after ToolRunner.  mapred.job.tracker: " + getConf().get("mapred.job.tracker")
				+ "========>");
		Class clazz = Class.forName(args[0]);
		Tool tool = (Tool) clazz.newInstance();
		String[] mainArgs = new String[args.length - 1];
		System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);
		tool.setConf(getConf());
		int returnCode = tool.run(mainArgs);
		System.out.println("<=================================RETURN: " + returnCode);
		return 0;
	}

	private static Configuration initActionConf() {
		Configuration mahoutConfig = new Configuration(false);

		String actionXml = System.getProperty("oozie.action.conf.xml");
		if (actionXml == null) {
			throw new RuntimeException("Missing Java System Property [oozie.action.conf.xml]");
		}
		if (!new File(actionXml).exists()) {
			throw new RuntimeException("Action Configuration XML file [" + actionXml + "] does not exist");
		}
		System.out.println("Using action configuration file " + actionXml);

		mahoutConfig.addResource(new Path("file:///", actionXml));
		return mahoutConfig;
	}
}