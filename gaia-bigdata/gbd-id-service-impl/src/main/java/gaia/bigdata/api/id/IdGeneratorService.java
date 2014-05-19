package gaia.bigdata.api.id;

public interface IdGeneratorService {
	public String generateIdAsString();

	public byte[] generateIdAsBytes();
}
