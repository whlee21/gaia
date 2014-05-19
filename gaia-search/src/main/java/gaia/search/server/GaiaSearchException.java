package gaia.search.server;

import java.io.IOException;

@SuppressWarnings("serial")
public class GaiaSearchException extends IOException {

	public GaiaSearchException(String message) {
		super(message);
	}

	public GaiaSearchException(String message, Throwable cause) {
		super(message, cause);
	}

}
