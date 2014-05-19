package gaia.commons.management;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class JMXMonitoredMap extends ConcurrentHashMap<String, GaiaMBean> {
	private static transient Logger LOG = LoggerFactory.getLogger(JMXMonitoredMap.class);

	private MBeanServer server = null;
	private String jmxRootName;

	@Inject
	public JMXMonitoredMap(String jmxRootName, MBeanServer server) {
		this.jmxRootName = jmxRootName;
		if (jmxRootName == null) {
			throw new NullPointerException("jmxRootName is null");
		}
		this.server = server;
	}

	public void clear() {
		if (server != null) {
			for (Map.Entry<String, GaiaMBean> entry : entrySet()) {
				unregister((String) entry.getKey(), (GaiaMBean) entry.getValue());
			}
		}

		super.clear();
	}

	public GaiaMBean put(String key, GaiaMBean infoBean) {
		if ((server != null) && (infoBean != null)) {
			try {
				ObjectName name = getObjectName(key, infoBean);
				if (server.isRegistered(name)) {
					server.unregisterMBean(name);
				}
				LWDynamicMBean mbean = new LWDynamicMBean(infoBean);
				server.registerMBean(mbean, name);
			} catch (Exception e) {
				LOG.warn("Failed to register info bean: " + key, e);
			}
		}

		return (GaiaMBean) super.put(key, infoBean);
	}

	public GaiaMBean remove(Object key) {
		GaiaMBean infoBean = (GaiaMBean) get(key);
		if (infoBean != null) {
			try {
				unregister((String) key, infoBean);
			} catch (RuntimeException e) {
				LOG.warn("Failed to unregister info bean: " + key, e);
			}
		}
		return (GaiaMBean) super.remove(key);
	}

	private void unregister(String key, GaiaMBean infoBean) {
		if (server == null)
			return;
		try {
			ObjectName name = getObjectName(key, infoBean);
			server.unregisterMBean(name);
		} catch (Exception e) {
			throw new RuntimeException("Failed to unregister info bean: " + key, e);
		}
	}

	private ObjectName getObjectName(String key, GaiaMBean infoBean) throws MalformedObjectNameException {
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("type", key);
		if ((infoBean.getName() != null) && (!"".equals(infoBean.getName()))) {
			map.put("id", infoBean.getName());
		}
		return ObjectName.getInstance(jmxRootName, map);
	}

	static class LWDynamicMBean implements DynamicMBean {
		private GaiaMBean infoBean;
		private HashSet<String> staticStats;

		public LWDynamicMBean(GaiaMBean managedResource) {
			infoBean = managedResource;
			staticStats = new HashSet<String>();

			staticStats.add("name");
			staticStats.add("description");
		}

		public MBeanInfo getMBeanInfo() {
			ArrayList<MBeanAttributeInfo> attrInfoList = new ArrayList<MBeanAttributeInfo>();

			for (String stat : staticStats) {
				attrInfoList.add(new MBeanAttributeInfo(stat, String.class.getName(), null, true, false, false));
			}

			attrInfoList.add(new MBeanAttributeInfo("coreHashCode", String.class.getName(), null, true, false, false));
			try {
				Map<String, Object> dynamicStats = infoBean.getStatistics();
				if (dynamicStats != null)
					for (Map.Entry<String, Object> entry : dynamicStats.entrySet()) {
						String name = (String) entry.getKey();
						if (!staticStats.contains(name))
							attrInfoList.add(new MBeanAttributeInfo(name, String.class.getName(), null, true, false, false));
					}
			} catch (Exception e) {
				LOG.warn("Could not getStatistics on info bean " + infoBean.getName(), e);
			}

			MBeanAttributeInfo[] attrInfoArr = (MBeanAttributeInfo[]) attrInfoList
					.toArray(new MBeanAttributeInfo[attrInfoList.size()]);

			return new MBeanInfo(getClass().getName(), infoBean.getDescription(), attrInfoArr, null, null, null);
		}

		public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
			Object val;
			if ((staticStats.contains(attribute)) && (attribute != null) && (attribute.length() > 0)) {
				try {
					String getter = "get" + attribute.substring(0, 1).toUpperCase(Locale.ENGLISH) + attribute.substring(1);

					Method meth = infoBean.getClass().getMethod(getter, new Class[0]);
					val = meth.invoke(infoBean, new Object[0]);
				} catch (Exception e) {
					throw new AttributeNotFoundException(attribute);
				}
			} else {
				Map<String, Object> list = infoBean.getStatistics();
				val = list.get(attribute);
			}

			if (val != null) {
				return val.toString();
			}
			return val;
		}

		public AttributeList getAttributes(String[] attributes) {
			AttributeList list = new AttributeList();
			for (String attribute : attributes) {
				try {
					list.add(new Attribute(attribute, getAttribute(attribute)));
				} catch (Exception e) {
					LOG.warn("Could not get attibute " + attribute);
				}
			}

			return list;
		}

		public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
				MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Operation not Supported");
		}

		public AttributeList setAttributes(AttributeList attributes) {
			throw new UnsupportedOperationException("Operation not Supported");
		}

		public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
				ReflectionException {
			throw new UnsupportedOperationException("Operation not Supported");
		}
	}
}
