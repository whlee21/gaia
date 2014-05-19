package gaia.crawl.http.robots;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class SimpleRobotRules extends BaseRobotRules {
	private ArrayList<RobotRule> _rules;
	private RobotRulesMode _mode;

	public SimpleRobotRules() {
		this(RobotRulesMode.ALLOW_SOME);
	}

	public SimpleRobotRules(RobotRulesMode mode) {
		_mode = mode;
		_rules = new ArrayList<RobotRule>();
	}

	public void clearRules() {
		_rules.clear();
	}

	public void addRule(String prefix, boolean allow) {
		if ((!allow) && (prefix.length() == 0)) {
			allow = true;
		}

		_rules.add(new RobotRule(prefix, allow));
	}

	public boolean isAllowed(String url) {
		if (_mode == RobotRulesMode.ALLOW_NONE)
			return false;
		if (_mode == RobotRulesMode.ALLOW_ALL) {
			return true;
		}
		String path = getPath(url);

		if (path.equals("/robots.txt")) {
			return true;
		}

		for (RobotRule rule : _rules) {
			if (path.startsWith(rule._prefix)) {
				return rule._allow;
			}
		}

		return true;
	}

	private String getPath(String url) {
		try {
			String path = new URL(url).getPath();

			if ((path == null) || (path.equals(""))) {
				return "/";
			}

			return URLDecoder.decode(path, "UTF-8").toLowerCase();
		} catch (Exception e) {
		}
		return "/";
	}

	public boolean isAllowAll() {
		return _mode == RobotRulesMode.ALLOW_ALL;
	}

	public boolean isAllowNone() {
		return _mode == RobotRulesMode.ALLOW_NONE;
	}

	protected class RobotRule {
		String _prefix;
		Pattern _pattern;
		boolean _allow;

		public RobotRule(String prefix, boolean allow) {
			_prefix = prefix;
			_pattern = null;
			_allow = allow;
		}

		public RobotRule(Pattern pattern, boolean allow) {
			_prefix = null;
			_pattern = pattern;
			_allow = allow;
		}
	}

	public static enum RobotRulesMode {
		ALLOW_ALL, ALLOW_NONE, ALLOW_SOME;
	}
}
