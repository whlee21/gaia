package com.digitalpebble.behemoth.tika;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.DocumentProcessor;

public class TikaMapper extends Mapper<Text, BehemothDocument, Text, BehemothDocument> {
	private static final Logger LOG = LoggerFactory.getLogger(TikaMapper.class);
	protected DocumentProcessor processor;

	@Override
	protected void map(Text text, BehemothDocument inputDoc, Context context) throws IOException, InterruptedException {
		BehemothDocument[] documents = this.processor.process(inputDoc, context);
		if (documents != null)
			for (int i = 0; i < documents.length; i++)
				context.write(text, documents[i]);
	}

	@Override
	protected void setup(Context context) {
		String handlerName = context.getConfiguration().get("tika.processor");
		LOG.info("Configured DocumentProcessor class: " + handlerName);
		if (handlerName != null) {
			Class handlerClass = context.getConfiguration().getClass("tika.processor", TikaProcessor.class);
			try {
				this.processor = ((DocumentProcessor) handlerClass.newInstance());
			} catch (InstantiationException e) {
				LOG.error("Exception", e);

				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				LOG.error("Exception", e);
				throw new RuntimeException(e);
			}
		} else {
			this.processor = new TikaProcessor();
		}
		LOG.info("Using DocumentProcessor class: " + this.processor.getClass().getName());
		this.processor.setConf(context.getConfiguration());
	}
}
