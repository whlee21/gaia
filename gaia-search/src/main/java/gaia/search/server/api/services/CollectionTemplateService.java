package gaia.search.server.api.services;

import gaia.Constants;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FilenameUtils;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Inject;

@Path("/collectiontemplates/")
public class CollectionTemplateService  extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CollectionTemplateService.class);
	
	private static final FilenameFilter ZIP_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return FilenameUtils.isExtension(name, "zip");
		}
	};
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
	
	@Inject
	public CollectionTemplateService(ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(serializer, bodyParser);
	}

	@GET
	@Produces("text/plain")
	public Response getCollections(@Context HttpHeaders headers, @Context UriInfo ui) {
//		return handleRequest(headers, null, ui, Request.Type.GET, createCollectionTemplateResource());
		Set<String> result = new HashSet<String>();

		result.addAll(listTemplates(APP_TEMPLATES_DIR));
		result.addAll(listTemplates(CONF_TEMPLATES_DIR));

		return buildResponse(Response.Status.OK, new LinkedList<String>(result)); 
	}

	private static List<String> listTemplates(File dir) {
		if ((null == dir) || (!dir.exists()) || (!dir.isDirectory()) || (!dir.canRead())) {
			return Collections.emptyList();
		}
		String[] names = dir.list(ZIP_FILTER);
		if (null == names) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("", Error.E_EXCEPTION,
					i18n.tr("I/O Error reading template list: " + dir.getAbsolutePath())));
		}

		return Arrays.asList(names);
	}
}
