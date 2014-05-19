package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.List;

public class NonNegLongStringValidator extends LongStringValidator {
	protected List<Error> checkLong(long parseLong, SpecProperty confProp) {
		if (parseLong < 0L) {
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "Positive number required"));
			return res;
		}

		return null;
	}
}
