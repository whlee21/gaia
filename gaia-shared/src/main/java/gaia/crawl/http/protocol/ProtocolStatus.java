package gaia.crawl.http.protocol;

public class ProtocolStatus {
	public static final ProtocolStatus OK = new ProtocolStatus(Code.OK, null, 200);
	public static final ProtocolStatus EXCEPTION = new ProtocolStatus(Code.EXCEPTION, null, 0);
	public Code code;
	public String message;
	public int httpCode;

	public ProtocolStatus(Code code, String message, int httpCode) {
		this.code = code;
		this.message = message;
		this.httpCode = httpCode;
	}

	public String toString() {
		return code + "/" + httpCode + ",msg=" + message;
	}

	public static enum Code {
		OK, NOT_FOUND, GONE, REDIRECT_PERM, REDIRECT_TEMP, REDIRECT_PROXY, ACCESS_DENIED, ROBOTS_DENIED, SERVER_ERROR, NOT_MODIFIED, DEFERRED, EXCEPTION;
	}
}
