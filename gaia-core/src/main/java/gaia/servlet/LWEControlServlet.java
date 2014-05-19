 package gaia.servlet;
 
 import com.google.inject.Inject;
 import com.google.inject.Singleton;
 import gaia.control.RestAPI;
 import org.restlet.Application;
 import org.restlet.Context;
 import org.restlet.ext.servlet.ServerServlet;
 
 @Singleton
 public class LWEControlServlet extends ServerServlet
 {
   private RestAPI application;
 
   @Inject
   public LWEControlServlet(RestAPI application)
   {
     this.application = application;
   }
 
   protected Application createApplication(Context parentContext)
   {
     application.setContext(parentContext.createChildContext());
     return application;
   }
 }

