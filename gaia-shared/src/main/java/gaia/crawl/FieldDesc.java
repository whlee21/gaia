package gaia.crawl;

class FieldDesc {
	private String fieldName;
	private float boost;
	private boolean multiValued;
	private boolean dateField;

	public FieldDesc(String fName, float boost) {
		this(fName, boost, false, false);
	}

	public FieldDesc(String fieldName, float boost, boolean multiValued, boolean dateField) {
		this.fieldName = fieldName;
		this.boost = boost;
		this.multiValued = multiValued;
		this.dateField = dateField;
	}

	public FieldDesc(String fName) {
		this(fName, 1.0F);
	}

	public boolean isDateField() {
		return dateField;
	}

	public float getBoost() {
		return boost;
	}

	public String getFieldName() {
		return fieldName;
	}

	public boolean isMultiValued() {
		return multiValued;
	}
}
