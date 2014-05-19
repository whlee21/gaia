package gaia.hello.server.controller;

public class HelloResponse {

	private String message;

	public HelloResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{" + " message=" + message);
		sb.append(" }");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		HelloResponse that = (HelloResponse) o;

		if (message != null ? !message.equals(that.message)
				: that.message != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 71 * result + (message != null ? message.hashCode() : 0);
		return result;
	}
}
