package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotNullValidator extends Validator {
	public List<Error> validate(SpecProperty confProp, Object value) {
		if (value == null) {
			if (!confProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(confProp.name, Error.E_NULL_VALUE));
			return res;
		}
		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
