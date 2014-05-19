package gaia.spec;

import gaia.api.Error;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class URLValidator extends Validator {
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
			new URL(value.toString());
		} catch (Exception e) {
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "invalid URL " + e.getMessage()));
			return res;
		}

		try {
			new URI(value.toString());
		} catch (Exception e) {
			List<Error> res = new ArrayList<Error>(1);
			res.add(new Error(confProp.name, Error.E_INVALID_VALUE, "invalid URL " + e.getMessage()));
			return res;
		}

		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		String url = value.toString();
		int pos = url.indexOf("://");
		if (pos != -1) {
			String proto = url.substring(0, pos).toLowerCase();
			if (proto.startsWith("http")) {
				pos = url.indexOf('/', pos + 3);
				if (pos == -1) {
					url = url + "/";
					pos = url.length() - 1;
				}

				url = url.substring(0, pos).toLowerCase(Locale.ENGLISH) + url.substring(pos);
			}
		}
		return url;
	}
}
