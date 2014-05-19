package gaia.crawl.dih;

public class DIHStatus {
	private Status status = Status.IDLE;
	private String error;

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String toString() {
		return "DIHStatus [status=" + status + ", error=" + error + "]";
	}

	public static enum Status {
		IDLE, BUSY, FAILED;
	}
}
