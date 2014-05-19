package gaia.search.ui;

import java.io.IOException;

@SuppressWarnings("serial")
public class GaiaSearchUIServerException extends IOException {

	public GaiaSearchUIServerException(String message) {
		super(message);
	}

	public GaiaSearchUIServerException(String message, Throwable cause) {
		super(message, cause);
	}

}
