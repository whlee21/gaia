package gaia.crawl.security;

public class ACE {
	private Principal principal;
	private Type type;

	public ACE(Principal principal, Type type) {
		this.principal = principal;
		this.type = type;
	}

	public Principal getPrincipal() {
		return principal;
	}

	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String toString() {
		return "ACE [principal=" + principal + ", type=" + type + "]";
	}

	public static enum Type {
		ALLOW, DENY;
	}
}
