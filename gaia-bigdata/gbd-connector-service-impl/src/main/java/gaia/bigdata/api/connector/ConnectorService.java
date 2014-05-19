package gaia.bigdata.api.connector;

import gaia.bigdata.api.State;
import java.util.List;
import java.util.Map;

public interface ConnectorService {
	public State create(String paramString, Map<String, Object> paramMap);

	public State update(String paramString1, String paramString2, Map<String, Object> paramMap);

	public List<State> list(String paramString);

	public State lookup(String paramString1, String paramString2);

	public State execute(State paramState);

	public boolean remove(String paramString1, String paramString2);
}
