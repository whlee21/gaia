package gaia.admin.collection;

import gaia.admin.roles.Role;
import gaia.jmx.JmxManager;
import gaia.utils.DeepCopy;
import gaia.yaml.YamlBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class YamlCollectionManager extends YamlBean implements CollectionManager {
	
	private static Logger LOG = LoggerFactory.getLogger(YamlCollectionManager.class);
	
	private static final String DEFAULT_COLLECTION = "collection1";
	private static final String LOGS_COLLECTION = "GaiaSearchLogs";
	private transient JmxManager jmxManager;
	protected long nextId;
	private String defaultCollection;
	private Map<String, Collection> collectionsMap;
	private Map<String, Schedule> schedules;
	private Map<String, Map> authorizations;
	private String gaiaSearchHandler;
	private String mltHandler;
	private String updateChain;

	public YamlCollectionManager() {
	}

	@Inject
	public YamlCollectionManager(@Named("collections-filename") String filename) {
		super(filename, false);
	}

	public YamlCollectionManager(String filename, boolean ignoreExistingContents) {
		super(filename, ignoreExistingContents);
	}

	protected synchronized void load(YamlBean yamlBean) {
		YamlCollectionManager cm = (YamlCollectionManager) yamlBean;
		setNextId(cm.getNextId());

		schedules = cm.schedules;
		mltHandler = cm.mltHandler;
		gaiaSearchHandler = cm.gaiaSearchHandler;
		updateChain = cm.updateChain;
		collectionsMap = cm.collectionsMap;

		defaultCollection = cm.defaultCollection;
	}

	protected void init() {
		collectionsMap = new HashMap<String, Collection>();
		schedules = new HashMap<String, Schedule>();

		initDefaultCollections();
		gaiaSearchHandler = "/gaia";
		mltHandler = "/mlt";
		updateChain = "gaia-update-chain";

		save();
	}

	public synchronized long getNextId() {
		return nextId++;
	}

	public synchronized void setNextId(long id) {
		nextId = id;
	}

	private void initRoles(String col) {
		Role role = new Role();
		role.setName("DEFAULT");
		role.addUser(new String[] { "admin" });
		addRole(col, role);
	}

	public String getGaiaSearchHandler() {
		return gaiaSearchHandler;
	}

	public String getMltHandler() {
		return mltHandler;
	}

	public String getUpdateChain() {
		return updateChain;
	}

	private void initDefaultCollections() {
		Collection collection = new Collection();
		long id = getNextId();

		collection.setName(DEFAULT_COLLECTION);
		collection.setInstanceDir("collection1_0");
		collection.setDescription("The default GaiaSearch (TM) collection");
		setDefaultCollection(collection.getName());
		collection.setId(Long.valueOf(id));
		collectionsMap.put(collection.getName(), collection);
		initRoles(collection.getName());

		collection = new Collection();
		id = getNextId();

		collection.setName(LOGS_COLLECTION);
		collection.setInstanceDir(LOGS_COLLECTION);
		collection.setDescription("GaiaSearch (TM) Logs");
		collection.setId(Long.valueOf(id));
		collectionsMap.put(collection.getName(), collection);
		initRoles(collection.getName());
	}

	public synchronized Long addCollection(Collection collection) {
		long id = getNextId();
		collection.setId(Long.valueOf(id));

		collectionsMap.remove(collection.getName());

		collectionsMap.put(collection.getName(), collection);
		initRoles(collection.getName());

		save();

		if (jmxManager != null) {
			jmxManager.registerCollectionMBean(collection.getName());
		}
		return Long.valueOf(id);
	}

	public synchronized Collection getCollection(String collection) {
		Collection coll = (Collection) collectionsMap.get(collection);
		if (coll == null) {
			return null;
		}
		return (Collection) DeepCopy.copy(coll);
	}

	private synchronized Collection collection(String collection) {
		return (Collection) collectionsMap.get(collection);
	}

	public synchronized List<Collection> getCollections() {
		java.util.Collection<Collection> collections = collectionsMap.values();
		List<Collection> collectionList = new ArrayList<Collection>(collections.size());
		for (Collection collection : collections) {
			collectionList.add((Collection) DeepCopy.copy(collection));
		}
		return collectionList;
	}

	public synchronized void removeCollection(Collection collection) {
		collectionsMap.remove(collection.getName());
		save();
		if (jmxManager != null)
			jmxManager.unregisterCollectionMBean(collection.getName());
	}

	public synchronized String getDefaultCollection() {
		return defaultCollection;
	}

	public synchronized void setDefaultCollection(String defaultCollection) {
		this.defaultCollection = defaultCollection;
	}

	public synchronized void updateCollection(Collection collection) {
		collection.setLastModified(new Date());

		collectionsMap.remove(getCollectionNameById(collection.getId()));
		collectionsMap.put(collection.getName(), collection);

		save();
	}

	private String getCollectionNameById(Long id) {
		for (Collection col : collectionsMap.values()) {
			if (col.getId().equals(id)) {
				return col.getName();
			}
		}
		return null;
	}

	public synchronized ScheduledSolrCommand getScheduledSolrCommand(String collection, String id) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		ScheduledSolrCommand cmd = (ScheduledSolrCommand) ((Collection) collectionsMap.get(collection)).getCmds().get(
				id);
		ScheduledSolrCommand returnCmd;
		if (cmd != null)
			returnCmd = (ScheduledSolrCommand) DeepCopy.copy(cmd);
		else
			return null;
		return returnCmd;
	}

	public synchronized java.util.Collection<ScheduledSolrCommand> getAllScheduledSolrCommands(String collection) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		Map<String, ScheduledSolrCommand> cmds = collectionsMap.get(collection).getCmds();
		List<ScheduledSolrCommand> commands = new ArrayList<ScheduledSolrCommand>(cmds.size());
		for (ScheduledSolrCommand cmd : cmds.values()) {
			commands.add((ScheduledSolrCommand) DeepCopy.copy(cmd));
		}

		return commands;
	}

	public synchronized void addScheduledSolrCommand(String collection, ScheduledSolrCommand command, String id) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		((Collection) collectionsMap.get(collection)).getCmds().put(id, command);
		save();
	}

	public synchronized void removeScheduledSolrCommand(String collection, String id) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		((Collection) collectionsMap.get(collection)).getCmds().remove(id);
		save();
	}

	public synchronized void updateScheduledSolrCommand(String collection, ScheduledSolrCommand command, String id) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		removeScheduledSolrCommand(collection, id);
		((Collection) collectionsMap.get(collection)).getCmds().put(id, command);
		save();
	}

	public synchronized Map<String, ScheduledSolrCommand> getCmds(String collection) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		return Collections.unmodifiableMap(((Collection) collectionsMap.get(collection)).getCmds());
	}

	public void setCmds(String collection, Map<String, ScheduledSolrCommand> cmds) {
		if (!collectionsMap.containsKey(collection)) {
			throw new IllegalArgumentException("Collection does not exist:" + collection);
		}
		((Collection) collectionsMap.get(collection)).setCmds(cmds);
	}

	public synchronized List<Schedule> getSchedules() {
		return Collections.unmodifiableList(new ArrayList<Schedule>(schedules.values()));
	}

	public synchronized void setSchedules(List<Schedule> newSchedules) {
		schedules.clear();
		for (Schedule s : newSchedules)
			schedules.put(s.getId(), s);
	}

	public synchronized Role getRole(String collection, String name) {
		Collection col = collection(collection);
		if (col == null) {
			return null;
		}
		Role role = (Role) col.roles.get(name);
		if (role == null) {
			return null;
		}
		return (Role) DeepCopy.copy(role);
	}

	public synchronized void addRole(String collection, Role role) {
		Collection col = collection(collection);
		if (col == null) {
			throw new IllegalArgumentException("Could not find collection:" + collection);
		}
		col.roles.put(role.getName(), role);
		save();
	}

	public synchronized void removeRole(String collection, String role) {
		collection(collection).roles.remove(role);
		save();
	}

	public synchronized void reset() {
		schedules.clear();
		collectionsMap.clear();
		for (Collection collection : collectionsMap.values()) {
			collection.getCmds().clear();
		}

		initDefaultCollections();
		save();
	}

	public synchronized void reload() {
		schedules.clear();
		collectionsMap.clear();
		for (Collection collection : collectionsMap.values()) {
			collection.getCmds().clear();
		}
		construct(file, false, location);
	}

	public synchronized java.util.Collection<Role> getRoles(String collection) {
		Collection col = collection(collection);
		if (col == null) {
			throw new IllegalArgumentException("Collection not found: " + collection + " in:" + collectionsMap.values());
		}
		List<Role> roles = new ArrayList<Role>();
		for (Role role : col.getRoles().values()) {
			roles.add((Role) DeepCopy.copy(role));
		}
		return roles;
	}

	public java.util.Collection<String> getCollectionNames() {
		return Collections.unmodifiableCollection(collectionsMap.keySet());
	}

	public void setJmxManager(JmxManager jmxManager) {
		this.jmxManager = jmxManager;
	}
}
