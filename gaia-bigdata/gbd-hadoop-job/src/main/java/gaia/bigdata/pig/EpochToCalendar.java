package gaia.bigdata.pig;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class EpochToCalendar extends EvalFunc<Tuple> {
	private static final Map<String, Integer> calMap = new HashMap<String, Integer>();

	public EpochToCalendar() {
		calMap.put("YEAR", Integer.valueOf(1));
		calMap.put("MONTH", Integer.valueOf(2));
		calMap.put("DATE", Integer.valueOf(5));
		calMap.put("HOUR", Integer.valueOf(11));
		calMap.put("MINUTE", Integer.valueOf(12));
		calMap.put("SECOND", Integer.valueOf(13));
		calMap.put("MILLISECOND", Integer.valueOf(14));
		calMap.put("DAY_OF_WEEK", Integer.valueOf(7));
		calMap.put("DAY_OF_MONTH", Integer.valueOf(5));
		calMap.put("DAY_OF_YEAR", Integer.valueOf(6));
		calMap.put("WEEK_OF_MONTH", Integer.valueOf(4));
		calMap.put("WEEK_OF_YEAR", Integer.valueOf(3));
	}

	public Tuple exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() < 2)) {
			throw new IOException("Not enough values");
		}
		long epoch = ((Long) tuple.get(0)).longValue();
		Tuple out = TupleFactory.getInstance().newTuple(tuple.size() - 1);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(epoch);
		for (int i = 1; i < tuple.size(); i++) {
			String key = (String) tuple.get(i);
			Integer calKey = (Integer) calMap.get(key);
			if (calKey == null)
				out.set(i - 1, null);
			else {
				out.set(i - 1, Integer.valueOf(cal.get(calKey.intValue())));
			}
		}
		return out;
	}
}
