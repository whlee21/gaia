package gaia.crawl;

public class CrawlEvent {
	protected Object source;
	protected String type;
	protected String status;
	protected String msg;

	public CrawlEvent(String type, String status, Object source) {
		this(type, status, null, source);
	}

	public CrawlEvent(String type, String status, String msg, Object source) {
		this.type = type;
		this.status = status;
		this.msg = msg;
		this.source = source;
	}

	public String getType() {
		return type;
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return msg;
	}

	public Object getSource() {
		return source;
	}

	public String toString() {
		return "type=" + type + ",status=" + status + ",msg=" + msg + ",source=" + source;
	}

	public static enum Status {
		OK, FAIL, EXCEPTION;
	}

	public static enum Type {
		ADD, UPDATE, REMOVE, REMOVE_MULTI;
	}
}
