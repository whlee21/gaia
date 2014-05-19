package gaia.search.server.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.resource.Resource;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

public class JdbcDriverService  extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(JdbcDriverService.class);
	private static final Logger LOG = LoggerFactory.getLogger(JdbcDriverService.class);
	private final String collectionName;
	private ConnectorManager cm;

	public JdbcDriverService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			ConnectorManager cm) {
		super(serializer, bodyParser);
		this.collectionName = collectionName;
		this.cm = cm;
	}
	

	private List<String> getCurrentJars(String collection) throws Exception
	{
	  List<Resource> resources = cm.listResources("gaia.jdbc", collection, null);
	  List<String> jars = new ArrayList<String>();
	  for (Resource res : resources) {
	    if ((res.getProperties() != null) && ("jar".equals(res.getProperties().get("type")))) {
	      jars.add(res.getName());
	    }
	  }
	  return jars;
	}

	@GET
	@Produces("text/plain")
	public Response getJdbcDrivers(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		// return handleRequest(headers, null, ui, Request.Type.GET,
		// createJdbcDriverResource(collectionName, null));
		LOG.debug("hhokyung getJdbcDrivers() (collection) = (" + collectionName + ")");
		List<String> jdbcDrivers = getCurrentJars(this.collectionName);
		return buildResponse(Response.Status.OK, jdbcDrivers); 
	}

	@DELETE
	@Path("{filename}")
	@Produces("text/plain")
	public Response deleteJdbcDriver(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("filename") String fileName) throws Exception {
		LOG.debug("hhokyung deleteJdbcDriver() (collection, filename) = (" + collectionName + ", " + fileName + ")");
		
		List<String> libs = getCurrentJars(this.collectionName);
	  if (!libs.contains(fileName)) {
	    throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
	  }
	
	  this.cm.deleteResource("gaia.jdbc", fileName, this.collectionName, null);
	
//	  setStatus(Status.SUCCESS_NO_CONTENT);

		return buildResponse(Response.Status.NO_CONTENT);
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadJdbcDriver(@Context HttpHeaders headers, @Context UriInfo ui,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
		LOG.debug("hhokyung upload filename: " + fileDetail.getFileName());
		// return handleRequest(headers, null, ui, Request.Type.POST,
		// createJdbcDriverResource(collectionName, null));
		List<String> currentLibs = getCurrentJars(this.collectionName);
		uploadJdbcDriver(this.cm, this.collectionName, fileDetail.getFileName(), uploadedInputStream, currentLibs);
		return null;
	}
	

  static String uploadJdbcDriver(ConnectorManager cm, String collection, String filename,  InputStream fileStream, List<String> currentLibs) {
    DiskFileItemFactory factory = new DiskFileItemFactory();
    factory.setSizeThreshold(1000240);
    FileUpload upload = new FileUpload(factory);
    try
    {

      boolean found = false;
     
      if (filename != null) {
      	 found = true;
       }

      if (!found) {
      	LOG.error(i18n.tr("Can't find JDBC driver file in HTTP request"));
        throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("file", Error.E_MISSING_VALUE, i18n.tr("Can't find JDBC driver file in HTTP request")));
      }

      LOG.info("Uploading driver " + filename);


     if (filename == null) {
          throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("file", Error.E_INVALID_VALUE, i18n.tr("There's no file stream in POST data")));
      }

     if (currentLibs.contains(filename)) {
          throw ErrorUtils.statusExp(Response.Status.CONFLICT, new Error("file", Error.E_FORBIDDEN_OP, i18n.tr("There's such file already - use PUT to update the driver")));
      }

      if (!filename.endsWith(".jar")) {
        throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("file", Error.E_INVALID_VALUE, i18n.tr("Uploaded file is not a regular jar file")));
      }

      InputStream is = fileStream;
      try {
        cm.uploadResource("gaia.jdbc", new Resource(filename, null), is, collection, null);
      } finally {
        if (is != null) {
          IOUtils.closeQuietly(is);
        }
      }
      return filename;
    } catch (FileUploadException e) {
      LOG.error("Got exception during JDBC driver upload", e);
      throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, i18n.tr(e.getMessage()));
    } catch (Exception e) {
    	LOG.error("Got exception during JDBC driver upload", e);
      throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, i18n.tr(e.getMessage()));
    }
  }

	@Path("classes")
	public JdbcDriverClassService getJdbcDriverClassHandler(@Context HttpHeaders headers, @Context UriInfo ui) {
		 return new JdbcDriverClassService(this.serializer, this.bodyParser, collectionName, cm);
	}
	

}
