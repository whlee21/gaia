package gaia.bigdata.classification.mahout;

import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.classification.ClassifierService;
import gaia.commons.server.APIServerConfig;
import gaia.commons.services.ServiceLocator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mahout.math.Vector;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SGDClassifierService implements ClassifierService {
	private static transient Logger log = LoggerFactory.getLogger(SGDClassifierService.class);
	protected ServiceLocator locator;
	protected Map<String, SGDClassifierModel> models = new ConcurrentHashMap<String, SGDClassifierModel>();
	protected SGDModelLoader loader;
	protected APIServerConfig config;
	protected String protocol;

	@Inject
	public SGDClassifierService(ServiceLocator locator, APIServerConfig config, SGDModelLoader loader) {
		this.locator = locator;
		this.loader = loader;
		this.config = config;
		if (config.useSSL == true)
			this.protocol = Protocol.HTTPS.getSchemeName();
		else
			this.protocol = Protocol.HTTP.getSchemeName();
	}

	public void train(ClassifierModel model, int actual, Vector input) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("training not yet supported for "
				+ SGDClassifierService.class.getSimpleName());
	}

	public Vector classify(ClassifierModel model, Vector input) throws IOException, UnsupportedOperationException {
		Vector result = null;
		SGDClassifierModel scm = (SGDClassifierModel) models.get(model.getName());
		if (scm != null)
			result = scm.getVectorClassifier().classify(input);
		else {
			throw new UnsupportedOperationException("Model not supported: " + model);
		}
		return result;
	}

	public Collection<? extends ClassifierModel> loadedModels() {
		return models.values();
	}

	public ClassifierModel getModel(String modelName) {
		return (ClassifierModel) models.get(modelName);
	}

	public void loadModel(ClassifierModel model) throws IOException, UnsupportedOperationException {
		if (!isSupported(model)) {
			throw new UnsupportedOperationException("The model given is not supported by this service: " + model);
		}
		if (hasRoom(model) > 0) {
			SGDClassifierModel loaded = loader.load(model);
			if (loaded != null) {
				models.put(loaded.getName(), loaded);
				try {
					URI location = new URI(protocol, null, config.address, config.port, config.apiBase
							+ "/classifier", null, null);
					locator.registerService("classifier." + model.getName(), location);
				} catch (URISyntaxException e) {
					log.error("Exception", e);
				}
			}
		} else {
			throw new IOException("The classifier service at " + config.address + ":" + config.port
					+ " does not have room for more models");
		}
	}

	public void unloadModel(ClassifierModel model) throws IOException, UnsupportedOperationException {
		if (models.containsKey(model.getName()))
			models.remove(model.getName());
	}

	public boolean isSupported(ClassifierModel model) {
		return (model != null) && (model.getType() != null) && (model.getType().equalsIgnoreCase("SGD"));
	}

	public int hasRoom(ClassifierModel model) throws IOException {
		int result = 0;
		if (isSupported(model) == true) {
			long size = loader.size(model);

			long freeMem = Runtime.getRuntime().freeMemory();
			long numSlots = freeMem / size;
			if (numSlots > 5L) {
				result = (int) (numSlots - 5L);
			}

		}

		return Math.max(0, result);
	}
}
