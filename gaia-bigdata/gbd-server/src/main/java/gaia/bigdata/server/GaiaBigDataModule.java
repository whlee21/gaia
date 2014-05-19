package gaia.bigdata.server;

import gaia.bigdata.server.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

public class GaiaBigDataModule extends AbstractModule {

	private static final Logger LOG = LoggerFactory.getLogger(GaiaBigDataModule.class);

	private final Configuration configs;

	public GaiaBigDataModule(Configuration configs) {
		this.configs = configs;
	}

	@Override
	protected void configure() {
		// TODO Auto-generated method stub

	}

}
