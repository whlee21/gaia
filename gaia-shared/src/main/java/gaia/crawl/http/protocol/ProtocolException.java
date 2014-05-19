package gaia.crawl.http.protocol;

public class ProtocolException extends Exception {
	private static final long serialVersionUID = 1109897799684387744L;
	private int code;
	private String url;

	public ProtocolException(String url, int code) {
		super(url);
		this.url = url;
		this.code = code;
	}

	public ProtocolException(String message, String url, int code) {
		super(message);
		this.url = url;
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public String getUrl() {
		return url;
	}
}
