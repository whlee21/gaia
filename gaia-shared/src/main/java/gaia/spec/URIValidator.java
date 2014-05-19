package gaia.spec;

import gaia.api.Error;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class URIValidator extends Validator {
	public List<Error> validate(SpecProperty confProp, Object value) {
		if ((value == null) || (value.toString().trim().length() == 0)) {
			if (!confProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_EMPTY_VALUE));
			return res;
		}
		try {
			new URI(value.toString());
		} catch (Exception e) {
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "invalid URI " + e.getMessage()));
			return res;
		}
		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
