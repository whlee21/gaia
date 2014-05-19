package gaia.utils;

import java.util.HashMap;
import java.util.Map;

import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.FieldMapping;

public class IdGeneratorFactory {
	private final Map<DataSourceId, IdGenerator<?>> mapPerDS;

	public IdGeneratorFactory() {
		mapPerDS = new HashMap<DataSourceId, IdGenerator<?>>();
	}

	public synchronized IdGenerator<?> getGenerator(DataSourceId dsId, FieldMapping mapping) {
		IdGenerator<?> generator = mapPerDS.get(dsId);
		if (generator == null) {
			if (mapping == null) {
				generator = new IntegerIdGenerator();
			} else {
				String uniqueKey = mapping.getUniqueKey();
				FieldMapping.FType type = mapping.checkType(uniqueKey);
				switch (type) {
				case STRING:
				case INT:
					generator = new IntegerIdGenerator();
					break;
				case LONG:
					generator = new FilenameIdGenerator();
					break;
				default:
					generator = new DateIdGenerator();
				}
			}
			mapPerDS.put(dsId, generator);
		}
		return generator;
	}
}
