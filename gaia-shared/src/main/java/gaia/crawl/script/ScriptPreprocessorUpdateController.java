package gaia.crawl.script;

import gaia.Constants;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.impl.SolrJUpdateController;
import gaia.utils.MasterConfUtil;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptPreprocessorUpdateController extends SolrJUpdateController {
	private static transient Logger LOG = LoggerFactory.getLogger(ScriptPreprocessorUpdateController.class);
	private File file;
	private ScriptEngine engine;

	public void init(DataSource ds) throws Exception {
		String origOutputArgs = ds.getString("output_args");
		String name = ds.getDisplayName();

		if (origOutputArgs != null) {
			String[] args = ds.getString("output_args").split(",");

			for (String s : args) {
				if (s.startsWith("script=")) {
					name = s.substring(7);
				}
			}
		}

		URL u = MasterConfUtil.getSolrAddress(true, ds.getCollection());
		ds.setProperty("output_args", "script=" + name + "," + u.toExternalForm());

		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

		engine = scriptEngineManager.getEngineByExtension("js");

		file = new File(Constants.GAIA_CONF_HOME, name + ".js");
		if (!file.exists()) {
			throw new Exception("Script " + file.getAbsolutePath() + " does not exist.");
		}
		LOG.info("Script " + name + " being used for update processing for collection " + ds.getDisplayName());

		super.init(ds);
	}

	public void add(SolrInputDocument doc) throws IOException {
		if (file == null) {
			super.add(doc);
			return;
		}
		ScriptContext context = new SimpleScriptContext();
		Bindings bindings = context.getBindings(100);
		bindings.put("log", LOG);
		bindings.put("doc", doc);
		bindings.put("operation", "add");

		Reader reader = new FileReader(file);
		Object retVal;
		try {
			retVal = engine.eval(reader, bindings);
		} catch (ScriptException e) {
			throw new IOException(e);
		}

		if (((retVal instanceof Boolean)) && (!((Boolean) retVal).booleanValue())) {
			LOG.warn("Adding document " + doc.getFieldValue("id") + " skipped by script");
		} else
			super.add(doc);
	}

	public void delete(String id) throws IOException {
		if (file == null) {
			super.delete(id);
			return;
		}
		ScriptContext context = new SimpleScriptContext();
		Bindings bindings = context.getBindings(100);
		bindings.put("log", LOG);
		bindings.put("id", id);
		bindings.put("operation", "delete");

		Reader reader = new FileReader(file);
		Object retVal;
		try {
			retVal = engine.eval(reader, bindings);
		} catch (ScriptException e) {
			throw new IOException(e);
		}

		id = (String) bindings.get("id");
		LOG.debug("Script deleting: " + id);

		if (((retVal instanceof Boolean)) && (!((Boolean) retVal).booleanValue())) {
			LOG.warn("Deleting document " + id + " skipped by script");
		} else
			super.delete(id);
	}
}
