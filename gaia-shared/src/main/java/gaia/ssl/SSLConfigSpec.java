package gaia.ssl;

import gaia.api.Error;
import gaia.spec.ListValidator;
import gaia.spec.Spec;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SSLConfigSpec extends Spec {
	public SSLConfigSpec() {
		super("sslConfig");
		addSpecProperty(new SpecProperty("auth_authorized_clients", "Authorized clients", List.class, "",
				new ListValidator(String.class), false));

		addSpecProperty(new SpecProperty("auth_require_authorization", "Are clients required to be authorized",
				Boolean.class, "", Validator.BOOLEAN_VALIDATOR, false));
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);

		Set<String> fields = new HashSet<String>(map.keySet());
		for (SpecProperty p : getSpecProperties()) {
			fields.remove(p.name);
		}
		if (fields.size() > 0) {
			errors.add(new Error(fields.toString(), Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + fields));
		}

		return errors;
	}
}
