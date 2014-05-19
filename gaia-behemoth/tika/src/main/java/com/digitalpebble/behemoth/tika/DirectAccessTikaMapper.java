package com.digitalpebble.behemoth.tika;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.DocumentProcessor;

public class DirectAccessTikaMapper extends Mapper<Text, Text, Text, BehemothDocument> {
	private static final Logger LOG = LoggerFactory.getLogger(DirectAccessTikaMapper.class);
	protected DocumentProcessor processor;
	int MAX_SIZE = 20971520;
	protected Configuration conf;
	private Text key = new Text();

	@Override
	protected void map(Text path, Text type, Context context) throws IOException, InterruptedException {
		// configure(context.getConfiguration());
		String pathString = path.toString();
		try {
			pathString = URLDecoder.decode(path.toString(), "UTF-8");
		} catch (Exception e) {
			LOG.warn("Invalid URLEncoded string, file might be inaccessible: " + e.toString());
			pathString = path.toString();
		}

		Path p = new Path(pathString);
		FileSystem fs = p.getFileSystem(conf);
		if (!fs.exists(p)) {
			LOG.warn("File could not be found! " + p.toUri());
			if (context != null)
				context.getCounter("TIKA", "NOT_FOUND");
			return;
		}
		String uri = p.toUri().toString();
		int processed = 0;
		String fn = p.getName().toLowerCase(Locale.ENGLISH);
		if (((type.toString().equals("seq")) || (!type.toString().equals("map"))) || ((processed == 0) && (isArchive(fn)))) {
			InputStream fis = null;
			try {
				fis = fs.open(p);
				if ((fn.endsWith(".gz")) || (fn.endsWith(".tgz")))
					fis = new GZIPInputStream(fis);
				else if ((fn.endsWith(".tbz")) || (fn.endsWith(".tbz2")) || (fn.endsWith(".bzip2"))) {
					fis = new BZip2CompressorInputStream(fis);
				}
				ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(fis));

				ArchiveEntry entry = null;
				while ((entry = input.getNextEntry()) != null) {
					String name = entry.getName();
					long size = entry.getSize();
					byte[] content = new byte[(int) size];
					input.read(content);
					key.set(uri + "!" + name);

					BehemothDocument value = new BehemothDocument();
					value.setUrl(uri + ":" + name);
					value.setContent(content);
					processed++;
					BehemothDocument[] documents = processor.process(value, context);
					if (documents != null) {
						for (int i = 0; i < documents.length; i++) {
							// outputCollector.collect(key, documents[i]);
							context.write(key, documents[i]);
						}
					} else
						LOG.info("Empty parsing result for " + value.getUrl());
				}
			} catch (Throwable t) {
				if (processed == 0)
					LOG.warn("Error unpacking archive: " + p + ", adding as a regular file: " + t.toString());
				else
					LOG.warn("Error unpacking archive: " + p + ", processed " + processed
							+ " entries, skipping remaining entries: " + t.toString());
			} finally {
				if (fis != null) {
					fis.close();
				}
			}
		}
		if (processed == 0) {
			int realSize = (int) fs.getFileStatus(p).getLen();
			int maxLen = Math.min(MAX_SIZE, realSize);
			byte[] fileBArray = new byte[maxLen];
			FSDataInputStream fis = null;
			try {
				fis = fs.open(p);
				fis.readFully(0L, fileBArray);
				fis.close();
				key.set(uri);

				BehemothDocument value = new BehemothDocument();
				value.setUrl(uri);
				value.setContent(fileBArray);
				if (realSize > maxLen) {
					value.getMetadata(true).put(new Text("fetch"),
							new Text("truncated " + realSize + " to " + maxLen + " bytes."));
				}
				BehemothDocument[] documents = processor.process(value, context);
				if (documents != null) {
					for (int i = 0; i < documents.length; i++) {
						// outputCollector.collect(key, documents[i]);
						context.write(key, documents[i]);
					}
				} else
					LOG.info("Empty parsing result for " + value.getUrl());
			} catch (FileNotFoundException e) {
				LOG.warn("File not found " + p + ", skipping: " + e);
				collectErrorDoc(context, p, e);
			} catch (IOException e) {
				LOG.warn("IO error reading file " + p + ", skipping: " + e);
				collectErrorDoc(context, p, e);
			} finally {
				if (fis != null)
					fis.close();
			}
		}
	}

	private static void collectErrorDoc(Context context, Path p, Throwable t) throws IOException, InterruptedException {
		BehemothDocument doc = new BehemothDocument();
		Text key = new Text(p.toUri().toString());
		doc.setUrl(key.toString());
		doc.getMetadata(true).put(new Text("fetch"), new Text("error: " + t.toString()));
		context.write(key, doc);
	}

	private static boolean isArchive(String n) {
		if ((n.endsWith(".cpio")) || (n.endsWith(".jar")) || (n.endsWith(".dump")) || (n.endsWith(".ar"))
				|| (n.endsWith("tar")) || (n.endsWith(".zip")) || (n.endsWith("tar.gz")) || (n.endsWith(".tgz"))
				|| (n.endsWith(".tbz2")) || (n.endsWith(".tbz")) || (n.endsWith("tar.bzip2"))) {
			return true;
		}
		return false;
	}

	@Override
	protected void setup(Context context) {
		this.conf = context.getConfiguration();
		String handlerName = conf.get("tika.processor");
		LOG.info("Configured DocumentProcessor class: " + handlerName);
		if (handlerName != null) {
			Class handlerClass = conf.getClass("tika.processor", DocumentProcessor.class);
			try {
				processor = ((DocumentProcessor) handlerClass.newInstance());
			} catch (InstantiationException e) {
				LOG.error("Exception", e);

				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				LOG.error("Exception", e);
				throw new RuntimeException(e);
			}
		} else {
			processor = new TikaProcessor();
		}
		LOG.info("Using DocumentProcessor class: " + processor.getClass().getName());
		processor.setConf(conf);
	}
}
