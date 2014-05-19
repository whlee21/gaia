package gaia.crawl.gcm.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuthorizationResponse extends CMResponse implements Iterable<String> {
	private Map<String, String> answer = new HashMap<String, String>();

	public void put(String resource, String decision) {
		this.answer.put(resource, decision);
	}

	public Map<String, String> getAnswer() {
		return this.answer;
	}

	public Iterator<String> iterator() {
		return this.answer.keySet().iterator();
	}

	public String get(String key) {
		return (String) this.answer.get(key);
	}
}
