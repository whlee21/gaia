package gaia.crawl.fs.ds;

public class SmbPathValidator {
	public static String validate(String path) {
		if (path.contains("%20")) {
			path = path.replaceAll("%20", " ");
		}

		return path;
	}

	public static String invert(String path) {
		if (path.contains(" ")) {
			path = path.replaceAll(" ", "%20");
		}

		return path;
	}
}
