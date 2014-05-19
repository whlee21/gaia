package gaia.commons.server.api.services;

import gaia.commons.server.api.util.TreeNode;
import gaia.commons.server.controller.spi.Resource;

public interface Result {

	public static enum STATUS {
		OK(200, "OK", false),
		CREATED(201, "Created", false),
		ACCEPTED(202, "Accepted", false),
		CONFLICT(409, "Resource Conflict", true),
		NOT_FOUND(404, "Not Found", true),
		BAD_REQUEST(400, "Bad Request", true),
		UNAUTHORIZED(401, "Unauthorized", true),
		FORBIDDEN(403, "Forbidden", true),
		SERVER_ERROR(500, "Internal Server Error", true);

		private int m_code;
		private String m_desc;
		private boolean m_isErrorState;

		private STATUS(int code, String description, boolean isErrorState) {
			m_code = code;
			m_desc = description;
			m_isErrorState = isErrorState;
		}

		public int getStatus() {
			return m_code;
		}

		public String getDescription() {
			return m_desc;
		}

		public boolean isErrorState() {
			return m_isErrorState;
		}

		@Override
		public String toString() {
			return getDescription();
		}
	};

	/**
	 * Obtain the results of the request invocation as a Tree structure.
	 * 
	 * @return the results of the request a a Tree structure
	 */
	public TreeNode<Resource> getResultTree();

	/**
	 * Determine whether the request was handled synchronously. If the request
	 * is synchronous, all work was completed prior to returning.
	 * 
	 * @return true if the request was synchronous, false if it was asynchronous
	 */
	public boolean isSynchronous();

	public ResultStatus getStatus();

	public void setResultStatus(ResultStatus status);
}
