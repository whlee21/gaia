package gaia.spellchecking;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.apache.solr.spelling.SolrSpellChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.spellchecking.teragram.TeragramSolrSpeller;

public class GaiaSpellCheckComponent extends SpellCheckComponent {
	private static transient Logger LOG = LoggerFactory.getLogger(GaiaSpellCheckComponent.class);
	public static final String SPELLCHECKER_OVERRIDE = "spellcheck.override";

	protected SolrSpellChecker getSpellChecker(SolrParams params) {
		SolrSpellChecker result = null;
		boolean override = params.getBool("spellcheck.override", false);
		if (!override) {
			result = (SolrSpellChecker) spellCheckers.get("teragram");
			if ((result == null)
					|| ((!((TeragramSolrSpeller) result).isEnabled()) && (!params.getBool("spellcheck.reload", false)))) {
				result = super.getSpellChecker(params);
			}
		} else {
			result = super.getSpellChecker(params);
		}
		return result;
	}

	public String getDescription() {
		return "Gaia Spell Checker component";
	}

	public String getVersion() {
		return "$Revision:$";
	}

	public String getSource() {
		return "$URL:$";
	}
}
