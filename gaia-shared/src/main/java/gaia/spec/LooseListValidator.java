package gaia.spec;

import gaia.api.Error;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class LooseListValidator extends Validator {
	Class<?> type;

	public LooseListValidator(Class<?> type) {
		this.type = type;
	}

	public List<Error> validate(SpecProperty specProp, Object value) {
		List<Error> errors = new ArrayList<Error>();
		try {
			if ((value == null) || (value.toString().trim().isEmpty()))
				return errors;
			List list;
			if ((value instanceof List)) {
				list = (List) value;
			} else {
				list = new ArrayList<Error>();

				if (((value instanceof String)) && (value.toString().split("\\r?\\n") != null)
						&& (value.toString().split("\\r?\\n").length > 1)) {
					String[] lines = value.toString().split("\\r?\\n");
					for (String line : lines)
						if (!line.trim().isEmpty())
							list.add(line.trim());
				} else {
					list.add(value);
				}

			}

			for (Object listEntry : list) {
				try {
					Constructor<?> ctor = type.getConstructor(new Class[] { String.class });
					Object object = ctor.newInstance(new Object[] { listEntry.toString() });
				} catch (Exception e) {
					errors.add(new Error(specProp.name, Error.E_INVALID_VALUE, "Invalid type in array (" + value + ") not :"
							+ type.getName() + " was:" + listEntry.getClass().getName()));
				}
			}
		} catch (Throwable t) {
			errors.add(new Error(specProp.name, Error.E_INVALID_TYPE, "Invalid type, not array of " + type.getName()
					+ " was: " + value.getClass().getName()));
			return errors;
		}

		return errors;
	}

	public Object cast(SpecProperty specProp, Object value) {
		if (value == null)
			return null;
		if (value.toString().trim().isEmpty())
			return null;
		List<Object> list;
		if ((value instanceof List)) {
			list = (List) value;
		} else {
			list = new ArrayList<Object>();

			if (((value instanceof String)) && (value.toString().split("\\r?\\n") != null)
					&& (value.toString().split("\\r?\\n").length > 1)) {
				String[] lines = value.toString().split("\\r?\\n");
				for (String line : lines)
					if (!line.trim().isEmpty())
						list.add(line.trim());
			} else {
				list.add(value);
			}
		}

		if (list.isEmpty()) {
			return null;
		}
		List<Object> result = new ArrayList<Object>();
		for (Object val : list) {
			if (val != null)
				try {
					Constructor<?> ctor = type.getConstructor(new Class[] { String.class });
					Object object = ctor.newInstance(new Object[] { val.toString() });
					result.add(object);
				} catch (Exception e) {
					return null;
				}
		}
		return result;
	}
}
