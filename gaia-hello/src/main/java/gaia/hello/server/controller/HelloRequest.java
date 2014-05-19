package gaia.hello.server.controller;

public class HelloRequest {

	private String message;
	
	public HelloRequest() {
		this.message = "Request: Hello World!";
	}
	
	public HelloRequest(String message) {
		this.message = message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("{ message= " + message);
	    sb.append(" }");
	    return sb.toString();
	}
}
