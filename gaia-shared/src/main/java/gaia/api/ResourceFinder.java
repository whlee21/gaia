package gaia.api;

import java.lang.reflect.Type;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

public class ResourceFinder {
	private volatile Injector injector;

	@Inject
	public ResourceFinder(Injector injector) {
		this.injector = injector;
	}

	public Finder finderOf(Key<? extends ServerResource> key) {
		return new ServerResourceKeyFinder(key);
	}

	public Finder finderOf(Class<? extends ServerResource> cls) {
		return new ServerResourceKeyFinder(Key.get(cls));
	}

	public class ServerResourceKeyFinder extends ResourceFinder.KeyFinder {
		private final Key<? extends ServerResource> serverResourceKey;

		ServerResourceKeyFinder(Key<? extends ServerResource> serverResourceKey) {
			super(serverResourceKey.getTypeLiteral().getType());
			this.serverResourceKey = serverResourceKey;
		}

		public ServerResource create(Request request, Response response) {
			return (ServerResource) injector.getInstance(this.serverResourceKey);
		}
	}

	public class KeyFinder extends Finder {
		private final Class<? extends ServerResource> targetClass;

		KeyFinder(Type type) {
			this.targetClass = ((Class) type);
		}

		public final Class<? extends ServerResource> getTargetClass() {
			return this.targetClass;
		}
	}
}
