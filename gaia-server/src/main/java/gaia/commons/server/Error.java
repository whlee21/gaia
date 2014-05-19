package gaia.commons.server;

public class Error {
	public static final String E_INVALID_VALUE = "error.invalid.value";
	public static final String E_EMPTY_VALUE = "error.empty.value";
	public static final String E_MISSING_VALUE = "error.missing.value";
	public static final String E_NULL_VALUE = "error.null.value";
	public static final String E_INVALID_TYPE = "error.invalid.type";
	public static final String E_INVALID_OPERATION = "error.invalid.operation";
	public static final String E_FORBIDDEN_OP = "error.forbidden.operation";
	public static final String E_FORBIDDEN_VALUE = "error.forbidden.value";
	public static final String E_FORBIDDEN_KEY = "error.forbidden.key";
	public static final String E_EXCEPTION = "error.other.exception";
	public static final String E_EXISTS = "error.already.exists";
	public static final String E_NOT_FOUND = "error.not.found";
	public static final String E_LOW_DISK_SPACE = "error.low.disk.space";
	public static final String E_TIMED_OUT = "error.timed.out";
	private String key = "";
	private String message;
	private String code;

	public Error(String key, String code, String message) {
		this.key = key;
		this.message = message;
		this.code = code;
	}

	public Error(String key, String code) {
		this.key = key;
		this.code = code;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String toString() {
		return "key:" + key + " msg:" + message + " code:" + code;
	}
}
