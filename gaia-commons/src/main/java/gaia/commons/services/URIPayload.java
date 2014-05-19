package gaia.commons.services;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class URIPayload {
	public URI uri;
	public Map<String, String> payload = Collections.emptyMap();

	public URIPayload(URI uri) {
		this.uri = uri;
	}

	public URIPayload(URI uri, Map<String, String> payload) {
		this.uri = uri;
		this.payload = payload;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if ((o == null) || (getClass() != o.getClass()))
			return false;

		URIPayload that = (URIPayload) o;

		if (!uri.equals(that.uri))
			return false;

		return true;
	}

	public int hashCode() {
		return uri.hashCode();
	}

	public String toString() {
		return new StringBuilder().append("URIPayload{uri=").append(uri).append(", payload size=")
				.append(payload != null ? Integer.valueOf(payload.size()) : "0").append('}').toString();
	}
}
