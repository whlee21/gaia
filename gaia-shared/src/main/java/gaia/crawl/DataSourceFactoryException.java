package gaia.crawl;

import gaia.api.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataSourceFactoryException extends Exception {
	private List<Error> errors = new ArrayList<Error>();
	private static final long serialVersionUID = -3538382812801819133L;

	public DataSourceFactoryException(String msg) {
		this(msg, null, Collections.singletonList(new Error("", Error.E_EXCEPTION, msg)));
	}

	public DataSourceFactoryException(String key, String msg) {
		this(msg, null, Collections.singletonList(new Error(key, msg)));
	}

	public DataSourceFactoryException(String msg, Error error) {
		this(msg, null, Collections.singletonList(error));
	}

	public DataSourceFactoryException(String msg, List<Error> errors) {
		this(msg, null, errors);
	}

	public DataSourceFactoryException(String msg, Throwable cause) {
		this(msg, cause, Collections.singletonList(new Error("", Error.E_EXCEPTION, cause.getMessage())));
	}

	public DataSourceFactoryException(String msg, Throwable cause, List<Error> errors) {
		super(msg, cause);
		if (errors != null)
			errors.addAll(errors);
	}

	public List<Error> getErrors() {
		return errors;
	}

	public String toString() {
		String res = getMessage();
		if (res == null)
			res = "";
		if (!errors.isEmpty()) {
			if (res.length() > 0)
				res = res + ", ";
			res = res + "errors: " + errors.toString();
		}
		return res;
	}
}
