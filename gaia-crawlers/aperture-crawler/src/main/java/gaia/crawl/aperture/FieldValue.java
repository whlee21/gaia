package gaia.crawl.aperture;

public class FieldValue {
	private String prefix;
	private String suffix;
	private Object value;
	private float boost = 1.0F;

	public FieldValue(String prefix, String suffix, Object value, float theBoost) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.value = value;
		this.boost = theBoost;
	}

	public float getBoost() {
		return boost;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public Object getValue() {
		return value;
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(boost);
		result = prime * result + (prefix == null ? 0 : prefix.hashCode());
		result = prime * result + (suffix == null ? 0 : suffix.hashCode());
		result = prime * result + (value == null ? 0 : value.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldValue other = (FieldValue) obj;
		if (Float.floatToIntBits(boost) != Float.floatToIntBits(other.boost))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		if (suffix == null) {
			if (other.suffix != null)
				return false;
		} else if (!suffix.equals(other.suffix))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
