 package gaia.api;
 
 import java.util.List;

import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
 
 public class JDBCDriversServerResource extends ServerResource
   implements JDBCDriversResource
 {

	@Override
	public List<String> retrieve() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Representation accept(Representation paramRepresentation) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
//   private static transient Logger log = LoggerFactory.getLogger(JDBCDriversServerResource.class);
//   private ConnectorManager cm;
//   private String collection;
// 
//   @Inject
//   public JDBCDriversServerResource(ConnectorManager cm)
//   {
//     this.cm = cm;
//   }
// 
//   public void doInit()
//   {
//     this.collection = ((String)getRequestAttributes().get("coll_name"));
//   }
// 
//   @Get("json")
//   public List<String> retrieve() throws Exception
//   {
//     setStatus(Status.SUCCESS_OK);
//     return getCurrentJars(this.cm, this.collection);
//   }
// 
//   @Post
//   public Representation accept(Representation entity) throws Exception
//   {
//     String driverFileName = null;
//     if (entity != null) {
//       if (MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
//         driverFileName = uploadJdbcDriver(this.cm, this.collection, getRequest(), null, getCurrentJars(this.cm, this.collection));
// 
//         getResponse().setLocationRef("jdbcdrivers/" + URLEncoder.encode(driverFileName, "UTF-8"));
//         setStatus(Status.SUCCESS_NO_CONTENT);
//       } else {
//         throw ErrorUtils.statusExp(ResultStatus.STATUS.BAD_REQUEST, new Error("file", E_INVALID_VALUE, "Can't find JDBC driver file in HTTP request"));
//       }
//     }
//     else {
//       throw ErrorUtils.statusExp(ResultStatus.STATUS.BAD_REQUEST, new Error("file", E_MISSING_VALUE, "Can't find JDBC driver file in HTTP request"));
//     }
// 
//     return new EmptyRepresentation();
//   }
// 
//   static List<String> getCurrentJars(ConnectorManager cm, String collection) throws Exception
//   {
//     List<Resource> resources = cm.listResources("gaia.jdbc", collection, null);
//     List jars = new ArrayList();
//     for (Resource res : resources) {
//       if ((res.getProperties() != null) && ("jar".equals(res.getProperties().get("type")))) {
//         jars.add(res.getName());
//       }
//     }
//     return jars;
//   }
// 
//   static String uploadJdbcDriver(ConnectorManager cm, String collection, Request req, String filename, List<String> currentLibs) {
//     DiskFileItemFactory factory = new DiskFileItemFactory();
//     factory.setSizeThreshold(1000240);
//     RestletFileUpload upload = new RestletFileUpload(factory);
//     try
//     {
//       List<FileItem> items = upload.parseRequest(req);
// 
//       boolean found = false;
//       FileItem driverFileItem = null;
//       for (FileItem fi : items) {
//         if (fi.getFieldName().equals("file")) {
//           driverFileItem = fi;
//           found = true;
//         }
//       }
// 
//       if (!found) {
//         log.error("Can't find JDBC driver file in HTTP request");
//         throw ErrorUtils.statusExp(ResultStatus.STATUS.BAD_REQUEST, new Error("file", E_MISSING_VALUE, "Can't find JDBC driver file in HTTP request"));
//       }
// 
//       log.info("Uploading driver " + driverFileItem.getName());
// 
//       if (filename == null) {
//         filename = driverFileItem.getName();
// 
//         if (filename == null) {
//           throw ErrorUtils.statusExp(ResultStatus.STATUS.BAD_REQUEST, new Error("file", E_INVALID_VALUE, "There's no file stream in POST data"));
//         }
// 
//         if (currentLibs.contains(filename)) {
//           throw ErrorUtils.statusExp(ResultStatus.STATUS.CONFLICT, new Error("file", E_FORBIDDEN_OP, "There's such file already - use PUT to update the driver"));
//         }
//       }
// 
//       if (!filename.endsWith(".jar")) {
//         throw ErrorUtils.statusExp(ResultStatus.STATUS.BAD_REQUEST, new Error("file", E_INVALID_VALUE, "Uploaded file is not a regular jar file"));
//       }
// 
//       InputStream is = driverFileItem.getInputStream();
//       try {
//         cm.uploadResource("gaia.jdbc", new Resource(filename, null), is, collection, null);
//       } finally {
//         if (is != null) {
//           IOUtils.closeQuietly(is);
//         }
//       }
//       return filename;
//     } catch (FileUploadException e) {
//       log.error("Got exception during JDBC driver upload", e);
//       throw ErrorUtils.statusExp(ResultStatus.STATUS.SERVER_ERROR, e.getMessage());
//     } catch (ResourceException e) {
//       throw e;
//     } catch (Exception e) {
//       log.error("Got exception during JDBC driver upload", e);
//       throw ErrorUtils.statusExp(ResultStatus.STATUS.SERVER_ERROR, e.getMessage());
//     }
//   }
 }

