package gaia.crawl.gcm.api;

public class ConnectorMessageCode {
	public static final int SUCCESS = 0;
	public static final int RESPONSE_HTTP_FAIL = 5210;
	public static final int RESPONSE_EMPTY_REQUEST = 5211;
	public static final int RESPONSE_EMPTY_RESPONSE = 5212;
	public static final int RESPONSE_EMPTY_NODE = 5213;
	public static final int RESPONSE_STATUS_ID_NON_INTEGER = 5214;
	public static final int RESPONSE_NULL_CONNECTOR = 5215;
	public static final int RESPONSE_NULL_CONNECTOR_TYPE = 5216;
	public static final int RESPONSE_NULL_CONNECTOR_STATUS = 5217;
	public static final int RESPONSE_NULL_FORM_SNIPPET = 5218;
	public static final int RESPONSE_NULL_CONFIG_DATA = 5219;
	public static final int RESPONSE_NULL_CONFIGURE = 5220;
	public static final int RESPONSE_NULL_RESOURCE = 5221;
	public static final int RESPONSE_NULL_DOCID = 5222;
	public static final int INVALID_CONNECTOR_CONFIG = 5223;
	public static final int ERROR_PARSING_XML_REQUEST = 5300;
	public static final int EXCEPTION_CONNECTOR_MANAGER = 5301;
	public static final int EXCEPTION_CONNECTOR_EXISTS = 5302;
	public static final int EXCEPTION_CONNECTOR_NOT_FOUND = 5303;
	public static final int EXCEPTION_CONNECTOR_TYPE_NOT_FOUND = 5304;
	public static final int EXCEPTION_INSTANTIATOR = 5305;
	public static final int EXCEPTION_PERSISTENT_STORE = 5306;
	public static final int EXCEPTION_THROWABLE = 5307;
	public static final int EXCEPTION_IO = 5308;
	public static final int EXCEPTION_MALFORMED_URL = 5309;
	public static final int EXCEPTION_NUMBER_FORMAT = 5310;
	public static final int EXCEPTION_SAX = 5311;
	public static final int EXCEPTION_HTTP_CONNECTION = 5312;
	public static final int EXCEPTION_HTTP_SERVLET = 5313;
	public static final int DUPLICATE_CONNECTOR_MANAGER_ID = 5400;
	public static final int INVALID_CONNECTOR_MANAGER_NAME = 5401;
	public static final int INVALID_CONNECTOR_MANAGER_URL = 5402;
	public static final int MISMATCHED_CONNECTOR_MANAGER = 5403;
	public static final int UNKNOWN_CONNECTOR_MANAGER_ID = 5404;
	public static final int INVALID_CONNECTOR_MANAGER_TRUSTED_IP = 5405;
	public static final int CANNOT_CERT_AUTH_ON_HTTPS = 5406;
	public static final int CANNOT_ADD_INTERNAL_CONNECTOR_MANAGER = 5407;
	public static final int ATTEMPT_TO_CHANGE_INTERNAL_MANAGER = 5408;
	public static final int ATTEMPT_TO_DELETE_INTERNAL_MANAGER = 5409;
	public static final int ATTEMPT_TO_DELETE_ACTIVE_MANAGER = 5410;
	public static final int INVALID_CONNECTOR_NAME = 5411;
	public static final int MISSING_OR_UNKNOWN_CONNECTOR_SCHEDULE_VERSION = 5412;
	public static final int OVERLAP_BAD_SCHEDULE = 5413;
	public static final int CONNECTOR_DISCONNECTED = 5414;
	public static final int CONNECTOR_MANAGER_DISCONNECTED = 5415;
	public static final int ATTEMPT_TO_CHANGE_LOCKED_CONNECTOR_MANAGER = 5416;
	public static final int FIRST_SUCCESS_MESSAGE_CODE = 5500;
	public static final int SUCCESS_RESTART_TRAVERSAL = 5500;
	public static final int LAST_SUCCESS_MESSAGE_CODE = 5599;
	public static final int UNSUPPORTED_CALL = 5997;
	public static final int UNSUPPORTED_MESSAGE_CODE = 5998;
	public static final int WRONG_MESSAGE_CODE = 5999;
	public static final int LAST_CONNECTOR_MESSAGE_CODE = 6000;
	private int messageId;
	private String message;
	private Object[] params;

	public ConnectorMessageCode() {
		this.messageId = 0;
		this.message = null;
		this.params = new Object[0];
	}

	public ConnectorMessageCode(int messageId) {
		this.messageId = messageId;
		this.message = null;
		this.params = new Object[0];
	}

	public ConnectorMessageCode(int messageId, Object[] params) {
		this.messageId = messageId;
		this.message = null;
		this.params = params;
	}

	public ConnectorMessageCode(int messageId, String param) {
		this.messageId = messageId;
		this.message = null;
		String[] params = { param };
		this.params = params;
	}

	public ConnectorMessageCode(int messageId, String message, Object[] params) {
		this.messageId = messageId;
		this.message = message;
		this.params = params;
	}

	public boolean isSuccess() {
		return this.messageId == 0;
	}

	public boolean isSuccessMessage() {
		return (this.messageId >= 5500) && (this.messageId <= 5599);
	}

	public boolean hasMessage() {
		return (!isSuccess()) && (this.message != null) && (this.message.length() > 1);
	}

	public int getMessageId() {
		return this.messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
		if (messageId == 0) {
			this.message = null;
			this.params = null;
		}
	}

	public Object[] getParams() {
		return this.params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
