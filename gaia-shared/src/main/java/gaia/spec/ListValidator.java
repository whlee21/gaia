package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.List;

public class ListValidator extends Validator {
	Class<?> type;

	public ListValidator(Class<?> type) {
		this.type = type;
	}

	public List<Error> validate(SpecProperty specProp, Object value) {
		List<Error> errors = new ArrayList<Error>();
		try {
			List list = (List) value;
			if (list == null) {
				return errors;
			}
			for (Object listEntry : list) {

				if (!type.isAssignableFrom(listEntry.getClass()))
					errors.add(new Error(specProp.name, Error.E_INVALID_VALUE, "Invalid type in array (" + value + ") not :"
							+ type.getName() + " was:" + listEntry.getClass().getName()));
			}
		} catch (Throwable t) {
			errors.add(new Error(specProp.name, Error.E_INVALID_TYPE, "Invalid type, not array of " + type.getName()
					+ " was: " + value.getClass().getName()));
			return errors;
		}

		return errors;
	}

	public Object cast(SpecProperty specProp, Object value) {
		return value;
	}
}
