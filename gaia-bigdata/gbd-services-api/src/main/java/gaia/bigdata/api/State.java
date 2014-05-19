package gaia.bigdata.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.std.ToStringSerializer;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class State implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	private String parentId;
	private String serviceType;
	private String workflowId;
	private Date createTime;
	private Status status = Status.UNKNOWN;
	private Map<String, Object> properties;
	private String collection;
	private Collection<State> children = new ArrayList<State>();
	private Throwable throwable;
	private String errorMsg;

	public State() {
	}

	public State(String id, String collection) {
		this(id, null, null, collection, Status.UNKNOWN, null);
	}

	public State(String id, String parentId, String workflowId, String collection, Status status, Date createTime) {
		this.id = id;
		this.parentId = parentId;
		this.workflowId = workflowId;
		this.createTime = (createTime != null ? createTime : new Date());
		this.status = status;
		this.collection = collection;
	}

	public String getCollection() {
		return this.collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParentId() {
		return this.parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getWorkflowId() {
		return this.workflowId;
	}

	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}

	public Date getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@JsonSerialize(using = ToStringSerializer.class)
	public Throwable getThrowable() {
		return this.throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public void addProperty(String key, Object value) {
		if (this.properties == null) {
			this.properties = new HashMap<String, Object>();
		}
		this.properties.put(key, value);
	}

	public String getServiceType() {
		return this.serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public Collection<State> getChildren() {
		return this.children;
	}

	public void setChildren(Collection<State> children) {
		this.children = children;
	}

	public String toString() {
		return "State{id='" + id + '\'' + ", parentId='" + parentId + '\'' + ", serviceType='" + serviceType + '\''
				+ ", workflowId='" + workflowId + '\'' + ", createTime=" + createTime + ", status=" + status + ", throwable="
				+ throwable + ", errorMsg='" + errorMsg + '\'' + '}';
	}

	public String toStringDebug() {
		return "State{id='" + id + '\'' + ", parentId='" + parentId + '\'' + ", serviceType='" + serviceType + '\''
				+ ", workflowId='" + workflowId + '\'' + ", createTime=" + createTime + ", status=" + status + ", properties="
				+ properties + ", collection='" + collection + '\'' + ", children=" + children + ", throwable=" + throwable
				+ ", errorMsg='" + errorMsg + '\'' + '}';
	}

	public void addChild(State child) {
		children.add(child);
	}
}
