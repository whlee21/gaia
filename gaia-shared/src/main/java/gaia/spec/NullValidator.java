package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NullValidator extends Validator {
	public List<Error> validate(SpecProperty specProp, Object value) {
		if ((value == null) || (!specProp.required)) {
			return Collections.emptyList();
		}
		List<Error> res = new ArrayList<Error>();
		res.add(new Error(specProp.name, Error.E_FORBIDDEN_KEY, "cannot set this property"));
		return res;
	}

	public Object cast(SpecProperty specProp, Object value) {
		return null;
	}
}
