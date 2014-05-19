package gaia.spec;

import gaia.api.Error;
import gaia.crawl.datasource.Authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationValidator extends Validator {
	private static final Logger LOG = LoggerFactory.getLogger(AuthenticationValidator.class);

	public List<Error> validate(SpecProperty specProp, Object value) {
		List<Error> errors = new ArrayList<Error>();
		if (value == null) {
			if (specProp.required) {
				errors.add(new Error("auth", Error.E_MISSING_VALUE));
			}
			return errors;
		}
		if ((value instanceof Map))
			value = Collections.singletonList(value);
		if ((value instanceof List)) {
			if (((List) value).size() > 0) {
				List list = (List) value;
				for (Iterator it = list.iterator(); it.hasNext();) {
					Object el = it.next();
					if (!(el instanceof Authentication)) {
						if (!(el instanceof Map)) {
							errors.add(new Error("auth", Error.E_INVALID_TYPE, "expected Map, got " + el.getClass().getName()));
						} else {
							Map m = (Map) el;

							if (m.get("username") == null) {
								errors.add(new Error("auth.username", Error.E_MISSING_VALUE));
							}

							if (m.get("password") == null)
								errors.add(new Error("auth.password", Error.E_MISSING_VALUE));
						}
					}
				}
			}
		} else {
			errors.add(new Error("auth", Error.E_INVALID_TYPE, "expected a list of maps, got " + value.getClass().getName()));
		}

		return errors;
	}

	public Object cast(SpecProperty specProp, Object value) {
		if (value == null) {
			return null;
		}
		List res = new ArrayList();
		if ((value instanceof Map)) {
			Authentication auth = convert((Map) value);
			if (auth != null)
				res.add(auth);
		} else if ((value instanceof Authentication)) {
			res.add((Authentication) value);
		} else {
			if ((value instanceof List)) {
				List list = (List) value;
				for (Object o : list) {
					if (o != null) {
						if ((o instanceof Authentication)) {
							res.add((Authentication) o);
						} else if ((o instanceof Map)) {
							Authentication auth = convert((Map) o);
							if (auth != null)
								res.add(auth);
						} else {
							LOG.warn("Invalid type, expected Authentication or equivalent Map, was " + o.getClass().getName());
						}
					}
				}
			} else {
				LOG.warn("Invalid type, expected List<Authentication> or equivalent List<Map>, was "
						+ value.getClass().getName());
			}
		}
		return res;
	}

	private Authentication convert(Map<?, ?> m) {
		try {
			Authentication res = new Authentication();
			res.setHost((String) m.get("host"));
			res.setRealm((String) m.get("realm"));
			res.setUsername((String) m.get("username"));
			res.setPassword((String) m.get("password"));
			return res;
		} catch (Exception e) {
			LOG.warn("Can't convert map to Authentication", e);
		}
		return null;
	}
}
