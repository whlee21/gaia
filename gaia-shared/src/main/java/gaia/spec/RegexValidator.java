package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class RegexValidator extends Validator {
	Pattern regex;

	public RegexValidator(String regexString) {
		this.regex = Pattern.compile(regexString);
	}

	public List<Error> validate(SpecProperty confProp, Object value) {
		if (value == null) {
			if (!confProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_NULL_VALUE));
			return res;
		}
		String stringVal = value.toString();
		if (this.regex.matcher(stringVal).matches()) {
			return Collections.emptyList();
		}
		List<Error> res = new ArrayList<Error>(1);
		res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "does not match the pattern " + this.regex));
		return res;
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
