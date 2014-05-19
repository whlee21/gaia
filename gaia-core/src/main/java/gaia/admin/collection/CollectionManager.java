package gaia.admin.collection;

import gaia.admin.roles.Role;
import gaia.jmx.JmxManager;
import java.util.List;

public interface CollectionManager {
	public java.util.Collection<String> getCollectionNames();

	public String getDefaultCollection();

	public void setDefaultCollection(String paramString);

	public Long addCollection(Collection paramCollection);

	public void removeCollection(Collection paramCollection);

	public Collection getCollection(String paramString);

	public List<Collection> getCollections();

	public void updateCollection(Collection paramCollection);

	public void addScheduledSolrCommand(String paramString1, ScheduledSolrCommand paramScheduledSolrCommand,
			String paramString2);

	public void removeScheduledSolrCommand(String paramString1, String paramString2);

	public void updateScheduledSolrCommand(String paramString1, ScheduledSolrCommand paramScheduledSolrCommand,
			String paramString2);

	public ScheduledSolrCommand getScheduledSolrCommand(String paramString1, String paramString2);

	public java.util.Collection<ScheduledSolrCommand> getAllScheduledSolrCommands(String paramString);

	public void save();

	public String getUpdateChain();

	public String getGaiaSearchHandler();

	public String getMltHandler();

	public long getNextId();

	public void reset();

	public void reload();

	public Role getRole(String paramString1, String paramString2);

	public void addRole(String paramString, Role paramRole);

	public void removeRole(String paramString1, String paramString2);

	public java.util.Collection<Role> getRoles(String paramString);

	public void setJmxManager(JmxManager paramJmxManager);
}
