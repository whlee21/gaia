package gaia.commons.server.api.services;

import gaia.commons.server.Error;

import java.util.Collections;
import java.util.List;

public class ResultStatus {

	/**
	 * STATUS enum. Maps a status to a status code.
	 */
	public static enum STATUS {
		OK(200, "OK", false),
		CREATED(201, "Created", false),
		ACCEPTED(202,"Accepted", false),
		NO_CONTENT(204,"No Content", false),
		CONFLICT(409, "Resource Conflict", true),
		NOT_FOUND(404, "Not Found", true),
		BAD_REQUEST(400, "Bad Request", true),
		UNAUTHORIZED(401, "Unauthorized", true),
		FORBIDDEN(403, "Forbidden", true),
		EXPECTATION_FAILED(417, "Expectation Failed", true),
		UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", true),
		FAILED_DEPENDENCY(424,"Failed Dependency", true),
		SERVER_ERROR(500, "Internal Server Error", true),
		SERVICE_UNAVAILABLE(503, "Service Unavailable", true);

		/**
		 * Status code
		 */
		private int m_code;

		/**
		 * Description
		 */
		private String m_desc;

		/**
		 * whether this is an error state
		 */
		private boolean m_isErrorState;

		/**
		 * Constructor.
		 * 
		 * @param code
		 *            status code
		 * @param description
		 *            description
		 * @param isErrorState
		 *            whether this is an error state
		 */
		private STATUS(int code, String description, boolean isErrorState) {
			m_code = code;
			m_desc = description;
			m_isErrorState = isErrorState;
		}

		/**
		 * Obtain the status code. This is an http response code.
		 * 
		 * @return the status code
		 */
		public int getStatus() {
			return m_code;
		}

		/**
		 * Obtain a brief description.
		 * 
		 * @return the description
		 */
		public String getDescription() {
			return m_desc;
		}

		/**
		 * Whether this status is an error state
		 * 
		 * @return true if this is an error state; false otherwise
		 */
		public boolean isErrorState() {
			return m_isErrorState;
		}

		@Override
		public String toString() {
			return getDescription();
		}
	}

	/**
	 * Status instance
	 */
	private STATUS m_status;

	/**
	 * Result status message
	 */
	private String m_msg;
	
	/**
	 * Errors
	 */
	
	private List<Error> m_errors;

	/**
	 * Constructor.
	 * 
	 * @param status
	 *            result status
	 * @param msg
	 *            result msg. Usually used in case of an error.
	 */
	public ResultStatus(STATUS status, String msg) {
		m_status = status;
		m_msg = msg;
		m_errors = Collections.singletonList(new Error("exception", Error.E_EXCEPTION, msg));
	}
	
	public ResultStatus(STATUS status, Error error) {
		m_status = status;
		m_msg = error.getMessage();
		m_errors = Collections.singletonList(error);
	}
	
	public ResultStatus(STATUS status, List<Error> errors) {
		m_status = status;
		m_errors = errors;
	}
	
	public ResultStatus(STATUS status, String msg, List<Error> errors) {
		m_status = status;
		m_msg = msg;
		m_errors = errors;
	}
	/**
	 * Constructor.
	 * 
	 * @param status
	 *            result status
	 */
	public ResultStatus(STATUS status) {
		m_status = status;
	}

	/**
	 * Constructor.
	 * 
	 * @param status
	 *            result status
	 * @param e
	 *            result exception
	 */
	public ResultStatus(STATUS status, Exception e) {
		m_status = status;
		m_msg = e.toString();
	}

	/**
	 * Obtain the result status. The result status contains a status code and a
	 * description of the status.
	 * 
	 * @return the result status
	 */
	public STATUS getStatus() {
		return m_status;
	}

	/**
	 * Obtain the status code. This is a shortcut to obtaining the status code
	 * from the associated result status.
	 * 
	 * @return the status code
	 */
	public int getStatusCode() {
		return m_status.getStatus();
	}

	/**
	 * Determine whether the status is an error state. This is a shortcut to
	 * getting this information from the associated result status.
	 * 
	 * @return true if the status is a result state; false otherwise
	 */
	public boolean isErrorState() {
		return m_status.isErrorState();
	}

	/**
	 * Obtain the result message. This message is usually used when an exception
	 * occurred.
	 * 
	 * @return the result message
	 */
	public String getMessage() {
		return m_msg;
	}
	
	public List<Error> getErrors() {
		return m_errors;
	}
}
