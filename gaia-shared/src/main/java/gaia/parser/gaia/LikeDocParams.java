package gaia.parser.gaia;

public class LikeDocParams implements Cloneable {
	public static final String PARAM_PREFIX = "likeDoc.";
	public static final String FL_PARAM = "likeDoc.fl";
	public static final String[] FL_DEFAULT = { "title", "body" };
	public String[] fl = FL_DEFAULT;
	public static final String TOPX_PARAM = "likeDoc.topX";
	public static final int TOPX_DEFAULT = 5;
	public int topX = 5;
	public static final String BOTTOMX_PARAM = "likeDoc.bottomX";
	public static final int BOTTOMX_DEFAULT = 5;
	public int bottomX = 5;
	public static final String USENEGATIVES_PARAM = "likeDoc.useNegatives";
	public static final boolean USENEGATIVES_DEFAULT = false;
	boolean useNegatives = false;
	public static final String MAXQUERYTERMSPERDOCUMENT_PARAM = "likeDoc.maxQueryTermsPerDocument";
	public static final int MAXQUERYTERMSPERDOCUMENT_DEFAULT = 15;
	public int maxQueryTermsPerDocument = 15;
	public static final String MINTERMFREQ_PARAM = "likeDoc.minTermFreq";
	public static final int MINTERMFREQ_DEFAULT = 1;
	public int minTermFreq = 1;
	public static final String MINDOCFREQ_PARAM = "likeDoc.minDocFreq";
	public static final int MINDOCFREQ_DEFAULT = 1;
	public int minDocFreq = 1;
	public static final String OPERATOR_PARAM = "likeDoc.operator";
	public static final String OPERATOR_DEFAULT = "OR";
	public String operator = "OR";
	public static final String MAXCLAUSES_PARAM = "likeDoc.maxClauses";
	public static final int MAXCLAUSES_DEFAULT = 20;
	public int maxClauses = 20;
	public static final String USESTOPWORDS_PARAM = "likeDoc.useStopwords";
	public static final Boolean USESTOPWORDS_DEFAULT = Boolean.valueOf(true);
	public Boolean useStopwords = USESTOPWORDS_DEFAULT;
	public static final String MINWORDLENGTH_PARAM = "likeDoc.minWordLength";
	public static final int MINWORDLENGTH_DEFAULT = 4;
	public int minWordLength = 4;
	public static final String ALPHA_PARAM = "likeDoc.alpha";
	public static final float ALPHA_DEFAULT = 1.0F;
	public float alpha = 1.0F;
	public static final String BETA_PARAM = "likeDoc.beta";
	public static final float BETA_DEFAULT = 1.0F;
	public float beta = 1.0F;
	public static final String GAMMA_PARAM = "likeDoc.gamma";
	public static final float GAMMA_DEFAULT = 1.0F;
	public float gamma = 1.0F;
	public static final String SEARCHFIELD_PARAM = "likeDoc.searchField";
	public static final String SEARCHFIELD_DEFAULT = "text_all";
	public String searchField = "text_all";

	public LikeDocParams clone() {
		try {
			return (LikeDocParams) super.clone();
		} catch (Exception exception) {
		}
		return null;
	}
}
