package gaia.commons.server.api.services.serializers;

import gaia.commons.server.api.services.Result;
import gaia.commons.server.api.services.ResultStatus;

public interface ResultSerializer {

	/**
	 * Serialize the given result to a format expected by client.
	 * 
	 * 
	 * @param result
	 *            internal result
	 * @return the serialized result
	 */
	Object serialize(Result result);

	/**
	 * Serialize an error result to the format expected by the client.
	 * 
	 * @param error
	 *            the error result
	 * 
	 * @return the serialized error result
	 */
	Object serializeError(ResultStatus error);
}
