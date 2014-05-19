package gaia.bigdata.pig;

import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class Extract extends EvalFunc<Tuple> {
	public Tuple exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() != 2)) {
			throw new IOException("nope");
		}
		DataBag bag = (DataBag) tuple.get(0);
		int fieldNum = ((Integer) tuple.get(1)).intValue();
		Tuple out = TupleFactory.getInstance().newTuple();
		for (Tuple in : bag) {
			out.append(in.get(fieldNum));
		}
		return out;
	}
}
