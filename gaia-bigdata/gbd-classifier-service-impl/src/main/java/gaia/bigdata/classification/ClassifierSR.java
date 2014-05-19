package gaia.bigdata.classification;

import gaia.bigdata.api.classification.ClassificationResource;
import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.classification.ClassifierResult;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.util.List;
import java.util.Map;

import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.utils.vectors.VectorHelper;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ClassifierSR extends BaseServerResource implements ClassificationResource {
	private static transient Logger log = LoggerFactory.getLogger(ClassifierSR.class);
	private ClassifierService service;
	private String modelName;

	@Inject
	public ClassifierSR(Configuration configuration, ClassifierService service) {
		super(configuration);
		this.service = service;
	}

	protected void doInit() throws ResourceException {
		modelName = ((String) getRequest().getAttributes().get("model"));
	}

	public ClassifierResult classify(Map<String, Object> request) throws Exception {
		ClassifierModel model = service.getModel(modelName);
		ClassifierResult result = null;
		if (model != null) {
			Vector input = requestToVector(request);
			Vector output = service.classify(model, input);
			result = new ClassifierResult();
			result.jsonOutput = VectorHelper.vectorToJson(output, null, Integer.MAX_VALUE, false);
		} else {
			log.error("No model named" + modelName + " exists at this location");
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}
		return result;
	}

	private Vector requestToVector(Map<String, Object> vector) throws Exception {
		Vector result = null;

		Map<String, Object> sparse = (Map) vector.get("sparse");
		int i;
		if (sparse != null) {
			Integer cardinality = (Integer) vector.get("cardinality");
			if (cardinality == null) {
				throw new Exception("cardinality not specified for sparse vector");
			}
			String vecType = (String) vector.get("vecType");
			if (vecType.equalsIgnoreCase("sequential"))
				result = new SequentialAccessSparseVector(cardinality.intValue(), sparse.size());
			else if (vecType.equalsIgnoreCase("random")) {
				result = new RandomAccessSparseVector(cardinality.intValue(), sparse.size());
			}
			for (Map.Entry<String, Object> entry : sparse.entrySet())
				result.setQuick(Integer.parseInt((String) entry.getKey()), ((Double) entry.getValue()).doubleValue());
		} else {
			List<Double> dense = (List) vector.get("dense");
			if (dense != null) {
				result = new DenseVector(dense.size());
				i = 0;
				for (Double val : dense) {
					result.setQuick(i++, val.doubleValue());
				}
			}
		}
		return result;
	}

	public ClassifierResult train(int label, Map<String, Object> train) throws UnsupportedOperationException {
		return null;
	}
}
