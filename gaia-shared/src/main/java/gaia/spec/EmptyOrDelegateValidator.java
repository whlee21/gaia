package gaia.spec;

import gaia.api.Error;

import java.util.Collections;
import java.util.List;

public class EmptyOrDelegateValidator extends Validator {
	private final Validator delegate;
	private final boolean convertEmptyToNull;

	public EmptyOrDelegateValidator(Validator delegate, boolean convertEmptyToNull) {
		this.delegate = delegate;
		this.convertEmptyToNull = convertEmptyToNull;
	}

	public List<Error> validate(SpecProperty specProp, Object value) {
		if ((value != null) && (!value.toString().isEmpty())) {
			return delegate.validate(specProp, value);
		}
		return Collections.emptyList();
	}

	public Object cast(SpecProperty specProp, Object value) {
		if (value == null)
			return null;
		if ((convertEmptyToNull) && (value.toString().isEmpty()))
			return null;
		return delegate.cast(specProp, value);
	}
}
