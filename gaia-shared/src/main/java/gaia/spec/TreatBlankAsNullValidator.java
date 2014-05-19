package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class TreatBlankAsNullValidator extends Validator {
	public List<Error> validate(SpecProperty confProp, Object value) {
		if (((value instanceof String)) && (StringUtils.isBlank((String) value))) {
			value = null;
		}
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
		if (((value instanceof String)) && (StringUtils.isBlank((String) value)))
			return null;
		return value.toString();
	}
}
