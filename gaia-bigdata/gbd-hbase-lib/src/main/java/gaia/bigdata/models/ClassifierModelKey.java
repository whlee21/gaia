package gaia.bigdata.models;

import gaia.bigdata.hbase.Key;

public class ClassifierModelKey implements Key {
	public String modelName;

	public ClassifierModelKey(String modelName) {
		this.modelName = modelName;
	}

	public String toString() {
		return "ClassifierModelKey [modelName=" + modelName + ']';
	}
}
