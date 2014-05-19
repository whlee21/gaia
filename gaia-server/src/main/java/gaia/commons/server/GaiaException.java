package gaia.commons.server;

import gaia.commons.server.api.services.ResultStatus;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public class GaiaException extends RuntimeException {

	private ResultStatus.STATUS status;
	private List<Error> errors;

	public GaiaException(Exception e) {
	}

	public GaiaException(ResultStatus.STATUS status, Error error) {
		this.errors = Collections.singletonList(error);
	}
	
	public GaiaException(ResultStatus.STATUS status, List<Error> errors) {
		this.status = status;
		this.errors = errors;
	}
	public GaiaException(ResultStatus.STATUS status, List<Error> errors, Throwable cause) {
		this.status = status;
		this.errors = errors;
	}
	
	public ResultStatus.STATUS getStatus() {
		return status;
	}

	public List<Error> getErrors() {
		return errors;
	}
}
