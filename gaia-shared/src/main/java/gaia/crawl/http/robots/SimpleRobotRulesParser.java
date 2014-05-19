package gaia.crawl.http.robots;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRobotRulesParser extends BaseRobotsParser {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleRobotRulesParser.class);

	private static Map<String, RobotDirective> DIRECTIVE_PREFIX = new HashMap<String, RobotDirective>();

	private static final Pattern COLON_DIRECTIVE_DELIMITER = Pattern.compile("[ \t]*:[ \t]*(.*)");
	private static final Pattern BLANK_DIRECTIVE_DELIMITER = Pattern.compile("[ \t]+(.*)");

	private static final Pattern DIRECTIVE_SUFFIX_PATTERN = Pattern.compile("[^: \t]+(.*)");

	private static final Pattern SIMPLE_HTML_PATTERN = Pattern.compile("(?is)<(html|head|body)\\s*>");
	private static final Pattern USER_AGENT_PATTERN = Pattern.compile("(?i)user-agent:");
	private static final int MAX_WARNINGS = 5;
	private static final long MAX_CRAWL_DELAY = 300000L;
	private int _numWarnings;

	private static RobotToken tokenize(String line) {
		for (String prefix : DIRECTIVE_PREFIX.keySet()) {
			int prefixLength = prefix.length();
			if (line.startsWith(prefix)) {
				RobotDirective directive = (RobotDirective) DIRECTIVE_PREFIX.get(prefix);
				String dataPortion = line.substring(prefixLength);

				if (directive.isPrefix()) {
					Matcher m = DIRECTIVE_SUFFIX_PATTERN.matcher(dataPortion);
					if (m.matches()) {
						dataPortion = m.group(1);
					}
				} else {
					Matcher m = COLON_DIRECTIVE_DELIMITER.matcher(dataPortion);
					if (!m.matches()) {
						m = BLANK_DIRECTIVE_DELIMITER.matcher(dataPortion);
					}

					if (m.matches()) {
						return new RobotToken(directive, m.group(1).trim());
					}
				}
			}
		}
		Matcher m = COLON_DIRECTIVE_DELIMITER.matcher(line);
		if (m.matches()) {
			return new RobotToken(RobotDirective.UNKNOWN, line);
		}
		return new RobotToken(RobotDirective.MISSING, line);
	}

	public BaseRobotRules failedFetch(int httpStatusCode) {
		if ((httpStatusCode >= 200) && (httpStatusCode < 300))
			throw new IllegalStateException("Can't use status code constructor with 2xx response");
		SimpleRobotRules result;
		if ((httpStatusCode >= 300) && (httpStatusCode < 400)) {
			result = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_NONE);
			result.setDeferVisits(true);
		} else {
			if ((httpStatusCode >= 400) && (httpStatusCode < 500)) {
				result = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
			} else {
				result = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_NONE);
				result.setDeferVisits(true);
			}
		}
		return result;
	}

	public BaseRobotRules parseContent(String url, byte[] content, String contentType, String robotName) {
		_numWarnings = 0;

		if ((content == null) || (content.length == 0)) {
			return new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
		}

		int bytesLen = content.length;
		int offset = 0;
		String encoding = "us-ascii";

		if ((bytesLen >= 3) && (content[0] == -17) && (content[1] == -69) && (content[2] == -65)) {
			offset = 3;
			bytesLen -= 3;
			encoding = "UTF-8";
		}
		String contentAsStr;
		try {
			contentAsStr = new String(content, offset, bytesLen, encoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Impossible unsupported encoding exception for " + encoding);
		}

		boolean isHtmlType = (contentType != null) && (contentType.toLowerCase().startsWith("text/html"));

		boolean hasHTML = false;
		if ((isHtmlType) || (SIMPLE_HTML_PATTERN.matcher(contentAsStr).find())) {
			if (!USER_AGENT_PATTERN.matcher(contentAsStr).find()) {
				LOG.trace("Found non-robots.txt HTML file: " + url);
				return new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
			}

			if (isHtmlType)
				LOG.debug("HTML content type returned for robots.txt file: " + url);
			else {
				LOG.debug("Found HTML in robots.txt file: " + url);
			}

			hasHTML = true;
		}

		StringTokenizer lineParser = new StringTokenizer(contentAsStr, "\n\r  ");
		ParseState parseState = new ParseState(url, robotName.toLowerCase());
		boolean keepGoing = true;

		while ((keepGoing) && (lineParser.hasMoreTokens())) {
			String line = lineParser.nextToken();

			if (hasHTML) {
				line = line.replaceAll("<[^>]+>", "");
			}

			int hashPos = line.indexOf("#");
			if (hashPos >= 0) {
				line = line.substring(0, hashPos);
			}

			line = line.trim().toLowerCase();
			if (line.length() != 0) {
				RobotToken token = tokenize(line);
				switch (token.getDirective()) {
				case USER_AGENT:
					keepGoing = handleUserAgent(parseState, token);
					break;
				case DISALLOW:
					keepGoing = handleDisallow(parseState, token);
					break;
				case ALLOW:
					keepGoing = handleAllow(parseState, token);
					break;
				case CRAWL_DELAY:
					keepGoing = handleCrawlDelay(parseState, token);
					break;
				case SITEMAP:
					keepGoing = handleSitemap(parseState, token);
					break;
				case HTTP:
					keepGoing = handleHttp(parseState, token);
					break;
				case UNKNOWN:
					reportWarning("Unknown directive in robots.txt file: " + line, url);
					parseState.setFinishedAgentFields(true);
					break;
				case MISSING:
					reportWarning(
							String.format("Unknown line in robots.txt file (size %d): %s",
									new Object[] { Integer.valueOf(content.length), line }), url);
					parseState.setFinishedAgentFields(true);
				}

			}

		}

		SimpleRobotRules result = parseState.getRobotRules();
		if (result.getCrawlDelay() > MAX_CRAWL_DELAY) {
			LOG.debug("Crawl delay exceeds max value - so disallowing all URLs: " + url);
			return new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_NONE);
		}
		return result;
	}

	private void reportWarning(String msg, String url) {
		_numWarnings += 1;

		if (_numWarnings == 1) {
			LOG.warn("Problem processing robots.txt for " + url);
		}

		if (_numWarnings < MAX_WARNINGS)
			LOG.warn("\t" + msg);
	}

	private boolean handleUserAgent(ParseState state, RobotToken token) {
		if (state.isMatchedRealName()) {
			if (state.isFinishedAgentFields()) {
				return false;
			}

			return true;
		}

		if (state.isFinishedAgentFields()) {
			state.setFinishedAgentFields(false);
			state.setAddingRules(false);
		}

		String[] targetNames = state.getTargetName().split(" ");

		String[] agentNames = token.getData().split("[ \t,]");
		for (String agentName : agentNames) {
			if ((agentName.equals("*")) && (!state.isMatchedWildcard())) {
				state.setMatchedWildcard(true);
				state.setAddingRules(true);
			} else {
				for (String targetName : targetNames) {
					if (targetName.startsWith(agentName)) {
						state.setMatchedRealName(true);
						state.setAddingRules(true);
						state.clearRules();
						break;
					}
				}
			}

		}

		return true;
	}

	private boolean handleDisallow(ParseState state, RobotToken token) {
		state.setFinishedAgentFields(true);

		if (!state.isAddingRules()) {
			return true;
		}

		String path = token.getData();
		try {
			path = URLDecoder.decode(path, "UTF-8");

			if (path.length() == 0) {
				state.clearRules();
			} else
				state.addRule(path, false);
		} catch (Exception e) {
			reportWarning("Error parsing robots rules - can't decode path: " + path, state.getUrl());
		}

		return true;
	}

	private boolean handleAllow(ParseState state, RobotToken token) {
		state.setFinishedAgentFields(true);

		if (!state.isAddingRules()) {
			return true;
		}

		String path = token.getData();
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (Exception e) {
			reportWarning("Error parsing robots rules - can't decode path: " + path, state.getUrl());
		}

		if (path.length() == 0) {
			state.clearRules();
		} else
			state.addRule(path, true);

		return true;
	}

	private boolean handleCrawlDelay(ParseState state, RobotToken token) {
		state.setFinishedAgentFields(true);

		if (!state.isAddingRules()) {
			return true;
		}

		String delayString = token.getData();
		if (delayString.length() > 0) {
			try {
				if (delayString.indexOf(46) != -1) {
					double delayValue = Double.parseDouble(delayString) * 1000.0D;
					state.setCrawlDelay(Math.round(delayValue));
				} else {
					long delayValue = Integer.parseInt(delayString) * 1000L;
					state.setCrawlDelay(delayValue);
				}
			} catch (Exception e) {
				reportWarning("Error parsing robots rules - can't decode crawl delay: " + delayString, state.getUrl());
			}
		}

		return true;
	}

	private boolean handleSitemap(ParseState state, RobotToken token) {
		String sitemap = token.getData();
		try {
			String hostname = new URL(sitemap).getHost();
			if ((hostname != null) && (hostname.length() > 0)) {
				hostname = new URI(sitemap).getHost();
				if ((hostname != null) && (hostname.length() > 0))
					state.addSitemap(sitemap);
			}
		} catch (Exception e) {
			reportWarning("Invalid URL with sitemap directive: " + sitemap, state.getUrl());
		}

		return true;
	}

	private boolean handleHttp(ParseState state, RobotToken token) {
		String urlFragment = token.getData();
		if (urlFragment.contains("sitemap")) {
			RobotToken fixedToken = new RobotToken(RobotDirective.SITEMAP, "http:" + token.getData());
			return handleSitemap(state, fixedToken);
		}
		reportWarning("Found raw non-sitemap URL: http:" + urlFragment, state.getUrl());
		return true;
	}

	public int getNumWarnings() {
		return _numWarnings;
	}

	static {
		for (RobotDirective directive : RobotDirective.values()) {
			if (!directive.isSpecial()) {
				String prefix = directive.name().toLowerCase().replaceAll("_", "-");
				DIRECTIVE_PREFIX.put(prefix, directive);
			}
		}

		DIRECTIVE_PREFIX.put("useragent", RobotDirective.USER_AGENT);
		DIRECTIVE_PREFIX.put("useg-agent", RobotDirective.USER_AGENT);
		DIRECTIVE_PREFIX.put("ser-agent", RobotDirective.USER_AGENT);

		DIRECTIVE_PREFIX.put("disallow", RobotDirective.DISALLOW);
		DIRECTIVE_PREFIX.put("desallow", RobotDirective.DISALLOW);
		DIRECTIVE_PREFIX.put("dissalow", RobotDirective.DISALLOW);
		DIRECTIVE_PREFIX.put("dssalow", RobotDirective.DISALLOW);
		DIRECTIVE_PREFIX.put("dsallow", RobotDirective.DISALLOW);

		DIRECTIVE_PREFIX.put("crawl delay", RobotDirective.CRAWL_DELAY);
	}

	private static class RobotToken {
		private SimpleRobotRulesParser.RobotDirective _directive;
		private String _data;

		public RobotToken(SimpleRobotRulesParser.RobotDirective directive, String data) {
			_directive = directive;
			_data = data;
		}

		public SimpleRobotRulesParser.RobotDirective getDirective() {
			return _directive;
		}

		public String getData() {
			return _data;
		}
	}

	private static class ParseState {
		private boolean _matchedRealName;
		private boolean _matchedWildcard;
		private boolean _addingRules;
		private boolean _finishedAgentFields;
		private String _url;
		private String _targetName;
		private SimpleRobotRules _curRules;

		public ParseState(String url, String targetName) {
			_url = url;
			_targetName = targetName;
			_curRules = new SimpleRobotRules();
		}

		public String getTargetName() {
			return _targetName;
		}

		public boolean isMatchedRealName() {
			return _matchedRealName;
		}

		public void setMatchedRealName(boolean matchedRealName) {
			_matchedRealName = matchedRealName;
		}

		public boolean isMatchedWildcard() {
			return _matchedWildcard;
		}

		public void setMatchedWildcard(boolean matchedWildcard) {
			_matchedWildcard = matchedWildcard;
		}

		public boolean isAddingRules() {
			return _addingRules;
		}

		public void setAddingRules(boolean addingRules) {
			_addingRules = addingRules;
		}

		public boolean isFinishedAgentFields() {
			return _finishedAgentFields;
		}

		public void setFinishedAgentFields(boolean finishedAgentFields) {
			_finishedAgentFields = finishedAgentFields;
		}

		public void clearRules() {
			_curRules.clearRules();
		}

		public void addRule(String prefix, boolean allow) {
			_curRules.addRule(prefix, allow);
		}

		public void setCrawlDelay(long delay) {
			_curRules.setCrawlDelay(delay);
		}

		public SimpleRobotRules getRobotRules() {
			return _curRules;
		}

		public String getUrl() {
			return _url;
		}

		public void addSitemap(String sitemap) {
			_curRules.addSitemap(sitemap);
		}
	}

	private static enum RobotDirective {
		USER_AGENT, DISALLOW,

		ALLOW, CRAWL_DELAY, SITEMAP,

		HOST,

		NO_INDEX,

		ACAP_(true, false),

		REQUEST_RATE, VISIT_TIME, ROBOT_VERSION, COMMENT,

		HTTP,

		UNKNOWN(false, true),

		MISSING(false, true);

		private boolean _prefix;
		private boolean _special;

		private RobotDirective() {
			_prefix = false;
			_special = false;
		}

		private RobotDirective(boolean isPrefix, boolean isSpecial) {
			_prefix = isPrefix;
			_special = isSpecial;
		}

		public boolean isSpecial() {
			return _special;
		}

		public boolean isPrefix() {
			return _prefix;
		}
	}
}
