package gaia.crawl;

public class CrawlException extends Exception {
	private static final long serialVersionUID = 2755541116921866494L;

	public CrawlException(String message) {
		super(message);
	}

	public CrawlException(Throwable cause) {
		super(cause);
	}

	public CrawlException(String message, Throwable cause) {
		super(message, cause);
	}
}
