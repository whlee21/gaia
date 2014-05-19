package gaia.api;


import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Constants;

public class CollectionTemplatesServerResource {
	private static transient Logger LOG = LoggerFactory.getLogger(CollectionTemplatesServerResource.class);

	public static final String TEMPLATES_DIR_NAME = "collection_templates";
	public static final File APP_TEMPLATES_DIR;
	public static final File CONF_TEMPLATES_DIR;


	static {
		File appTemp = null;
		File confTemp = null;

		if (Constants.GAIA_APP_HOME != null) {
			appTemp = new File(Constants.GAIA_APP_HOME, "collection_templates");
		}
		if (Constants.GAIA_CONF_HOME != null)
			confTemp = new File(Constants.GAIA_CONF_HOME, "collection_templates");

		APP_TEMPLATES_DIR = appTemp;
		CONF_TEMPLATES_DIR = confTemp;
	}

}
