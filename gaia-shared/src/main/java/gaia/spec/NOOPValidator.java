package gaia.spec;

import gaia.api.Error;

import java.util.Collections;
import java.util.List;

public class NOOPValidator extends Validator {
	public List<Error> validate(SpecProperty specProp, Object value) {
		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
