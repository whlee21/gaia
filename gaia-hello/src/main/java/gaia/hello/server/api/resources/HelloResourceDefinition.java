package gaia.hello.server.api.resources;

import gaia.commons.server.api.resources.BaseResourceDefinition;
import gaia.commons.server.controller.spi.Resource;
import gaia.commons.server.controller.spi.Resource.Type;

public class HelloResourceDefinition extends BaseResourceDefinition {

	public HelloResourceDefinition() {
		super(Resource.Type.Hello);
	}

	public HelloResourceDefinition(Type resourceType) {
		super(resourceType);
	}

	@Override
	public String getPluralName() {
		return "hellos";
	}

	@Override
	public String getSingularName() {
		return "hello";
	}

}
