package gaia.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.NameFileComparator;

public class FileUtils {
	private static final long ONE_KB = 1024L;
	private static final long ONE_MB = 1048576L;
	private static final long ONE_GB = 1073741824L;
	public static String hexDigits = "0123456789abcdef";

	private static final Pattern FILE_CNTR = Pattern.compile(".*?_(\\d+)");
	private static final int TEMP_DIR_ATTEMPTS = 10000;

	public static File findRoot(File file) throws IOException {
		if (null == file) {
			return null;
		}
		if (!file.isAbsolute()) {
			return findRoot(file.getAbsoluteFile());
		}
		if (!file.exists()) {
			return null;
		}

		Set<File> roots = new HashSet<File>();
		for (File root : File.listRoots()) {
			roots.add(root.getAbsoluteFile());
		}
		for (File f = file; null != f; f = f.getParentFile()) {
			if (roots.contains(f))
				return f;
		}
		return null;
	}

	private static boolean startsWithIgnoreCase(String str, String prefix) {
		return str.toLowerCase().startsWith(prefix.toLowerCase());
	}

	public static String humanReadableUnits(long bytes) {
		DecimalFormat df = new DecimalFormat("#.#");
		String newSizeAndUnits;
		if (bytes / ONE_GB > 0L) {
			newSizeAndUnits = new StringBuilder().append(String.valueOf(df.format((float) bytes / 1.073742E+09F)))
					.append(" GB").toString();
		} else {
			if (bytes / ONE_MB > 0L) {
				newSizeAndUnits = new StringBuilder().append(String.valueOf(df.format((float) bytes / 1048576.0F)))
						.append(" MB").toString();
			} else {
				if (bytes / ONE_KB > 0L)
					newSizeAndUnits = new StringBuilder().append(String.valueOf(df.format((float) bytes / 1024.0F)))
							.append(" KB").toString();
				else
					newSizeAndUnits = new StringBuilder().append(String.valueOf(bytes)).append(" bytes").toString();
			}
		}
		return newSizeAndUnits;
	}

	public static String replacePathDelimiters(String path) {
		return replacePathDelimiters(path, "_");
	}

	public static String replacePathDelimiters(String path, String replacementChar) {
		if (path == null) {
			return path;
		}
		return path.replaceAll("[:\\\\/]", replacementChar);
	}

	public static String flattenPath(String path) {
		if (path == null)
			return "_e";
		int n = path.length();
		if (n == 0) {
			return "_e";
		}
		String flatName = "";
		for (int i = 0; i < n; i++) {
			char ch = path.charAt(i);
			if (Character.isLetterOrDigit(ch))
				flatName = new StringBuilder().append(flatName).append(ch).toString();
			else if (((ch == '.') && (i > 0) && (i < n - 1)) || (ch == '-') || (ch == '=') || (ch == '!') || (ch == '@')
					|| (ch == '#') || (ch == '$') || (ch == '^') || (ch == '*') || (ch == '(') || (ch == ')') || (ch == '[')
					|| (ch == ']') || (ch == '{') || (ch == '}') || (ch == ',') || (ch == ';') || (ch == '&')) {
				flatName = new StringBuilder().append(flatName).append(ch).toString();
			} else if (ch == '_')
				flatName = new StringBuilder().append(flatName).append("__").toString();
			else if (ch == ' ')
				flatName = new StringBuilder().append(flatName).append("_b").toString();
			else if (ch == ':')
				flatName = new StringBuilder().append(flatName).append("_c").toString();
			else if (ch == '+')
				flatName = new StringBuilder().append(flatName).append("_l").toString();
			else if (ch == '%')
				flatName = new StringBuilder().append(flatName).append("_p").toString();
			else if (ch == '?')
				flatName = new StringBuilder().append(flatName).append("_q").toString();
			else if (ch == '/')
				flatName = new StringBuilder().append(flatName).append("_s").toString();
			else if (ch == '\\') {
				flatName = new StringBuilder().append(flatName).append("_t").toString();
			} else if (ch < 'ÿ') {
				flatName = new StringBuilder().append(flatName).append("_x").append(hexDigits.charAt(ch / '\020' % 16))
						.append(hexDigits.charAt(ch % '\020')).toString();
			} else {
				flatName = new StringBuilder().append(flatName).append("_X").append(hexDigits.charAt(ch / '\020' / 16 / 16))
						.append(hexDigits.charAt(ch / '\020' / 16 % 16)).append(hexDigits.charAt(ch / '\020' % 16))
						.append(hexDigits.charAt(ch % '\020')).toString();
			}

		}

		return flatName;
	}

