package gaia.spec;

import gaia.api.Error;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NumberValidator extends Validator {
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
			Class<?> clazz = confProp.type;
			Constructor<?> ctor = clazz.getConstructor(new Class[] { String.class });
			Object object = ctor.newInstance(new Object[] { value.toString() });
		} catch (Exception e) {
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(confProp.name, Error.E_INVALID_TYPE, "not a number"));
			return res;
		}
		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		try {
			Class<?> clazz = specProp.type;
			Constructor<?> ctor = clazz.getConstructor(new Class[] { String.class });
			return ctor.newInstance(new Object[] { value.toString() });
		} catch (Exception e) {
		}
		return value;
	}
}
