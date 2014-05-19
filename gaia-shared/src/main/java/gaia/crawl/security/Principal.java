package gaia.crawl.security;

public class Principal {
	public static final String USER_NAMESPACE = "USER";
	public static final String GROUP_NAMESPACE = "GROUP";
	private String namespace;
	private String name;

	public Principal(String name) {
		this.name = name;
	}

	public Principal(String name, String namespace) {
		this.name = name;
		this.namespace = namespace;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "Principal [namespace=" + namespace + ", name=" + name + "]";
	}
}
