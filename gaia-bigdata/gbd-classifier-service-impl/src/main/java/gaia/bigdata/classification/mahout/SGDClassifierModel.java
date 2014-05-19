package gaia.bigdata.classification.mahout;

import gaia.bigdata.api.classification.ClassifierModel;

import java.net.URI;
import java.util.HashMap;

import org.apache.mahout.classifier.AbstractVectorClassifier;
import org.apache.mahout.classifier.sgd.AbstractOnlineLogisticRegression;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class SGDClassifierModel extends ClassifierModel {
	protected AbstractVectorClassifier vectorClassifier;

	public SGDClassifierModel(AbstractVectorClassifier vectorClassifier) {
		this.vectorClassifier = vectorClassifier;
	}

	public SGDClassifierModel(String name, AbstractVectorClassifier vectorClassifier) {
		super(name);
		this.vectorClassifier = vectorClassifier;
	}

	public SGDClassifierModel(String name, URI location) {
		super(name, location);
	}

	public SGDClassifierModel(String name, URI location, AbstractVectorClassifier vectorClassifier) {
		super(name, location);
		this.vectorClassifier = vectorClassifier;
	}

	public SGDClassifierModel(ClassifierModel other) {
		if (other.getType().equalsIgnoreCase("SGD")) {
			name = other.getName();
			type = other.getType();
			version = other.getVersion();
			numCategories = other.getNumCategories();
			numFeatures = other.getNumFeatures();
			desiredReplication = other.getDesiredReplication();
			location = other.getLocation();
			metadata = new HashMap<String, String>();
			if (other.getMetadata() != null) {
				metadata.putAll(other.getMetadata());
			}
			provider = other.getProvider();
			if ((other instanceof SGDClassifierModel))
				vectorClassifier = ((SGDClassifierModel) other).getVectorClassifier();
		} else {
			throw new UnsupportedOperationException("Unsupported ClassifierModel type: " + other);
		}
	}

	@JsonIgnore
	public AbstractVectorClassifier getVectorClassifier() {
		return this.vectorClassifier;
	}

	public void setVectorClassifier(AbstractOnlineLogisticRegression vectorClassifier) {
		this.vectorClassifier = vectorClassifier;
	}

	public String getType() {
		return "SGD";
	}

	public void setType(String type) {
		type = "SGD";
	}
}
