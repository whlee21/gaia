package gaia.server.servlet;

import gaia.server.api.model.GuicyInterface;
import gaia.server.api.model.GuicyInterfaceImpl;
import gaia.server.api.services.HelloGuice;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.google.inject.Scopes;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class HelloServletModule extends JerseyServletModule {
	@Override
	protected void configureServlets() {
        // Must configure at least one JAX-RS resource or the 
        // server will fail to start.
        bind(HelloGuice.class);
        bind(GuicyInterface.class).to(GuicyInterfaceImpl.class);
        
        bind(GuiceContainer.class);
        
        bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
         
        // Route all requests through GuiceContainer
        serve("/*").with(GuiceContainer.class);
	}
}
