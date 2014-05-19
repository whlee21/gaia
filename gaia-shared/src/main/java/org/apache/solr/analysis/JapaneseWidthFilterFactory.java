package org.apache.solr.analysis;

import java.util.Map;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;

@Deprecated
public class JapaneseWidthFilterFactory extends CJKWidthFilterFactory {
	public JapaneseWidthFilterFactory(Map<String, String> args) {
		super(args);
	}
}
