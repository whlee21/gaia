 package gaia.api;
 
 import gaia.crawl.ConnectorManager;
import gaia.utils.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
 
 public class JDBCDriverServerResource extends ServerResource
   implements JDBCDriverResource
 {

	@Override
	public Representation retrieve(Representation paramRepresentation) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Representation accept(Representation paramRepresentation) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
//   private static transient Logger log = LoggerFactory.getLogger(JDBCDriversServerResource.class);
//   private String collection;
//   private String filename;
//   private ConnectorManager cm;
// 
//   @Inject
//   public JDBCDriverServerResource(ConnectorManager cm)
//   {
//     this.cm = cm;
//   }
// 
//   public void doInit()
//   {
//     this.collection = ((String)getRequestAttributes().get("coll_name"));
// 
//     this.filename = ((String)getRequestAttributes().get("filename"));
// 
//     if (!this.filename.endsWith(".jar"))
//       this.filename += ".jar";
//   }
// 
//   @Delete("json")
//   public void remove()
//     throws Exception
//   {
//     List libs = JDBCDriversServerResource.getCurrentJars(this.cm, this.collection);
//     if (!libs.contains(this.filename)) {
//       throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
//     }
// 
//     this.cm.deleteResource("gaia.jdbc", this.filename, this.collection, null);
// 
//     setStatus(Status.SUCCESS_NO_CONTENT);
//   }
// 
//   @Put
//   public Representation accept(Representation entity) throws Exception
//   {
//     List libs = JDBCDriversServerResource.getCurrentJars(this.cm, this.collection);
//     if (!libs.contains(this.filename)) {
//       throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
//     }
// 
//     String driverFileName = null;
//     if (entity != null) {
//       if (MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
//         driverFileName = JDBCDriversServerResource.uploadJdbcDriver(this.cm, this.collection, getRequest(), this.filename, null);
//         getResponse().setLocationRef("jdbcdrivers/" + URLEncoder.encode(driverFileName, "UTF-8"));
//         setStatus(Status.SUCCESS_NO_CONTENT);
//       }
//     }
//     else throw ErrorUtils.statusExp(ResultStatus.STATUS.BAD_REQUEST, new Error("file", E_INVALID_VALUE, "Can't find JDBC driver file in HTTP request"));
// 
//     return new EmptyRepresentation();
//   }
// 
//   @Get("html")
//   public Representation retrieve(Representation entity)
//     throws Exception
//   {
//     List libs = JDBCDriversServerResource.getCurrentJars(this.cm, this.collection);
//     if (!libs.contains(this.filename)) {
//       throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
//     }
//     setStatus(Status.SUCCESS_OK);
//     InputStream is = this.cm.openResource("gaia.jdbc", this.filename, this.collection, null);
//     File file = FileUtils.createFileInTempDirectory(this.filename, is);
//     return new FileRepresentation(file, MediaType.APPLICATION_ALL);
//   }
 }

