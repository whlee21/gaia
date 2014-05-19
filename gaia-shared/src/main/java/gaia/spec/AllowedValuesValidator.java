package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AllowedValuesValidator extends Validator {
	Collection<Object> allowedValues;

	public AllowedValuesValidator(Collection<Object> allowedValues) {
		this.allowedValues = allowedValues;
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
		if (allowedValues.contains(value)) {
			return Collections.emptyList();
		}
		List<Error> res = new ArrayList<Error>(1);
		res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "is not one of the allowed values "
				+ allowedValues.toString()));
		return res;
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
