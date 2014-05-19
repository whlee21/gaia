package gaia.crawl.http.protocol;

public class UserAgent {
	public static final String DEFAULT_BROWSER_VERSION = "Mozilla/5.0";
	private final String agentName;
	private final String agentEmail;
	private final String agentUrl;
	private final String browserVersion;
	private final String agentVersion;
	private final String agentString;

	public UserAgent(String agentString) {
		this.agentString = agentString;
		this.agentName = null;
		this.agentEmail = null;
		this.agentUrl = null;
		this.browserVersion = null;
		this.agentVersion = null;
	}

	public UserAgent(String agentName, String agentEmail, String agentUrl) {
		this(agentName, agentEmail, agentUrl, "Mozilla/5.0");
	}

	public UserAgent(String agentName, String agentEmail, String agentUrl, String browserVersion) {
		this(agentName, agentEmail, agentUrl, browserVersion, null);
	}

	public UserAgent(String agentName, String agentEmail, String agentUrl, String browserVersion, String agentVersion) {
		this.agentName = agentName;
		this.agentEmail = (agentEmail == null ? "" : agentEmail);
		this.agentUrl = (agentUrl == null ? "" : agentUrl);
		this.browserVersion = browserVersion;
		this.agentVersion = ("/" + agentVersion);
		this.agentString = null;
	}

	public String getAgentName() {
		return agentName;
	}

	public String toString() {
		if (agentString != null) {
			return agentString;
		}

		return String.format("%s (compatible; %s%s; +%s; %s)", new Object[] { browserVersion, getAgentName(),
				agentVersion, agentUrl, agentEmail });
	}
}
