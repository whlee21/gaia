package org.apache.solr.analysis;

import java.util.Map;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilterFactory;

@Deprecated
public class JapaneseBasicFormFilterFactory extends JapaneseBaseFormFilterFactory {
	public JapaneseBasicFormFilterFactory(Map<String, String> args) {
		super(args);
	}
}
