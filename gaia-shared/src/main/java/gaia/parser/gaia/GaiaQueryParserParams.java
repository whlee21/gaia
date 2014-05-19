package gaia.parser.gaia;

import org.apache.solr.parser.QueryParser;

public interface GaiaQueryParserParams {
	public static final String NAME = "gaia";
	public static final String QF = "qf";
	public static final String PF = "pf";
	public static final String PF3 = "pf3";
	public static final String PS = "ps";
	public static final int PSDefault = 50;
	public static final String QS = "qs";
	public static final int QSDefault = 0;
	public static final String BQ = "bq";
	public static final String BF = "bf";
	public static final String MULTBOOST = "boost";
	public static final String Q = "q";
	public static final String ALTQ = "q.alt";
	public static final String ALTQDefault = "*:*";
	public static final String NOTUP = "notUp";
	public static final boolean NOTUPDefault = true;
	public static final String OPUP = "opUp";
	public static final boolean OPUPDefault = false;
	public static final String NATUP = "natUp";
	public static final boolean NATUPDefault = false;
	public static final String OPGRAM = "opGram";
	public static final boolean OPGRAMDefault = true;
	public static final String LIKEMIN = "likeMin";
	public static final int LIKEMINDefault = 12;
	public static final String DEFOP = "defOp";
	public static final QueryParser.Operator DEFOPDefault = QueryParser.Operator.AND;
	public static final String RMACC = "rmAcc";
	public static final boolean RMACCDefault = true;
	public static final String TIE = "tie";
	public static final float TIEDefault = 0.01F;
	public static final String NEARSLOP = "nearSlop";
	public static final int NEARSLOPDefault = 15;
	public static final String LEADWILD = "leadWild";
	public static final boolean LEADWILDDefault = true;
	public static final String SYNONYMS_FIELDS = "synonyms.fields";
	public static final String SYNONYMS_FIELDSDefault = "";
	public static final String SYNONYMS_ENABLED = "synonyms.enabled";
	public static final boolean SYNONYMS_ENABLEDDefault = true;
	public static final String STOPWORDS_FIELDS = "stopwords.fields";
	public static final String STOPWORDS_FIELDSDefault = "";
	public static final String SYNONYMS_DEFAULT = "synonyms.default";
	public static final boolean SYNONYMS_DEFAULTDefault = true;
	public static final String STOPWORDS_ENABLED = "stopwords.enabled";
	public static final boolean STOPWORDS_ENABLEDDefault = true;
	public static final String STICKYMODIFIERS = "stickyModifiers";
	public static final boolean STICKYMODIFIERSDefault = true;
	public static final String STRICTCOLON = "strictColon";
	public static final boolean STRICTCOLONDefault = false;
	public static final String BOOSTUNIGRAMS = "boostUnigrams";
	public static final boolean BOOSTUNIGRAMSDefault = false;
	public static final String BOOSTBIGRAMS = "boostBigrams";
	public static final boolean BOOSTBIGRAMSDefault = true;
	public static final String BOOSTTRIGRAMS = "boostTrigrams";
	public static final boolean BOOSTTRIGRAMSDefault = true;
	public static final String TEXTPREFIX = "textPrefix";
	public static final String TEXTPREFIXDefault = "text";
	public static final String MINSIM = "minSim";
	public static final float MINSIMDefault = 2.0F;
	public static final String ALLFIELDS = "allFields";
	public static final String ALLFIELDSDefault = "*";
	public static final String LEFTTORIGHTPREC = "leftToRightPrec";
	public static final boolean LEFTTORIGHTPRECDefault = true;
	public static final String MAXTERMS = "maxTerms";
	public static final int MAXTERMSDefault = 20000;
	public static final int MAXTERMSMin = 10;
	public static final int MAXTERMSMax = 100000;
	public static final String MAXGENTERMS = "maxGenTerms";
	public static final int MAXGENTERMSDefault = 100000;
	public static final int MAXGENTERMSMin = 10;
	public static final int MAXGENTERMSMax = 500000;
	public static final String MAXQUERY = "maxQuery";
	public static final int MAXQUERYDefault = 65536;
	public static final int MAXQUERYMin = 10;
	public static final int MAXQUERYMax = 100000;
	public static final String SPANFUZZY = "spanFuzzy";
	public static final boolean SPANFUZZYDefault = true;
	public static final String MINSTRIPTRAILQMARK = "minStripTrailQMark";
	public static final int MINSTRIPTRAILQMARKDefault = 3;
	public static final String IMPNICE = "impNice";
	public static final boolean IMPNICEDefault = true;
	public static final boolean SORTFIELDNAMESDefault = true;
	public static final String MAXBOOLEANCLAUSES = "maxBooleanClauses";
	public static final int MAXBOOLEANCLAUSESDefault = 100000;
	public static final int MAXBOOLEANCLAUSESMin = 10;
	public static final int MAXBOOLEANCLAUSESMax = 1000000;
	public static final String MINMATCH = "minMatch";
	public static final int MINMATCHDefault = 0;
	public static final int MINMATCHMin = 0;
	public static final int MINMATCHMax = 100;
	public static final int MINMATCHMaxTermCount = 19;
	public static final int MINMATCHMinPercentage = 20;
}
