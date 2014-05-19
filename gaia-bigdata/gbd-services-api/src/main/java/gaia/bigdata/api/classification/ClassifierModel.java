package gaia.bigdata.api.classification;

import java.net.URI;
import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ClassifierModel {
	protected String name;
	protected URI location;
	protected String type;
	protected ModelProvider provider = ModelProvider.MAHOUT;
	protected Map<String, String> metadata;
	protected long version = 0L;
	protected int numCategories = 0;
	protected int numFeatures = 0;
	protected int desiredReplication = 3;

	public ClassifierModel() {
	}

	public ClassifierModel(String name) {
		this.name = name;
	}

	public ClassifierModel(String name, URI location) {
		this.name = name;
		this.location = location;
	}

	public int getNumFeatures() {
		return numFeatures;
	}

	public void setNumFeatures(int numFeatures) {
		this.numFeatures = numFeatures;
	}

	public int getNumCategories() {
		return numCategories;
	}

	public void setNumCategories(int numCategories) {
		this.numCategories = numCategories;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public URI getLocation() {
		return location;
	}

	public void setLocation(URI location) {
		this.location = location;
	}

	public ModelProvider getProvider() {
		return provider;
	}

	public void setProvider(ModelProvider provider) {
		this.provider = provider;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getDesiredReplication() {
		return desiredReplication;
	}

	public void setDesiredReplication(int desiredReplication) {
		this.desiredReplication = desiredReplication;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if ((o == null) || (getClass() != o.getClass()))
			return false;

		ClassifierModel that = (ClassifierModel) o;

		return (location.equals(that.location)) && (name.equals(that.name));
	}

	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + location.hashCode();
		return result;
	}

	public String toString() {
		return "ClassifierModel{name='" + name + '\'' + ", location=" + location + ", version=" + version + '}';
	}
}
