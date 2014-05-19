package gaia.bigdata.pig;

import java.io.IOException;
import java.util.Map;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

public class AddToMap extends EvalFunc<Map> {
	public Map exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() < 3) || (tuple.size() % 2 == 0))
			throw new IOException("Incorrect number of values.");
		try {
			Map map = (Map) tuple.get(0);

			for (int i = 1; i < tuple.size(); i += 2) {
				String key = (String) tuple.get(i);
				Object value = tuple.get(i + 1);
				map.put(key, value);
			}
			return map;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
