package gaia.bigdata.pig;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

public class CalendarToEpoch extends EvalFunc<Long> {
	private static final Map<String, Integer> calMap = new HashMap<String, Integer>();
	private static final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

	public CalendarToEpoch() {
		calMap.put("YEAR", Integer.valueOf(1));
		calMap.put("MONTH", Integer.valueOf(2));
		calMap.put("DATE", Integer.valueOf(5));
		calMap.put("HOUR", Integer.valueOf(11));
		calMap.put("MINUTE", Integer.valueOf(12));
		calMap.put("SECOND", Integer.valueOf(13));
		calMap.put("MILLISECOND", Integer.valueOf(14));

		cal.set(1, 0);
		cal.set(2, 0);
		cal.set(5, 1);
		cal.set(11, 0);
		cal.set(12, 0);
		cal.set(13, 0);
		cal.set(14, 0);
	}

	private static Calendar getZeroedCalendar() {
		return (Calendar) cal.clone();
	}

	public Long exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() < 2)) {
			throw new IOException("Not enough values");
		}
		Tuple timeTuple = (Tuple) tuple.get(0);
		if (tuple.size() - 1 > timeTuple.size()) {
			throw new IOException("Not enough tuple values to convert. Specified " + (tuple.size() - 1)
					+ " calendar fields, but time tuple only has " + timeTuple.size());
		}
		Calendar cal = getZeroedCalendar();
		for (int i = 0; i < tuple.size() - 1; i++) {
			String key = (String) tuple.get(i + 1);
			Integer calKey = (Integer) calMap.get(key);
			if (calKey == null) {
				throw new IOException("Unknown Calendar field: " + key);
			}
			cal.set(calKey.intValue(), ((Integer) timeTuple.get(i)).intValue());
		}
		return Long.valueOf(cal.getTimeInMillis());
	}
}