	public static int hexDigitToInt(char hexDigit) {
		if (Character.isDigit(hexDigit))
			return hexDigit - '0';
		if (Character.isLowerCase(hexDigit))
			return '\n' + hexDigit - 97;
		if (Character.isUpperCase(hexDigit)) {
			return '\n' + hexDigit - 65;
		}
		return 0;
	}

	public static int hexToInt(String hex) {
		if (hex == null) {
			return 0;
		}
		int num = 0;
		int n = hex.length();
		for (int i = 0; i < n; i++) {
			char hexDigit = hex.charAt(i);
			int numDigit = hexDigitToInt(hexDigit);
			num = num * 16 + numDigit;
		}

		return num;
	}

	public static String unflattenPath(String path) {
		if (path == null) {
			return null;
		}
		int n = path.length();
		String flatName = "";
		for (int i = 0; i < n; i++) {
			char ch = path.charAt(i);
			if (ch != '_') {
				flatName = new StringBuilder().append(flatName).append(ch).toString();
			} else {
				i++;
				ch = path.charAt(i);
				if (ch == '_')
					flatName = new StringBuilder().append(flatName).append('_').toString();
				else if (ch == 'b')
					flatName = new StringBuilder().append(flatName).append(' ').toString();
				else if (ch == 'c')
					flatName = new StringBuilder().append(flatName).append(':').toString();
				else if (ch != 'e') {
					if (ch == 'l') {
						flatName = new StringBuilder().append(flatName).append('+').toString();
					} else if (ch == 'p') {
						flatName = new StringBuilder().append(flatName).append('%').toString();
					} else if (ch == 'q') {
						flatName = new StringBuilder().append(flatName).append('?').toString();
					} else if (ch == 's') {
						flatName = new StringBuilder().append(flatName).append('/').toString();
					} else if (ch == 't') {
						flatName = new StringBuilder().append(flatName).append('\\').toString();
					} else if (ch == 'x') {
						i++;
						char ch1 = (char) hexToInt(path.substring(i, i + 2));
						flatName = new StringBuilder().append(flatName).append(ch1).toString();
						i++;
					} else if (ch == 'X') {
						i++;
						char ch1 = (char) hexToInt(path.substring(i, i + 4));
						flatName = new StringBuilder().append(flatName).append(ch1).toString();
						i += 3;
					}
				}
			}
		}
		return flatName;
	}

	public static boolean emptyDirectory(String dirPath) {
		return emptyDirectory(new File(dirPath));
	}

