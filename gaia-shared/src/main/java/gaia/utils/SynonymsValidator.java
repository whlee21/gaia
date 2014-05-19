package gaia.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SynonymsValidator {
	public static final Pattern BLANK_OR_COMMENT_LINE_PATTERN = Pattern.compile("^\\s*(?:#|$)");

	public static final Pattern MULTIPLE_TERMS_OR_MAPPING_SEPARATOR_PATTERN = Pattern
			.compile("(?<!^\\\\)(?<![^\\\\]\\\\)(?:,|=>)");

	public static final Pattern MULTIPLE_MAPPING_SEPARATORS_PATTERN = Pattern
			.compile("(?<!^\\\\)(?<![^\\\\]\\\\)=>.*(?<![^\\\\]\\\\)=>");

	public static final Pattern EMPTY_TERM_PATTERN = Pattern
			.compile("^\\s*(?:,|=>)|(?<![^\\\\]\\\\),\\s*(?:,|=>)|(?<![^\\\\]\\\\)=>\\s*(?:,|$)|,\\s*$");

	public static final Pattern BAD_MAPPING_SEPARATORS_PATTERN = Pattern.compile("(?<![^\\\\]\\\\)(?:==+>+|=*>>+|=*>=+)");

	public boolean supports(Class<?> clazz) {
		return Reader.class.isAssignableFrom(clazz);
	}

	public Set<String> validate(Object target) {
		Set<String> errors = new HashSet<String>();
		BufferedReader reader = new BufferedReader((Reader) target);

		int lineNum = 0;
		try {
			String line;
			while (null != (line = reader.readLine())) {
				lineNum++;
				if (!BLANK_OR_COMMENT_LINE_PATTERN.matcher(line).lookingAt()) {
					if (!MULTIPLE_TERMS_OR_MAPPING_SEPARATOR_PATTERN.matcher(line).find()) {
						errors.add("No mapping specified on line #" + lineNum);
					}
					if (MULTIPLE_MAPPING_SEPARATORS_PATTERN.matcher(line).find()) {
						errors.add("Multiple \"=>\" mapping separators on line #" + lineNum);
					}
					if (EMPTY_TERM_PATTERN.matcher(line).find()) {
						errors.add("Empty term on line #" + lineNum);
					}
					Matcher matcher = BAD_MAPPING_SEPARATORS_PATTERN.matcher(line);
					if (matcher.find())
						errors.add("Suspicious mapping separator \"" + matcher.group(0) + "\" on line #" + lineNum
								+ " - use \"=>\" instead");
				}
			}
		} catch (IOException e) {
			errors.add("I/O error reading synonyms file");
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close Reader", e);
			}
		}
		return errors;
	}
}
