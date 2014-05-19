package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntStringValidator extends Validator {
	public List<Error> validate(SpecProperty confProp, Object value) {
		if ((value == null) || (value.toString().trim().length() == 0)) {
			if (!confProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_EMPTY_VALUE));
			return res;
		}
		if ((value instanceof Number)) {
			List<Error> res = checkInt(((Number) value).intValue(), confProp);
			if ((res != null) && (res.size() > 0)) {
				return res;
			}
			return Collections.emptyList();
		}
		if ((value instanceof String)) {
			if (((String) value).trim().length() == 0) {
				List<Error> res = new ArrayList<Error>();
				res.add(new Error(confProp.name, Error.E_EMPTY_VALUE, "empty string"));
				return res;
			}
			try {
				List<Error> res = checkInt(Integer.parseInt((String) value), confProp);
				if ((res != null) && (res.size() > 0))
					return res;
			} catch (NumberFormatException nfe) {
				List<Error> res = new ArrayList<Error>();
				res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "not a long int number"));
				return res;
			}
			return Collections.emptyList();
		}
		List<Error> res = new ArrayList<Error>();
		res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "not a number"));
		return res;
	}

	protected List<Error> checkInt(int parseInt, SpecProperty confProp) {
		return null;
	}

	public Object cast(SpecProperty specProp, Object value) {
		if ((value instanceof Number)) {
			return Integer.valueOf(((Number) value).intValue());
		}
		return Integer.valueOf(Integer.parseInt(value.toString()));
	}
}