	public static boolean emptyDirectory(File dirFile) {
		File[] files = dirFile.listFiles();
		int n = files.length;
		for (int i = 0; i < n; i++) {
			File file = files[i];
			if (file.isFile()) {
				if (!file.delete())
					return false;
			} else if (file.isDirectory()) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(file);
				} catch (Exception e) {
					return false;
				}
			}
		}
		return true;
	}

	public static int extract(File file, String location) throws FileNotFoundException {
		return extract(file, new File(location));
	}

	public static void decompress(File srcArchive, File dstDir, boolean flatten) throws IOException {
		ZipArchiveInputStream input = new ZipArchiveInputStream(new FileInputStream(srcArchive));
		while (true) {
			ArchiveEntry zipentry = input.getNextEntry();
			if (zipentry == null)
				break;
			String name = zipentry.getName();
			File newFile = flatten ? new File(dstDir, new File(name).getName()) : new File(dstDir, name);

			if (zipentry.isDirectory()) {
				if (!flatten)
					newFile.mkdir();
			} else {
				FileOutputStream output = new FileOutputStream(newFile);
				IOUtils.copy(input, output);
				output.close();
			}
		}
		input.close();
	}

	public static int extract(File file, File location) throws FileNotFoundException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		int num = 0;
		try {
			num = extract(is, location);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return num;
	}

	public static int extract(InputStream is, File location) throws FileNotFoundException {
		int numExtracted = 0;
		byte[] buf = new byte[2048];
		ZipInputStream zf = null;
		try {
			zf = new ZipInputStream(is);

			// int extracted = 0;
			ZipEntry entry;
			while ((entry = zf.getNextEntry()) != null)
				if (!entry.isDirectory()) {
					numExtracted++;

					String pathname = entry.getName();

					// extracted++;

					File outFile = new File(location, pathname);
					Date archiveTime = new Date(entry.getTime());

					if (outFile.exists())
						;
					File parent = new File(outFile.getParent());

					if ((parent != null) && (!parent.exists())) {
						parent.mkdirs();
					}

					FileOutputStream out = new FileOutputStream(outFile);
					while (true) {
						int nRead = zf.read(buf, 0, buf.length);

						if (nRead <= 0) {
							break;
						}
						out.write(buf, 0, nRead);
					}
					IOUtils.closeQuietly(out);

					outFile.setLastModified(archiveTime.getTime());
				}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(zf);
		}

		return numExtracted;
	}

	public static String safeFileName(String name) {
		StringBuilder safeName = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (Character.isLetterOrDigit(ch))
				safeName.append(Character.toLowerCase(ch));
			else {
				safeName.append("_");
			}
		}
		return safeName.toString();
	}

	public static String uniqueFileName(File dir, String name) {
		int retry = 0;

		final int[] cntr = { 0 };
		String instanceDir;
		File file;
		do {
			if (retry++ > TEMP_DIR_ATTEMPTS) {
				throw new RuntimeException(new StringBuilder()
						.append("Could not find an available unique filename for core directory:").append(name).toString());
			}
			dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fname) {
					Matcher m = FileUtils.FILE_CNTR.matcher(fname);
					if (m.matches())
						try {
							int num = Integer.parseInt(m.group(1));
							cntr[0] = Math.max(cntr[0], num + 1);
						} catch (NumberFormatException e) {
						}
					return false;
				}
			});
			instanceDir = new StringBuilder().append(name).append("_").append(cntr[0]).toString();
			file = new File(dir, instanceDir);
		} while (file.exists());
		return instanceDir;
	}

	public static File sortAndPickOldestFile(File[] files) {
		return sortAndPickFirstFile(files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
	}

	public static File sortAndPickNewestFile(File[] files) {
		return sortAndPickFirstFile(files, NameFileComparator.NAME_INSENSITIVE_REVERSE);
	}

	public static File sortAndPickFirstFile(File[] files, Comparator<File> comp) {
		if ((null == files) || (0 == files.length))
			return null;

		Arrays.sort(files, comp);

		return files[0];
	}

	public static File createTempDir() {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		String baseName = new StringBuilder().append(System.currentTimeMillis()).append("-").toString();

		for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
			File tempDir = new File(baseDir, new StringBuilder().append(baseName).append(counter).toString());
			if (tempDir.mkdir()) {
				return tempDir;
			}
		}
		throw new IllegalStateException(new StringBuilder()
				.append("Failed to create directory within 10000 attempts (tried ").append(baseName).append("0 to ")
				.append(baseName).append(9999).append(')').toString());
	}

	public static File createFileInTempDirectory(String filename, InputStream is) throws IOException {
		File tmpDir = createTempDir();
		File file = new File(tmpDir, filename);
		OutputStream os = null;
		try {
			os = new FileOutputStream(file);
			IOUtils.copy(is, os);
		} finally {
			if (os != null) {
				os.close();
			}
		}
		return file;
	}
}
