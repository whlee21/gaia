package gaia.crawl.behemoth;

import gaia.api.Error;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.MasterConfUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.tika.TikaProcessor;

public class BehemothAccessSpec extends DataSourceSpec {
	private static transient Logger LOG = LoggerFactory.getLogger(BehemothAccessSpec.class);
	public static final String PATH = "path";
	public static final String WORK_PATH = "work_path";
	public static final String TIKA_PROCESSOR = "tika_content_handler";
	public static final String MIME_TYPE = "mime_type";
	public static final String ADD_METADATA = "add_metadata";
	public static final String ADD_ANNOTATIONS = "add_annotations";
	public static final String ANNOTATIONS = "annotations";
	public static final String RECURSE = "recurse";
	public static final String ZK_HOST = "zookeeper_host";
	public static final String HADOOP_CONF = "hadoop_conf";
	public static final String DIRECT_ACCESS = "direct_access";
	public static final String SOLR_SERVER_URL = "solr_server_url";

	public BehemothAccessSpec() {
		super("high_volume_hdfs");
	}

	protected BehemothAccessSpec(String name) {
		super(name);
	}

	protected void addCrawlerSupportedProperties() {
		addCommonBehemothProperties();
		addSpecProperty(new SpecProperty(HADOOP_CONF, "The location of Hadoop configuration files", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true));

		addCommitProperties();

		addFieldMappingProperties();
	}

	protected void addCommonBehemothProperties() {
		addSpecProperty(new SpecProperty(RECURSE, "Boolean indicating whether we should descend into sub directories.",
				Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false));

		addSpecProperty(new SpecProperty(TIKA_PROCESSOR,
				"The name of the Behemoth TikaProcessor class to use.  The default is " + TikaProcessor.class.getName()
						+ ". Must be available from the Hadoop classpath.", String.class, TikaProcessor.class.getName(),
				Validator.NOOP_VALIDATOR, false));

		addSpecProperty(new SpecProperty(
				MIME_TYPE,
				"If the MIME type is known for all the content to be processed, provide it here to save time during content extraction",
				String.class, null, Validator.NOOP_VALIDATOR, false));

		addSpecProperty(new SpecProperty(PATH, "The fully-qualified Hadoop path of the input to be processed",
				String.class, null, Validator.URI_VALIDATOR, true));

		addSpecProperty(new SpecProperty(WORK_PATH, "The Hadoop path where the connector can store intermediate results. ",
				String.class, "/tmp", Validator.NOT_BLANK_VALIDATOR, true));
		
		addSpecProperty(new SpecProperty(SOLR_SERVER_URL, "The solr server url of one collection. ",
				String.class, "http://127.0.0.1:8088/solr/collection1", Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty(ADD_METADATA, "Include metadata obtained during Tika content extraction",
				Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(ADD_ANNOTATIONS,
				"Include Behemoth annotations obtained during content processing", Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(ANNOTATIONS,
				"Space-separated list of annotations to include (e.g. 'Person.string Token')", String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(DIRECT_ACCESS,
				"Directly access source files from a shared file system (avoids copying)", Boolean.class, Boolean.TRUE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(ZK_HOST, "ZooKeeper connection string for GaiaSearch", String.class, null,
				new ZKValidator(), false, SpecProperty.HINTS_ADVANCED));
	}

	public FieldMapping getDefaultFieldMapping() {
		FieldMapping mapping = new FieldMapping();
		FieldMappingUtil.addTikaFieldMapping(mapping, true);

		mapping.defineMapping("text", "body");
		return mapping;
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		Map<String, Object> map = super.cast(input);
		String zkHost = (String) map.get(ZK_HOST);
		if ((zkHost != null) && (zkHost.trim().length() > 0)) {
			return map;
		}
		String outputType = (String) map.get("output_type");
		String outputArgs = (String) map.get("output_args");
		String collection = (String) map.get("collection");
		if (outputArgs == null) {
			outputArgs = "";
		}
		if ((outputType != null) && ((outputType.trim().length() == 0) || (outputType.equalsIgnoreCase("solr")))
				&& (!outputArgs.matches(".*http[s]?://.*"))) {
			String url = null;
			try {
				URL u = MasterConfUtil.getSolrAddress(false, (String) map.get("collection"));
				url = u.toString();
			} catch (Exception e) {
				LOG.warn("Could not obtain Solr URL, using http://localhost:8888/solr/" + collection + " - indexing may fail!",
						e);
				url = "http://localhost:8888/solr/" + collection;
			}
			if (outputArgs.length() == 0)
				outputArgs = url;
			else {
				outputArgs = "," + url;
			}
			map.put("output_args", outputArgs);
		}

		return map;
	}

	private static final class ZKValidator extends Validator {
		Pattern pat = Pattern.compile("([\\w-_\\.]+:\\d+(/[\\w-_\\.]*)*[,\\s]*)+");

		public List<Error> validate(SpecProperty specProp, Object value) {
			if ((value == null) || (value.toString().trim().length() == 0)) {
				if (!specProp.required) {
					return Collections.emptyList();
				}
				List res = new ArrayList(1);
				res.add(new Error(specProp.name, Error.E_EMPTY_VALUE));
				return res;
			}
			String str = value.toString();
			if (this.pat.matcher(str).matches()) {
				return Collections.emptyList();
			}
			List res = new ArrayList(1);
			res.add(new Error(specProp.name, Error.E_INVALID_VALUE, "does not match required pattern '" + this.pat.pattern()
					+ "'"));
			return res;
		}

		public Object cast(SpecProperty specProp, Object value) {
			if (value == null) {
				return null;
			}
			return value.toString().trim();
		}
	}
}
