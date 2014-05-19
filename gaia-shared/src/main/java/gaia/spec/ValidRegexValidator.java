package gaia.spec;

import gaia.api.Error;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ValidRegexValidator extends Validator {
	public List<Error> validate(SpecProperty specProp, Object value) {
		if (value == null) {
			if (!specProp.required) {
				return Collections.emptyList();
			}
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(specProp.name, Error.E_NULL_VALUE));
			return res;
		}
		List<String> regexes = StringUtils.getList(String.class, value);
		try {
			for (String regex : regexes)
				Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			List<Error> res = new ArrayList<Error>();
			res.add(new Error(specProp.name, Error.E_INVALID_VALUE, "not valid regex:" + e.getMessage()));
			return res;
		}

		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
