package gaia.curator.example1;

import java.util.UUID;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class WorkerMetadata {
	@JsonProperty("workerId")
	private final UUID workerId;

	@JsonProperty("listenAddress")
	private final String listenAddress;

	@JsonProperty("listenPort")
	private final int listenPort;

	@JsonCreator
	public WorkerMetadata(@JsonProperty("workerId") UUID workerId, @JsonProperty("listenAddress") String listenAddress,
			@JsonProperty("listenPort") int listenPort) {
		this.workerId = workerId;
		this.listenAddress = listenAddress;
		this.listenPort = listenPort;
	}

	public UUID getWorkerId() {
		return workerId;
	}

	public String getListenAddress() {
		return listenAddress;
	}

	public int getListenPort() {
		return listenPort;
	}
}
