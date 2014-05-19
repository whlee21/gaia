package gaia.curator.example1;

import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.JsonServiceInstance;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.base.Throwables;

public class WorkerAdvertiser {
	private final CuratorFramework curatorFramework;
	private final InstanceSerializer<WorkerMetadata> jacksonInstanceSerializer;
	private final UUID workerId = UUID.randomUUID();

	private final String serviceName;
	private final String listenAddress;
	private final int listenPort;

	WorkerAdvertiser(CuratorFramework curatorFramework, InstanceSerializerFactory instanceSerializerFactory,
			String serviceName, String listenAddress, int listenPort) {
		this.curatorFramework = curatorFramework;
		this.jacksonInstanceSerializer = instanceSerializerFactory
				.getInstanceSerializer(new TypeReference<JsonServiceInstance<WorkerMetadata>>() {
				});
		this.listenAddress = listenAddress;
		this.listenPort = listenPort;
		this.serviceName = serviceName;
	}

	public void advertiseAvailability() {
		try {
			ServiceDiscovery<WorkerMetadata> discovery = getDiscovery();
			discovery.start();
			discovery.registerService(getInstance());
			discovery.close();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void deAdvertiseAvailability() {
		try {
			ServiceDiscovery<WorkerMetadata> discovery = getDiscovery();
			discovery.start();
			discovery.unregisterService(getInstance());
			discovery.close();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	private ServiceInstance<WorkerMetadata> getInstance() throws Exception {
		WorkerMetadata workerMetadata = new WorkerMetadata(workerId, listenAddress, listenPort);
		return ServiceInstance.<WorkerMetadata> builder().name(serviceName).address(listenAddress).port(listenPort)
				.id(workerId.toString()).payload(workerMetadata).build();
	}

	private ServiceDiscovery<WorkerMetadata> getDiscovery() {
		return ServiceDiscoveryBuilder.builder(WorkerMetadata.class).basePath(Config.basePath).client(curatorFramework)
				.serializer(jacksonInstanceSerializer).build();
	}
}
