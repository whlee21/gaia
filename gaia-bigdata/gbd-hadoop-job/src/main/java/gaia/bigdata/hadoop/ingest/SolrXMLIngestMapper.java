package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

public class SolrXMLIngestMapper extends AbstractHBaseIngestMapper<Writable, Text> {
	private final AbstractJobFixture fixture = new AbstractJobFixture() {
		public void init(Job job) throws IOException {
			boolean override = job.getConfiguration().getBoolean("inputFormatOverride", false);
			if (!override)
				job.setInputFormatClass(SequenceFileInputFormat.class);
		}
	};
	private SolrXMLLoader xmlLoader;

	public AbstractJobFixture getFixture() {
		return fixture;
	}

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		super.setup(context);
		String idField = conf.get("idField", "id");
		String collection = conf.get(AbstractHBaseIngestMapper.COLLECTION);
		if (collection == null) {
			throw new RuntimeException("No collection specified, aborting");
		}
		xmlLoader = new SolrXMLLoader(collection, idField);
	}

	protected Document[] toDocuments(Writable key, Text value, Context context) throws IOException {
		try {
			Collection<Document> documents = xmlLoader.readDocs(
					new ByteArrayInputStream(value.getBytes(), 0, value.getLength()), key.toString());
			return (Document[]) documents.toArray(new Document[documents.size()]);
		} catch (XMLStreamException e) {
			log.error("Unable to parse SolrXML from " + key.toString(), e);
		}

		return new Document[0];
	}
}
