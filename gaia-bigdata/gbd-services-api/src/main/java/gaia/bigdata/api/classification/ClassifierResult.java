package gaia.bigdata.api.classification;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ClassifierResult {
	public String jsonOutput;
}
