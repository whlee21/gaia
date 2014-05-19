package gaia.curator.example1;

import java.util.Collection;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.JsonServiceInstance;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.base.Throwables;

public class WorkerFinder {
	private final ServiceDiscovery<WorkerMetadata> discovery;

	WorkerFinder(CuratorFramework curatorFramework, InstanceSerializerFactory instanceSerializerFactory) {
		discovery = ServiceDiscoveryBuilder
				.builder(WorkerMetadata.class)
				.basePath(Config.basePath)
				.client(curatorFramework)
				.serializer(
						instanceSerializerFactory.getInstanceSerializer(new TypeReference<JsonServiceInstance<WorkerMetadata>>() {
						})).build();

		try {
			discovery.start();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public Collection<ServiceInstance<WorkerMetadata>> getWorkers(String serviceName) {
		Collection<ServiceInstance<WorkerMetadata>> instances;

		try {
			instances = discovery.queryForInstances(serviceName);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		return instances;
	}

	public static void main() {

		// for (ServiceInstance<WorkerMetadata> instance :
		// workerFinder.getWorkers()) {
		// WorkerMetadata workerMetadata = instance.getPayload();
		// // Do something useful here
		// }
	}
}
