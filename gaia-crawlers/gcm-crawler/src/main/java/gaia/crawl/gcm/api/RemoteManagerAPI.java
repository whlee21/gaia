package gaia.crawl.gcm.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RemoteManagerAPI {
	public static final int DEFAULT_TIMEOUT = 3000;

	public CMResponse setConnectorManagerConfig(String paramString1, int paramInt1, String paramString2, int paramInt2)
			throws IOException;

	public Set<String> getConnectorTypeNames() throws IOException;

	public ConnectorType getConnectorType(String paramString) throws IOException;

	public List<ConnectorStatus> getConnectorStatuses() throws IOException;

	public ConnectorStatus getConnectorStatus(String paramString) throws IOException;

	public ConfigureResponse getConfigForm(String paramString1, String paramString2) throws IOException;

	public ConfigureResponse getConfigFormForConnector(String paramString1, String paramString2) throws IOException;

	public ConfigureResponse setConnectorConfig(String paramString1, String paramString2, Map<String, String> paramMap,
			String paramString3, boolean paramBoolean) throws IOException;

	public AuthenticationResponse authenticate(String paramString1, String paramString2, String paramString3,
			String paramString4) throws IOException;

	public Set<String> authorizeDocids(String paramString1, List<String> paramList, String paramString2)
			throws IOException;

	public CMResponse stopTraversal(String paramString) throws IOException;

	public CMResponse setSchedule(String paramString, Schedule paramSchedule) throws IOException;

	public CMResponse removeConnector(String paramString) throws IOException;

	public CMResponse restartConnectorTraversal(String paramString) throws IOException;

	public CMResponse testConnectivity() throws IOException;
}
