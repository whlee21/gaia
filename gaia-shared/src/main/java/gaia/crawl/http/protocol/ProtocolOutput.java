package gaia.crawl.http.protocol;

import gaia.crawl.metadata.Metadata;
import java.net.URL;

public class ProtocolOutput {
	private URL url;
	private Metadata metadata = new Metadata();
	private byte[] data = new byte[0];
	private ProtocolStatus status = ProtocolStatus.OK;

	public ProtocolOutput(URL url, Metadata meta, byte[] data, ProtocolStatus status) {
		this.url = url;
		if (meta != null)
			metadata = meta;
		if (data != null)
			this.data = data;
		if (status != null)
			this.status = status;
	}

	public ProtocolOutput(HttpResponse response, ProtocolStatus status) {
		if (response != null) {
			url = response.getUrl();
			metadata = response.getMetadata();
			data = response.getData();
		}
		if (status != null)
			this.status = status;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public byte[] getData() {
		return data;
	}

	public ProtocolStatus getStatus() {
		return status;
	}

	public URL getUrl() {
		return url;
	}
}
