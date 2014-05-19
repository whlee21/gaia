package gaia.bigdata.api.id;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUID4IdGeneratorService implements IdGeneratorService {
	public String generateIdAsString() {
		return UUID.randomUUID().toString();
	}

	public byte[] generateIdAsBytes() {
		UUID uuid = UUID.randomUUID();
		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(uuid.getLeastSignificantBits());
		bb.putLong(uuid.getMostSignificantBits());
		return bb.array();
	}
}
