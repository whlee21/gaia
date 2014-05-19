package gaia.analysis;

import java.util.Map;
import org.apache.lucene.analysis.util.ElisionFilterFactory;

@Deprecated
public class GaiaElisionFilterFactory extends ElisionFilterFactory {
	public GaiaElisionFilterFactory(Map<String, String> args) {
		super(args);
	}
}
