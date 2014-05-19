package gaia.bigdata.oozie.logs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class OozieLogsPrep {
	public static void main(String[] args) throws Exception {
		DateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		DateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd/HH");
		String input = "/tmp/kafka/*/" + outFormat.format(inFormat.parse(args[0])) + "/logs-*";
		throw new RuntimeException("args: " + Arrays.asList(args));
	}
}
