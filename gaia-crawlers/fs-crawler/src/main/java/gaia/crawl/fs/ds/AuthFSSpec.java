package gaia.crawl.fs.ds;

import gaia.spec.SpecProperty;
import gaia.spec.Validator;

public abstract class AuthFSSpec extends FSSpec {
	public AuthFSSpec() {
		addSpecProperty(new SpecProperty.Separator("authentication"));
		addSpecProperty(new SpecProperty("username", "datasource.username", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true));

		addSpecProperty(new SpecProperty("password", "datasource.password", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true, SpecProperty.HINTS_PASSWORD));
	}
}
