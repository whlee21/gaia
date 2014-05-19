package gaia.spec;

import gaia.api.Error;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpecProperty {
	public static final Separator SEPARATOR = new Separator(null);

	public static final String[] HINTS_DEFAULT = { Hint.summary.toString() };

	public static final String[] HINTS_PASSWORD = { Hint.secret.toString() };

	public static final String[] HINTS_ADVANCED = { Hint.advanced.toString() };
	public String name;
	public String description;
	public Class<?> type;
	public Object defaultValue;
	public List<Object> allowedValues;
	public Validator validator;
	public boolean required;
	public boolean readOnly;
	public String[] hints = HINTS_DEFAULT;

	private SpecProperty() {
	}

	public SpecProperty(String name, String description, Class<?> type, Object defaultValue, Validator validator,
			boolean required) {
		this(name, description, type, defaultValue, validator, required, false, null, null);
	}

	public SpecProperty(String name, String description, Class<?> type, Object defaultValue, Validator validator,
			boolean required, String[] uiHints) {
		this(name, description, type, defaultValue, validator, required, false, uiHints);
	}

	public SpecProperty(String name, String description, Class<?> type, Object defaultValue, Validator validator,
			boolean required, boolean readOnly, String[] uiHints) {
		this(name, description, type, defaultValue, validator, required, readOnly, uiHints, null);
	}

	public SpecProperty(String name, String description, Class<?> type, Object defaultValue, Validator validator,
			boolean required, boolean readOnly, String[] uiHints, List<Object> allowedValues) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.defaultValue = defaultValue;
		if (validator == null) {
			throw new RuntimeException("Validator must not be null. You may use NOOPValidator for no validaton.");
		}
		this.validator = validator;
		this.required = required;
		this.readOnly = readOnly;
		if (uiHints != null) {
			hints = uiHints;
		}
		this.allowedValues = allowedValues;
	}

	public List<Error> validate(Object value) {
		if (validator != null) {
			return validator.validate(this, value);
		}
		return Collections.emptyList();
	}

	public String toString() {
		return "SpecProperty [name=" + name + ", description=" + description + ", type=" + type + ", defaultValue="
				+ defaultValue + ", allowedValues=" + allowedValues + ", validator=" + validator + ", required=" + required
				+ ", readOnly=" + readOnly + ", hints=" + Arrays.toString(hints) + "]";
	}

	public static final class Separator extends SpecProperty {
		private Separator() {
			super();
		}

		public Separator(String description) {
			super();
			hints = new String[] { SpecProperty.Hint.hidden.toString() };
			name = "---";
			this.description = description;
		}
	}

	public static enum Hint {
		summary,

		secret,

		lengthy,

		advanced,

		hidden;
	}
}
