package gaia.security.ad;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ADGroupHierarchyHelper {
	private HashMap<String, ADGroup> groupIndex = new HashMap<String, ADGroup>();
	private HashMap<String, HashSet<String>> groupToSidSet = new HashMap<String, HashSet<String>>();

	public ADGroup getOrCreateGroup(String dn) {
		ADGroup group = (ADGroup) groupIndex.get(dn);
		if (group == null) {
			group = new ADGroup(dn);
			groupIndex.put(dn, group);
		}
		return group;
	}

	public void rebuild() {
		for (Map.Entry<String, ADGroup> entry : groupIndex.entrySet()) {
			HashSet<String> groupHierarchy = new HashSet<String>();
			addParentsRecursive(groupHierarchy, (String) entry.getKey());
			HashSet<String> sidSet = new HashSet<String>();
			for (String dn : groupHierarchy) {
				sidSet.add(((ADGroup) groupIndex.get(dn)).getSid());
			}
			groupToSidSet.put(entry.getKey(), sidSet);
		}
	}

	private void addParentsRecursive(HashSet<String> groupHierarchy, String addition) {
		if (!groupHierarchy.contains(addition)) {
			groupHierarchy.add(addition);
			ADGroup group = (ADGroup) groupIndex.get(addition);
			if (group.getParents().size() > 0)
				for (ADGroup parent : group.getParents())
					if (!groupHierarchy.contains(parent.getDN()))
						addParentsRecursive(groupHierarchy, parent.getDN());
		}
	}

	public Set<String> getEffectiveSidsforGroupDN(String groupDn) {
		Set<String> res = groupToSidSet.get(groupDn);
		if (res == null) {
			return Collections.emptySet();
		}
		return res;
	}
}
