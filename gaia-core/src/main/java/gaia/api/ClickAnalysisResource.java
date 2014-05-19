package gaia.api;

import gaia.Constants;
import java.util.Map;
import org.restlet.Request;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface ClickAnalysisResource {
	public static final String OP_SYNC = "sync";
	public static final String DEFAULT_DATA_PATH = Constants.GAIA_DATA_HOME + "/click";
	public static final String DEFAULT_LOGS_PATH = Constants.GAIA_LOGS_HOME;
	public static final String DATA_PATH = "data_path";
	public static final String LOGS_PATH = "logs_path";
	public static final String PREPARE_PATH = "prepare_path";
	public static final String BOOST_PATH = "boost_path";
	public static final String DICT_PATH = "dict_path";
	public static final String PREPARE = "prepare";
	public static final String BOOST = "boost";
	public static final String DICT = "dict";
	public static final String LOGS = "logs";

	@Get("json")
	public Map<String, Object> status() throws Exception;

	@Put("json")
	public Map<String, Object> process(Map<String, Object> paramMap) throws Exception;

	@Delete("json")
	public String stop() throws Exception;

	public void setRequest(Request paramRequest);
}
