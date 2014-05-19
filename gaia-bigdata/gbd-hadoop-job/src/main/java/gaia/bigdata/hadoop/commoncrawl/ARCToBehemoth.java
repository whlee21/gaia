package gaia.bigdata.hadoop.commoncrawl;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.commoncrawl.protocol.shared.ArcFileItem;

import com.digitalpebble.behemoth.BehemothDocument;

public class ARCToBehemoth extends Mapper<Text, ArcFileItem, Text, BehemothDocument> {
	protected BehemothDocument reuseDoc = new BehemothDocument();

	@Override
	protected void setup(Context context) {
	}

	@Override
	protected void cleanup(Context context) throws IOException {
	}

	protected BehemothDocument newBehemothDocument() {
		reuseDoc.setAnnotations(null);
		reuseDoc.setContent(null);
		reuseDoc.setContentType(null);
		reuseDoc.setMetadata(null);
		reuseDoc.setText(null);
		reuseDoc.setUrl(null);
		return reuseDoc;
	}

	@Override
	protected void map(Text url, ArcFileItem doc, Context context) throws IOException, InterruptedException {
		BehemothDocument outDoc = newBehemothDocument();
		outDoc.setContent(doc.getContent().getReadOnlyBytes());
		outDoc.setContentType(doc.getMimeType());
		outDoc.setUrl(doc.getUri());
		context.write(url, outDoc);
	}
}
