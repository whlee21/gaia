package gaia.crawl.gcm.api;

public class ConfigureResponse extends CMResponse {
	String formSnippet;
	String message;

	public String getFormSnippet() {
		return this.formSnippet;
	}

	public void setFormSnippet(String formSnippet) {
		this.formSnippet = formSnippet;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
