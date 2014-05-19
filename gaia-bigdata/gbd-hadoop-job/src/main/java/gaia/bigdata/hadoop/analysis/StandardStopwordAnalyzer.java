package gaia.bigdata.hadoop.analysis;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;

public final class StandardStopwordAnalyzer extends AnalyzerWrapper {
	private static final Logger log = LoggerFactory.getLogger(StandardStopwordAnalyzer.class);

	private static CharArraySet stopwords = new CharArraySet(Version.LUCENE_45, 50, false);

	private final StandardAnalyzer stdAnalyzer = new StandardAnalyzer(Version.LUCENE_45, stopwords);

	protected Analyzer getWrappedAnalyzer(String s) {
		return stdAnalyzer;
	}

	protected Analyzer.TokenStreamComponents wrapComponents(String fieldName, Analyzer.TokenStreamComponents components) {
		return components;
	}

	static {
		String stopwordsFile = "stopwords.txt";
		log.info("Initializing stopwords...");
		InputStream propsStream = null;
		try {
			propsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(stopwordsFile);

			if (propsStream == null) {
				propsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + stopwordsFile);
			}

			if (propsStream != null) {
				BufferedReader br = new BufferedReader(new InputStreamReader(propsStream));
				String strLine;
				while ((strLine = br.readLine()) != null) {
					stopwords.add(strLine);
					log.info("stopword: " + strLine);
				}
			}
		} catch (Exception e) {
			log.error("Error initializing StandardStopwordAnalyzer: " + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			log.info("Initialized StandardStopwordAnalyzer with stream: " + propsStream);
			Closeables.closeQuietly(propsStream);
		}
	}
}
