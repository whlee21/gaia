package gaia.upgrade.metadata;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public final class SimpleCollectionsYaml extends Yaml {
	private static final String YAML_PRE = "tag:yaml.org,2002:";

	private static DumperOptions getDumperOptions() {
		DumperOptions doptions = new DumperOptions();
		doptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return doptions;
	}

	public SimpleCollectionsYaml() {
		this(false);
	}

	public SimpleCollectionsYaml(boolean fixS3DataSourceTypeMadness) {
		super(new Ctor(), new Repr(fixS3DataSourceTypeMadness), getDumperOptions());
	}

	private static class Repr extends Representer {
		public boolean fixS3DataSourceTypeMadness = false;

		public Repr(boolean fixS3DataSourceTypeMadness) {
			this.fixS3DataSourceTypeMadness = fixS3DataSourceTypeMadness;
			getPropertyUtils().setBeanAccess(BeanAccess.FIELD);

			addClassTag(SimpleCollectionsYaml.SimpleDataSource.class, new Tag(
					"tag:yaml.org,2002:gaia.crawl.datasource.DataSource"));

			addClassTag(SimpleCollectionsYaml.SimpleLogDataSource.class, new Tag(
					"tag:yaml.org,2002:gaia.crawl.datasource.DataSource"));

			addClassTag(SimpleCollectionsYaml.SimpleAuthentication.class, new Tag(
					"tag:yaml.org,2002:gaia.crawl.datasource.Authentication"));
		}

		protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,
				Tag customTag) {
			if ((javaBean instanceof SimpleCollectionsYaml.SimpleDataSource)) {
				if ((property.getName().equals("sourceUri")) || (property.getName().equals("uriPrefix"))
						|| (property.getName().equals("schedule")) || (property.getName().equals("id"))) {
					return null;
				}

				if ((property.getName().equals("properties")) && (null != propertyValue) && ((propertyValue instanceof Map))) {
					Map<String, String> props = (Map) propertyValue;

					if (fixS3DataSourceTypeMadness) {
						if (props.containsKey("type")) {
							if ("s3".equals(props.get("type")))
								props.put("type", "s3h");
							else if ("s3n".equals(props.get("type"))) {
								props.put("type", "s3");
							}
						}
						if (props.containsKey("url")) {
							String url = props.get("url").toString();
							url = url.replaceAll("s3n://", "s3://");
							props.put("url", url);
						}
					}
				}
			}

			return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
		}
	}

	public static class SimpleAuthentication {
		public String host;
		public String realm;
		public String username;
		public String password;
	}

	public static class SimpleLogDataSource extends SimpleCollectionsYaml.SimpleDataSource {
	}

	public static class SimpleDataSource implements Serializable {
		public Map<String, Object> properties = new HashMap<String, Object>();
		public Map<String, Object> dsId = new HashMap<String, Object>();
		public Map<String, Object> fieldMapping = new HashMap<String, Object>();
		public Map<String, Object> schedule = new HashMap<String, Object>();
		public int id;
		public Date createDate;
		public Date lastModified;
		public String uriPrefix;
		public String sourceUri;
	}

	private static class Ctor extends Constructor {
		private static String[] dss = { "gaia.crawl.CrawlDataSource", "gaia.crawl.fs.ds.FileFSDataSource",
				"gaia.crawl.fs.ds.HDFSDataSource", "gaia.crawl.fs.ds.S3nFSDataSource", "gaia.crawl.fs.ds.S3FSDataSource",
				"gaia.crawl.fs.ds.CIFSFSDataSource", "gaia.crawl.gcm.GCMDataSource", "gaia.crawl.dih.JDBCDataSource",
				"gaia.admin.collection.datasource.ExternalDataSource", "gaia.admin.collection.datasource.FileSystemDataSource",
				"gaia.admin.collection.datasource.JDBCDataSource", "gaia.admin.collection.datasource.WebDataSource" };

		public Ctor() {
			for (String ds : dss) {
				addTypeDescription(new TypeDescription(SimpleCollectionsYaml.SimpleDataSource.class, YAML_PRE + ds));
			}

			addTypeDescription(new TypeDescription(SimpleCollectionsYaml.SimpleLogDataSource.class,
					"tag:yaml.org,2002:gaia.admin.collection.datasource.LogDataSource"));

			addTypeDescription(new TypeDescription(SimpleCollectionsYaml.SimpleLogDataSource.class,
					"tag:yaml.org,2002:gaia.crawl.gaialogs.LogDataSource"));

			addTypeDescription(new TypeDescription(SimpleCollectionsYaml.SimpleAuthentication.class,
					"tag:yaml.org,2002:gaia.admin.collection.datasource.Authentication"));
		}
	}
}
