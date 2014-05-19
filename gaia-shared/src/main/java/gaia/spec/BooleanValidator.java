package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BooleanValidator extends Validator {
	private static Set<String> valTrue = new HashSet<String>();
	private static Set<String> valFalse = new HashSet<String>();

	public List<Error> validate(SpecProperty confProp, Object value) {
		if ((value == null) || (value.toString().trim().length() == 0)) {
			if (!confProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_EMPTY_VALUE));
			return res;
		}
		if ((value instanceof Boolean)) {
			return Collections.emptyList();
		}
		String strValue = value.toString().toLowerCase();
		if ((valTrue.contains(strValue)) || (valFalse.contains(strValue))) {
			return Collections.emptyList();
		}
		List<Error> res = new ArrayList<Error>();
		res.add(new Error(confProp.name, Error.E_INVALID_VALUE));
		return res;
	}

	public Object cast(SpecProperty specProp, Object value) {
		return cast(value);
	}

	public static Boolean cast(Object value) {
		if ((value instanceof Boolean)) {
			return (Boolean) value;
		}
		String strValue = value.toString().toLowerCase();
		if (valTrue.contains(strValue)) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	static {
		valTrue.add("true");
		valTrue.add("yes");
		valTrue.add("on");
		valTrue.add("1");
		valFalse.add("false");
		valFalse.add("no");
		valFalse.add("off");
		valFalse.add("0");
	}
}
