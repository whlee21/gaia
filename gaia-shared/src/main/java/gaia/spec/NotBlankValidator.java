package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotBlankValidator extends Validator {
	public List<Error> validate(SpecProperty confProp, Object value) {
		if (value == null) {
			if (!confProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(confProp.name, Error.E_NULL_VALUE));
			return res;
		}
		if (value.toString().trim().length() == 0) {
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(confProp.name, Error.E_EMPTY_VALUE, "empty value"));
			return res;
		}
		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value.toString();
	}
}
