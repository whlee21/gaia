package gaia.spec;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Spec {
	private List<SpecProperty> specProps;
	private Map<String, SpecProperty> nameToProp;
	protected String name;

	protected Spec(String name) {
		this.name = name;
		specProps = new ArrayList<SpecProperty>();
		nameToProp = new HashMap<String, SpecProperty>();
	}

	protected void reset() {
		specProps.clear();
		nameToProp.clear();
	}

	public String getName() {
		return name;
	}

	public void addSpecProperties(List<SpecProperty> confProps) {
		for (SpecProperty prop : confProps)
			addSpecProperty(prop);
	}

	public void addSpecProperty(SpecProperty prop) {
		specProps.add(prop);
		nameToProp.put(prop.name, prop);
	}

	public void removeSpecProperty(String name) {
		SpecProperty prop = (SpecProperty) nameToProp.get(name);
		if (prop != null)
			specProps.remove(prop);
	}

	public SpecProperty getSpecProperty(String name) {
		return (SpecProperty) nameToProp.get(name);
	}

	public List<SpecProperty> getSpecProperties() {
		return Collections.unmodifiableList(specProps);
	}

	public Map<String, SpecProperty> getNameToPropertyMap() {
		return Collections.unmodifiableMap(nameToProp);
	}

	public static Map<String, Object> toMap(Spec spec) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("name", spec.name);
		List<Map<String, Object>> props = new ArrayList<Map<String, Object>>(spec.specProps.size());
		res.put("props", props);
		for (SpecProperty p : spec.specProps) {
			Map<String, Object> mp = new HashMap<String, Object>();
			props.add(mp);
			if ((p instanceof SpecProperty.Separator)) {
				mp.put("name", "---");
				mp.put("description", p.description);
			} else {
				mp.put("name", p.name);
				mp.put("required", Boolean.valueOf(p.required));
				mp.put("read_only", Boolean.valueOf(p.readOnly));
				mp.put("hints", Arrays.asList(p.hints));
				mp.put("description", p.description);
				mp.put("default_value", p.defaultValue);
				mp.put("type", p.type != null ? p.type.getSimpleName().toLowerCase() : null);
				mp.put("allowed_values", p.allowedValues);
				mp.put("validator", p.validator != null ? p.validator.getClass().getName() : null);
			}
		}
		return res;
	}

	public static Spec fromMap(Map<String, Object> m, Spec template) {
		return template;
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> res = new ArrayList<Error>();
		if (map == null) {
			res.add(new Error("", Error.E_NULL_VALUE, "null input map"));
			return res;
		}
		if ((specProps == null) || (specProps.isEmpty())) {
			return Collections.emptyList();
		}
		for (SpecProperty prop : specProps)
			if (!prop.readOnly) {
				res.addAll(prop.validate(map.get(prop.name)));
			}
		return res;
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		for (SpecProperty prop : specProps) {
			Object value = input.get(prop.name);
			if (value != null) {
				if (prop.validator != null) {
					value = prop.validator.cast(prop, value);
					input.put(prop.name, value);
				}
			} else if ((!prop.required) && (!(prop instanceof SpecProperty.Separator))) {
				input.put(prop.name, prop.defaultValue);
			}
		}

		return input;
	}
}
