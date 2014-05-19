package gaia.bigdata.hadoop.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.StringUtils;
import org.commoncrawl.hadoop.io.deprecated.ArcFileReader;
import org.commoncrawl.protocol.shared.ArcFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.digitalpebble.behemoth.BehemothDocument;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CommonCrawlSeqFileWriter extends BaseSeqFileWriter {
	private static transient Logger log = LoggerFactory.getLogger(CommonCrawlSeqFileWriter.class);

	@Inject
	CommonCrawlSeqFileWriter(FileSystem fs, Path parent, @Named("prefix") String prefix) throws IOException {
		super(prefix, fs, parent);
	}

	public void write(S3Object object) throws IOException {
		S3ObjectInputStream is = object.getObjectContent();
		final ArcFileReader reader = new ArcFileReader();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					int count = 0;
					while (reader.hasMoreItems()) {
						ArcFileItem item = new ArcFileItem();

						reader.getNextItem(item);

						if (CommonCrawlSeqFileWriter.log.isDebugEnabled()) {
							CommonCrawlSeqFileWriter.log.debug("GOT Item URL:" + item.getUri() + " StreamPos:" + item.getArcFilePos()
									+ " Content Length:" + item.getContent().getCount());
						}

						BehemothDocument bDoc = new BehemothDocument();
						bDoc.setContent(item.getContent().getReadOnlyBytes());
						bDoc.setContentType(item.getMimeType());
						bDoc.setUrl(item.getUri());
						writer.append(new Text(item.getUri()), bDoc);

						if ((count % config.syncCount == 0) && (count != 0)) {
							log.info("sync count: {}", Integer.valueOf(count));
							writer.syncFs();
						}
						count++;
					}
					CommonCrawlSeqFileWriter.log.info("NO MORE ITEMS... BYE");
				} catch (IOException e) {
					CommonCrawlSeqFileWriter.log.error(StringUtils.stringifyException(e));
				}
			}
		});
		thread.start();
		ReadableByteChannel channel = Channels.newChannel(is);
		try {
			int totalBytesRead = 0;
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(config.bufferSize);

				int bytesRead = channel.read(buffer);

				if (bytesRead == -1) {
					reader.finished();
					break;
				}
				buffer.flip();
				totalBytesRead += buffer.remaining();
				reader.available(buffer);
			}
		} finally {
			channel.close();
		}
	}
}
