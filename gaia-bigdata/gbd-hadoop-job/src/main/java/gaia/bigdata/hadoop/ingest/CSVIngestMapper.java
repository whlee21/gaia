package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.solr.common.util.StrUtils;

public class CSVIngestMapper extends AbstractHBaseIngestMapper<LongWritable, Text> {
	public static final String CSV_FIELD_MAPPING = "csvFieldMapping";
	protected Map<Integer, String> fieldMap = null;
	protected CSVStrategy strategy = null;

	private final AbstractJobFixture fixture = new AbstractJobFixture() {
		public void init(Job job) throws IOException {
			boolean override = job.getConfiguration().getBoolean("inputFormatOverride", false);
			if (!override)
				job.setInputFormatClass(TextInputFormat.class);
		}
	};
	private String collection;
	private String idField;

	@Override
	protected void setup(Context context) {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		idField = conf.get("idField", "id");
		collection = conf.get(AbstractHBaseIngestMapper.COLLECTION);
		if (collection == null) {
			throw new RuntimeException("No collection specified, aborting");
		}
		String fieldMapStr = conf.get("csvFieldMapping");
		if (fieldMapStr == null) {
			log.warn("No field mapping specified, mapping to generic names, i.e. field_1, field_2, etc");
			fieldMap = Collections.emptyMap();
		} else {
			fieldMap = parseFieldMapStr(fieldMapStr);
		}
		String stratStr = conf.get("csvStrategy", "default");
		if (stratStr.equalsIgnoreCase("default"))
			strategy = CSVStrategy.DEFAULT_STRATEGY;
		else if (stratStr.equalsIgnoreCase("excel"))
			strategy = CSVStrategy.EXCEL_STRATEGY;
		else if (stratStr.equalsIgnoreCase("tdf"))
			strategy = CSVStrategy.TDF_STRATEGY;
		else
			try {
				Class stratClass = Class.forName(stratStr).asSubclass(CSVStrategy.class);
				strategy = ((CSVStrategy) stratClass.newInstance());
			} catch (ClassNotFoundException e) {
				log.error("Exception", e);
				throw new RuntimeException("Couldn't load CSVStrategy class, aborting");
			} catch (InstantiationException e) {
				log.error("Exception", e);
				throw new RuntimeException("Couldn't load CSVStrategy class, aborting");
			} catch (IllegalAccessException e) {
				log.error("Exception", e);
				throw new RuntimeException("Couldn't load CSVStrategy class, aborting");
			}
	}

	private Map<Integer, String> parseFieldMapStr(String fieldMapStr) {
		Map<Integer, String> result = new HashMap<Integer, String>();

		List<String> keyValues = StrUtils.splitSmart(fieldMapStr, ',');
		for (String keyValue : keyValues) {
			String[] splits = keyValue.split("=");
			if ((splits != null) && (splits.length == 2))
				result.put(Integer.valueOf(Integer.parseInt(splits[0].trim())), splits[1].trim());
			else {
				throw new RuntimeException("Invalid Field mapping passed in");
			}
		}
		return result;
	}

	protected Document[] toDocuments(LongWritable key, Text value, Context context)
			throws IOException {
		Document[] result = null;

		if (key.get() != 0L) {
			result = new Document[1];
			result[0] = new Document(collection);

			CSVParser parser = new CSVParser(new InputStreamReader(new ByteArrayInputStream(value.getBytes(), 0,
					value.getLength()), "UTF-8"), strategy);
			String[] vals = parser.getLine();
			if (vals != null) {
				for (int i = 0; i < vals.length; i++) {
					String name = (String) fieldMap.get(Integer.valueOf(i));
					if (name != null) {
						if (name.equals(idField))
							result[0].id = vals[i];
						else {
							result[0].fields.put(name, vals[i]);
						}
					} else {
						result[0].fields.put("field_" + i, vals[i]);
					}
				}
			}
		}
		return result;
	}

	public AbstractJobFixture getFixture() {
		return fixture;
	}
}
