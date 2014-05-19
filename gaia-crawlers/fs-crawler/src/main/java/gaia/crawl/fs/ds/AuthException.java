package gaia.crawl.fs.ds;

public class AuthException extends RuntimeException {
	private static final long serialVersionUID = 7693110185503282430L;

	public AuthException(String name) {
		super(name);
	}
}
