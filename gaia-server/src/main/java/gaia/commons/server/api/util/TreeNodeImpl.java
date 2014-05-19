package gaia.commons.server.api.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TreeNodeImpl<T> implements TreeNode<T> {

	/**
	 * name of the node
	 */
	private String m_name;

	/**
	 * parent of the node
	 */
	private TreeNode<T> m_parent;

	/**
	 * child nodes
	 */
	private Map<String, TreeNode<T>> m_mapChildren = new HashMap<String, TreeNode<T>>();

	/**
	 * associated object
	 */
	private T m_object;

	/**
	 * properties
	 */
	private Map<String, String> m_mapNodeProps;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            parent node
	 * @param object
	 *            associated object
	 * @param name
	 *            node name
	 */
	public TreeNodeImpl(TreeNode<T> parent, T object, String name) {
		m_parent = parent;
		m_object = object;
		m_name = name;
	}

	@Override
	public TreeNode<T> getParent() {
		return m_parent;
	}

	@Override
	public Collection<TreeNode<T>> getChildren() {
		return m_mapChildren.values();
	}

	@Override
	public T getObject() {
		return m_object;
	}

	@Override
	public void setName(String name) {
		m_name = name;
	}

	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public void setParent(TreeNode<T> parent) {
		m_parent = parent;
	}

	@Override
	public TreeNode<T> addChild(T child, String name) {
		TreeNodeImpl<T> node = new TreeNodeImpl<T>(this, child, name);
		m_mapChildren.put(name, node);

		return node;
	}

	@Override
	public TreeNode<T> addChild(TreeNode<T> child) {
		child.setParent(this);
		m_mapChildren.put(child.getName(), child);

		return child;
	}

	@Override
	public void setProperty(String name, String value) {
		if (m_mapNodeProps == null) {
			m_mapNodeProps = new HashMap<String, String>();
		}
		m_mapNodeProps.put(name, value);
	}

	@Override
	public String getProperty(String name) {
		return m_mapNodeProps == null ? null : m_mapNodeProps.get(name);
	}

	@Override
	public TreeNode<T> getChild(String name) {
		if (name != null && name.contains("/")) {
			int i = name.indexOf('/');
			String s = name.substring(0, i);
			TreeNode<T> node = m_mapChildren.get(s);
			return node == null ? null : node.getChild(name.substring(i + 1));
		} else {
			return m_mapChildren.get(name);
		}
	}

}
