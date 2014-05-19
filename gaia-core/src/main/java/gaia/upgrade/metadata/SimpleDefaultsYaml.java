package gaia.upgrade.metadata;

import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class SimpleDefaultsYaml extends Yaml {
	private static final String YAML_PRE = "tag:yaml.org,2002:";

	private static DumperOptions getDumperOptions() {
		DumperOptions doptions = new DumperOptions();
		doptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return doptions;
	}

	public SimpleDefaultsYaml() {
		super(new Ctor(), new Repr(), getDumperOptions());
	}

	private static class SimpleFieldMapping {
		public String datasourceField;
		public String defaultField;
		public String dynamicField;
		public String uniqueKey;
		public boolean addGaiaworksFields;
		public boolean addOriginalContent;
		public boolean verifySchema;
		public Map<String, Object> literals = new HashMap<String, Object>();
		public Map<String, Object> mappings = new HashMap<String, Object>();
		public Map<String, Boolean> multiVal = new HashMap<String, Boolean>();
		public Map<String, String> types = new HashMap<String, String>();
	}

	private static class Repr extends Representer {
		Repr() {
			getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
			addClassTag(SimpleDefaultsYaml.SimpleFieldMapping.class, new Tag(YAML_PRE + "gaia.crawl.datasource.FieldMapping"));
		}
	}

	private static class Ctor extends Constructor {
		Ctor() {
			addTypeDescription(new TypeDescription(SimpleDefaultsYaml.SimpleFieldMapping.class, YAML_PRE
					+ "gaia.admin.collection.datasource.FieldMapping"));

			addTypeDescription(new TypeDescription(SimpleDefaultsYaml.SimpleFieldMapping.class, YAML_PRE
					+ "gaia.crawl.datasource.FieldMapping"));
		}
	}
}
