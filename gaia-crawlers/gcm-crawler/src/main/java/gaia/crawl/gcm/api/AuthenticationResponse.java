package gaia.crawl.gcm.api;

import java.util.Collection;

public class AuthenticationResponse extends CMResponse {
	private boolean status;
	private Collection<String> groups;

	public AuthenticationResponse(boolean status, Collection<String> groups) {
		this.status = status;
		this.groups = groups;
	}

	public boolean getStatus() {
		return this.status;
	}

	public Collection<String> getGroups() {
		return this.groups;
	}
}
