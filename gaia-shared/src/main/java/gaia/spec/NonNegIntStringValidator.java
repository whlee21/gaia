package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.List;

public class NonNegIntStringValidator extends IntStringValidator {
	protected List<Error> checkInt(int parseInt, SpecProperty confProp) {
		if (parseInt < 0) {
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "Positive number required"));
			return res;
		}
		return null;
	}
}
