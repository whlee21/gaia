package gaia.crawl.impl;

import java.io.File;
import java.io.IOException;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.UpdateController;
import gaia.crawl.batch.file.SolrFileWriter;
import gaia.crawl.datasource.DataSource;

public class FileUpdateController extends UpdateController {
	private static final Logger LOG = LoggerFactory.getLogger(FileUpdateController.class);
	SolrFileWriter writer = null;
	File out = null;

	public void init(DataSource ds) throws Exception {
		super.init(ds);
		String output = ds.getString("output_args");
		if (output == null) {
			throw new Exception(
					"Configuration error, output_args is null, should be a file: url to the output dir or an output file.");
		}
		out = new File(output);
		if ((out.exists()) && (out.isFile()))
			throw new Exception("Configuration error, file already exists: " + out.getAbsolutePath());
	}

	public void start() throws Exception {
		super.start();
		File output;
		if (out.isDirectory()) {
			output = File.createTempFile("crawl-" + ds.getDataSourceId() + "-", ".json", out);
			LOG.info("Creating new output file: " + output.getAbsolutePath());
		} else {
			output = out;
		}
		writer = new SolrFileWriter(output);
	}

	public void finish(boolean commit) throws IOException {
		super.finish(commit);
		writer.close();
	}

	public void add(SolrInputDocument doc) throws IOException {
		writer.writeAdd(doc);
	}

	public void delete(String id) throws IOException {
		writer.writeDelete(id);
	}

	public void deleteByQuery(String query) throws IOException {
		writer.writeDeleteByQuery(query);
	}

	public void commit() throws IOException {
	}
}
