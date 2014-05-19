package gaia.utils;

public class ExceptionUtil {
	public static Throwable getRootCause(Throwable t) {
		Throwable cause = t;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause;
	}
}
