package gaia.common.params;

public class FieldMappingParams {
	public static final String NAME = "fm";
	public static final String ACTION = "fm.action";
	public static final String DATASOURCE = "fm.ds";
	public static final String CHAIN = "fm.chain";

	public static enum Action {
		DEFINE,

		SYNC,

		GET,

		DELETE,

		CLEAR;
	}
}
