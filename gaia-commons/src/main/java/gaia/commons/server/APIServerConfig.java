package gaia.commons.server;

public class APIServerConfig {
	public String address;
	public int port;
	public boolean useSSL = false;

	public String apiBase = "/sda/v1";
	public String keystorePath;
	public String keystorePassword;
	public String keyPassword;
	public int minThreads = 5;
	public int maxThreads = 500;
}
