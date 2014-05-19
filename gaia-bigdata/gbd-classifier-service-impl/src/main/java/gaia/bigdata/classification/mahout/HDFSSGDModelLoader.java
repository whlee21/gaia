package gaia.bigdata.classification.mahout;

import gaia.bigdata.api.classification.ClassifierModel;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.mahout.classifier.sgd.AbstractOnlineLogisticRegression;
import org.apache.mahout.classifier.sgd.ModelSerializer;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDFSSGDModelLoader extends SGDModelLoader {
	private static transient Logger log = LoggerFactory.getLogger(HDFSSGDModelLoader.class);

	public SGDClassifierModel load(ClassifierModel model) throws IOException, UnsupportedOperationException {
		SGDClassifierModel result = null;
		if (model != null) {
			Configuration conf = new Configuration();
			log.info("Loading model {} from {} ", model, model.getLocation());
			FileSystem fs = FileSystem.get(model.getLocation(), conf);
			if (fs != null) {
				result = new SGDClassifierModel(model);
				FSDataInputStream open = fs.open(new Path(model.getLocation()));
				result.setVectorClassifier((AbstractOnlineLogisticRegression) ModelSerializer.readBinary(open,
						OnlineLogisticRegression.class));
			} else {
				throw new IOException("Unable to obtain a FileSystem to load values from.  Check HDFS Configuration");
			}
		}
		return result;
	}
}
