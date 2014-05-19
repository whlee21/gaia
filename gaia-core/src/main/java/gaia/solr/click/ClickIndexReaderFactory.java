package gaia.solr.click;

import gaia.api.ClickAnalysisResource;
import gaia.solr.click.log.TermFreq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.CompositeReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.ParallelCompositeReader;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.IndexReaderFactory;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.StandardIndexReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickIndexReaderFactory extends IndexReaderFactory {
	private static final Logger LOG = LoggerFactory.getLogger(ClickIndexReaderFactory.class);
	StandardIndexReaderFactory standardFactory;
	String clickIndexLocation = "click-index";
	String boostData;
	String boostField;
	String docIdField;
	File clickIndex;
	boolean enabled;
	Mode mode = Mode.max;
	float multiplier;

	public void init(NamedList args) {
		super.init(args);
		docIdField = ((String) args.get("docIdField"));
		boostData = ((String) args.get("boostData"));
		boostField = ((String) args.get("boostField"));
		enabled = ((Boolean) args.get("enabled")).booleanValue();

		String modeString = (String) args.get("mode");
		if (modeString != null)
			try {
				Mode m = Mode.valueOf(modeString);
				mode = m;
			} catch (Exception e) {
			}
		if (mode == Mode.total)
			multiplier = 100.0F;
		else {
			multiplier = 1.0F;
		}
		standardFactory = new StandardIndexReaderFactory();
		standardFactory.init(args);
	}

	public void init(String docIdField, String boostData, String boostField, Mode mode) {
		this.docIdField = docIdField;
		this.boostData = boostData;
		this.boostField = boostField;
		enabled = true;
		standardFactory = new StandardIndexReaderFactory();
		this.mode = mode;
		if (mode == Mode.total)
			multiplier = 100.0F;
		else
			multiplier = 1.0F;
	}

	@Override
	public DirectoryReader newReader(Directory indexDir, SolrCore core) throws IOException {
		DirectoryReader main = standardFactory.newReader(indexDir, core);
		if (!enabled) {
			LOG.info(new StringBuilder().append("Click not enabled, ").append(core.getIndexDir()).toString());

			return main;
		}
		DirectoryReader parallel = buildParallelReader(main, boostData, true);
		return parallel;
	}

	@Override
	public DirectoryReader newReader(IndexWriter indexWriter, SolrCore core) throws IOException {
		DirectoryReader main = standardFactory.newReader(indexWriter, core);
		if (!enabled) {
			LOG.info(new StringBuilder().append("Click not enabled, ").append(core.getIndexDir()).toString());

			return main;
		}
		DirectoryReader parallel = buildParallelReader(main, boostData, true);
		return parallel;
	}

	DirectoryReader reopen(DirectoryReader newMain, boolean rebuild) throws IOException {
		DirectoryReader parallel = buildParallelReader(newMain, boostData, rebuild);
		return parallel;
	}

	File[] getBoostDataResources(DirectoryReader main, String boostData) {
		String dataInIndex = null;
		String dataInBoostData = null;

		File indexDir = null;
		try {
			Directory d = main.directory();
			if ((d instanceof NRTCachingDirectory)) {
				d = ((NRTCachingDirectory) d).getDelegate();
			}
			if ((d instanceof FSDirectory)) {
				dataInIndex = ((FSDirectory) d).getDirectory().getPath();
				LOG.debug(new StringBuilder().append("-checking dataInIndex=").append(dataInIndex).toString());
				indexDir = new File(dataInIndex);
				File[] files = indexDir.listFiles(BoostDataFileFilter.INSTANCE);
				if ((files == null) || (files.length == 0)) {
					dataInIndex = null;
					LOG.debug("--no files in index");
				}
			} else {
				LOG.debug(new StringBuilder().append("-can't check dataInIndex, directory is ").append(d).toString());
			}
		} catch (Exception e) {
			LOG.warn("can't use main Directory as boostData", e);
			dataInIndex = null;
		}

		String collection = null;
		if (boostData != null) {
			try {
				Directory d = main.directory();
				if ((d instanceof NRTCachingDirectory)) {
					d = ((NRTCachingDirectory) d).getDelegate();
				}
				if ((d instanceof FSDirectory)) {
					indexDir = new File(((FSDirectory) d).getDirectory().getPath());

					String dataDir = indexDir.getParentFile().getAbsolutePath();

					collection = indexDir.getParentFile().getParentFile().getName();
					int idx = collection.lastIndexOf(95);
					if (idx != -1) {
						collection = collection.substring(0, idx);
					}
					dataInBoostData = new StringBuilder().append(dataDir).append(File.separator).append(boostData)
							.append(File.separator).append("current").toString();
					File dibd = new File(dataInBoostData);
					LOG.debug(new StringBuilder().append("-checking dataInBoostData=").append(dibd.getAbsolutePath()).toString());
					if (!dibd.exists()) {
						dataInBoostData = boostData;
						LOG.debug("--not found");
					} else if (dibd.isDirectory()) {
						LOG.debug("--is directory, picking the last one");
						File[] files = dibd.listFiles();
						if ((files != null) && (files.length > 0)) {
							Arrays.sort(files);
							dataInBoostData = files[(files.length - 1)].getAbsolutePath();
							LOG.debug(new StringBuilder().append("--picked ").append(dataInBoostData).toString());
						}
					} else {
						LOG.debug(new StringBuilder().append("--dibd is a file ").append(dibd.getAbsolutePath()).toString());
					}
				} else {
					LOG.debug(new StringBuilder().append("-can't check dataInBoostData, directory is ").append(d).toString());
				}
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("can't use data/").append(boostData).toString(), e);
			}
		}
		String data = null;

		if ((dataInBoostData != null) && (new File(dataInBoostData).exists()))
			data = dataInBoostData;
		else if (dataInIndex != null) {
			data = dataInIndex;
		}
		if (data == null) {
			if (collection != null) {
				LOG.debug("-checking data/collection location");
				File clickData = new File(new StringBuilder().append(ClickAnalysisResource.DEFAULT_DATA_PATH)
						.append(File.separator).append("boost").append(File.separator).append("current").toString());

				LOG.debug(new StringBuilder().append("-checking data/collection location in ")
						.append(clickData.getAbsolutePath()).toString());
				if ((clickData.exists()) && (clickData.isDirectory())) {
					File[] files = clickData.listFiles();
					if (files.length > 0) {
						Arrays.sort(files);
						File f = files[(files.length - 1)];
						if (f.isDirectory()) {
							data = f.getAbsolutePath();
							LOG.debug(new StringBuilder().append("--found dir ").append(data).toString());
						} else {
							LOG.debug("--no subdirs");
						}
					} else {
						LOG.debug("--empty");
					}
				}
			} else {
				LOG.debug("--collection is null, and data is null");
			}
		}
		if (data == null) {
			return null;
		}
		File dataFile = new File(data);
		if (!dataFile.exists()) {
			return null;
		}
		if (dataFile.isFile()) {
			LOG.debug(new StringBuilder().append("--return single file ").append(dataFile.getAbsolutePath()).toString());
			return new File[] { dataFile };
		}
		File[] boosts = dataFile.listFiles(BoostDataFileFilter.INSTANCE);
		LOG.debug(new StringBuilder().append("-listing files in ").append(dataFile.getAbsolutePath()).toString());
		if ((boosts == null) || (boosts.length == 0)) {
			LOG.debug("--no files found");
			return null;
		}
		LOG.debug(new StringBuilder().append("--return files ").append(Arrays.toString(boosts)).toString());
		return boosts;
	}

	DirectoryReader buildParallelReader(DirectoryReader main, String boostData, boolean rebuild) {
		try {
			File[] boosts = getBoostDataResources(main, boostData);
			if (boosts == null) {
				LOG.warn(new StringBuilder().append("Missing boost data in ").append(boostData)
						.append(", proceeding with single main index ").append(main).toString());
				try {
					return new ClickIndexReader(this, main, null, ClickIndexReader.getSequentialSubReaders(main), boostData,
							null, null);
				} catch (Exception e) {
					LOG.warn("Unexpected exception, returning single main index", e);
					return main;
				}
			}
			LOG.info(new StringBuilder().append("boostData in ")
					.append(boosts.length == 1 ? boosts[0].getAbsolutePath() : boosts[0].getParentFile().getAbsolutePath())
					.toString());

			Directory d = main.directory();
			File primaryDir = null;
			if ((d instanceof FSDirectory)) {
				String path = ((FSDirectory) d).getDirectory().getPath();
				primaryDir = new File(path);
				clickIndex = new File(primaryDir.getParentFile(), clickIndexLocation);
			} else {
				String secondaryPath = new StringBuilder().append(System.getProperty("java.io.tmpdir")).append(File.separator)
						.append(clickIndexLocation).append("-").append(System.currentTimeMillis()).toString();

				clickIndex = new File(secondaryPath);
			}

			if (primaryDir != null) {
				Map<String, File> indexFiles = new HashMap<String, File>();
				File[] list = primaryDir.listFiles();
				for (File f : list) {
					indexFiles.put(f.getName(), f);
				}
				File[] newBoosts = new File[boosts.length];
				Set<String> copied = new HashSet<String>();
				for (int i = 0; i < boosts.length; i++) {
					File boost = boosts[i];
					File target = (File) indexFiles.get(boost.getName());
					if ((target == null) || (target.lastModified() != boost.lastModified())) {
						target = new File(primaryDir, boost.getName());
						FileUtils.copyFile(boost, target);
						copied.add(boost.getName());
					}
					newBoosts[i] = target;
				}
				if (copied.size() > 0) {
					LOG.info(new StringBuilder().append("Copied ").append(copied.size())
							.append(" boost files to index dir for replication: ").append(copied).toString());
				}
				boosts = newBoosts;
			}

			File secondaryIndex = new File(clickIndex, new StringBuilder().append(System.currentTimeMillis())
					.append("-index").toString());
			File secondaryLookup = new File(clickIndex, new StringBuilder().append(System.currentTimeMillis())
					.append("-lookup").toString());
			DirectoryReader lookup = null;
			if ((!secondaryLookup.exists()) || (rebuild)) {
				safeDelete(clickIndex);

				buildLookupIndex(boosts, secondaryLookup);
			}
			Directory lookupDir = FSDirectory.open(secondaryLookup);
			lookup = DirectoryReader.open(lookupDir);
			IndexInput ii = lookupDir.openInput("boost", IOContext.DEFAULT);
			float maxBoost = Float.parseFloat(ii.readString());
			if (maxBoost == 0.0F)
				maxBoost = 1.0F;
			float totalBoost = Float.parseFloat(ii.readString());
			if (totalBoost == 0.0F)
				totalBoost = 1.0F;
			float totalPosBoost = Float.parseFloat(ii.readString());
			if (totalPosBoost == 0.0F)
				totalPosBoost = 1.0F;
			float totalTimeBoost = Float.parseFloat(ii.readString());
			if (totalTimeBoost == 0.0F)
				totalTimeBoost = 1.0F;
			int numBoosts = ii.readInt();
			if (numBoosts == 0)
				numBoosts = 1;
			ii.close();
			LOG.debug("building a new index");
			Directory dir = FSDirectory.open(secondaryIndex);
			if (IndexWriter.isLocked(dir)) {
				try {
					IndexWriter.unlock(dir);
				} catch (Exception e) {
					LOG.warn(new StringBuilder().append("Failed to unlock ").append(secondaryIndex).toString());
				}
			}

			AtomicReader[] subReaders = ClickIndexReader.getSequentialSubReaders(main);
			int[] mergeTargets;
			if ((subReaders == null) || (subReaders.length == 0)) {
				mergeTargets = new int[] { main.maxDoc() };
			} else {
				mergeTargets = new int[subReaders.length];
				for (int i = 0; i < subReaders.length; i++) {
					mergeTargets[i] = subReaders[i].maxDoc();
				}
			}
			IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_45, new WhitespaceAnalyzer(Version.LUCENE_45));

			cfg.setMergeScheduler(new SerialMergeScheduler());
			cfg.setMergePolicy(new ClickMergePolicy(mergeTargets, false));
			IndexWriter iw = new IndexWriter(dir, cfg);
			LOG.info(new StringBuilder().append("processing ").append(main.maxDoc()).append(" documents in main index")
					.toString());
			int boostedDocs = 0;
			Bits live = MultiFields.getLiveDocs(main);

			FieldType boostValType = new FieldType();
			boostValType.setIndexed(true);
			boostValType.setTokenized(false);
			boostValType.setStored(true);
			boostValType.freeze();

			FieldType boost1Type = new FieldType();
			boost1Type.setIndexed(true);
			boost1Type.setTokenized(false);
			boost1Type.setStored(false);
			boost1Type.freeze();
			int targetPos = 0;
			int nextTarget = mergeTargets[targetPos];
			for (int i = 0; i < main.maxDoc(); i++) {
				if (i == nextTarget) {
					iw.commit();
					nextTarget += mergeTargets[(++targetPos)];
				}
				if ((live != null) && (!live.get(i))) {
					addDummy(iw);
				} else {
					DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(new String[] { docIdField });
					main.document(i, visitor);
					Document doc = visitor.getDocument();

					String id = doc.get(docIdField);
					if (id == null) {
						LOG.debug(new StringBuilder().append("missing id, docNo=").append(i).toString());
						addDummy(iw);
					} else {
						Boost b = lookup(lookup, id, maxBoost, totalBoost, numBoosts, mode);
						if (b == null) {
							LOG.debug(new StringBuilder().append("missing boost data, docId=").append(id).toString());
							addDummy(iw);
						} else {
							LOG.debug(new StringBuilder().append("adding boost data, docId=").append(id).append(", b=").append(b)
									.toString());
							doc = new Document();

							for (TermFreq tf : b.topTerms) {
								Field ff = new Field(new StringBuilder().append(boostField).append("_terms").toString(), tf.term,
										TextField.TYPE_STORED);
								if (b.getCombinedBoost() > 0.0F) {
									ff.setBoost(b.getCombinedBoost());
								}

								doc.add(ff);
							}
							Field f = new Field(new StringBuilder().append(boostField).append("_val").toString(), String.valueOf(b
									.getCombinedBoost()), boostValType);
							if (b.getCombinedBoost() > 0.0F) {
								f.setBoost(b.getCombinedBoost());
							}

							doc.add(f);
							f = new Field(boostField, "1", boost1Type);
							if (b.getCombinedBoost() > 0.0F) {
								f.setBoost(b.getCombinedBoost());
							}

							doc.add(f);
							iw.addDocument(doc);
							boostedDocs++;
						}
					}
				}
			}
			iw.close();
			lookup.close();
			safeDelete(secondaryLookup);
			DirectoryReader other = DirectoryReader.open(dir);
			LOG.info(new StringBuilder().append("ClickIndexReader with ").append(boostedDocs).append(" boosted documents.")
					.toString());

			Map<String, File> resources = new HashMap<String, File>();
			for (File f : boosts) {
				resources.put(f.getName(), f);
			}
			ClickIndexReader pr = createClickIndexReader(main, other, boostData, resources, secondaryIndex);

			pr.maxBoost = maxBoost;
			pr.totalBoost = totalBoost;
			pr.totalPosBoost = totalPosBoost;
			pr.totalTimeBoost = totalTimeBoost;
			pr.numBoosts = numBoosts;
			pr.boostedDocs = boostedDocs;
			return pr;
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Unable to build parallel index: ").append(e.toString()).toString(), e);
			LOG.warn("Proceeding with single main index.");
			try {
				return new ClickIndexReader(this, main, null, ClickIndexReader.getSequentialSubReaders(main), boostData, null,
						null);
			} catch (Exception e1) {
				LOG.warn("Unexpected exception, returning single main index", e1);
			}
		}
		return main;
	}

	private ClickIndexReader createClickIndexReader(DirectoryReader main, DirectoryReader click, String boostData,
			Map<String, File> resources, File secondaryIndex) throws IOException {
		ParallelCompositeReader parallel = new ParallelCompositeReader(false, new CompositeReader[] { main, click });
		AtomicReader[] parReaders = ClickIndexReader.getSequentialSubReaders(parallel);
		AtomicReader[] readers = Arrays.<AtomicReader, AtomicReader> copyOf(parReaders, parReaders.length,
				AtomicReader[].class);
		for (AtomicReader reader : readers) {
			reader.incRef();
		}
		parallel.close();
		ClickIndexReader pr = new ClickIndexReader(this, main, ClickIndexReader.getSequentialSubReaders(click), readers,
				boostData, secondaryIndex, resources);

		return pr;
	}

	private void addDummy(IndexWriter iw) throws IOException {
		Document dummy = new Document();
		Field f = new Field(new StringBuilder().append("_").append(boostField).toString(), "d", StringField.TYPE_NOT_STORED);
		dummy.add(f);
		iw.addDocument(dummy);
	}

	private void buildLookupIndex(File[] boosts, File secondaryIndex) throws Exception {
		if (secondaryIndex.exists()) {
			for (File f : secondaryIndex.listFiles())
				safeDelete(f);
		} else {
			secondaryIndex.mkdirs();
		}
		Directory ldir = FSDirectory.open(secondaryIndex);
		IndexWriterConfig cfgSec = new IndexWriterConfig(Version.LUCENE_45, new WhitespaceAnalyzer(Version.LUCENE_45));

		IndexWriter iw = new IndexWriter(ldir, cfgSec);
		double totalBoost = 0.0D;
		double totalPosBoost = 0.0D;
		double totalTimeBoost = 0.0D;
		float maxBoost = 0.0F;
		int numBoosts = 0;
		FieldType storedOnly = new FieldType();
		storedOnly.setIndexed(false);
		storedOnly.setStored(true);
		storedOnly.freeze();
		for (File f : boosts) {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				if ((line.charAt(0) != '#') && (line.trim().length() != 0)) {
					String[] fields = line.split("\t");
					if (!fields[0].equals("*")) {
						float posBoost = Float.parseFloat(fields[1]);
						float timeBoost = Float.parseFloat(fields[2]);
						float boost = posBoost + timeBoost;
						if (boost > maxBoost) {
							maxBoost = boost;
						}
						totalBoost += boost;
						totalPosBoost += posBoost;
						totalTimeBoost += timeBoost;
						numBoosts++;

						Document doc = new Document();
						doc.add(new Field("id", fields[0], StringField.TYPE_NOT_STORED));

						doc.add(new Field("boost", String.valueOf(boost), storedOnly));
						if ((fields.length > 3) && (fields[3].length() > 0)) {
							doc.add(new Field("terms", fields[3], storedOnly));
						}
						if ((fields.length > 4) && (fields[4].length() > 0)) {
							doc.add(new Field("history", fields[4], storedOnly));
						}
						iw.addDocument(doc);
						LOG.debug(new StringBuilder().append("-add lookup for id=").append(fields[0]).toString());
					}
				}
			}
			br.close();
		}
		iw.forceMerge(1);
		iw.close();
		IndexOutput bo = ldir.createOutput("boost", IOContext.DEFAULT);
		bo.writeString(new StringBuilder().append(maxBoost).append("").toString());
		bo.writeString(new StringBuilder().append(totalBoost).append("").toString());
		bo.writeString(new StringBuilder().append(totalPosBoost).append("").toString());
		bo.writeString(new StringBuilder().append(totalTimeBoost).append("").toString());
		bo.writeInt(numBoosts);
		bo.flush();
		bo.close();
	}

	private Boost lookup(IndexReader lookup, String id, float maxBoost, float totalBoost, int numDocs, Mode mode)
			throws IOException {
		BytesRef idRef = new BytesRef(id);
		if (lookup.docFreq(new Term("id", id)) == 0) {
			return null;
		}

		DocsEnum docs = MultiFields.getTermDocsEnum(lookup, MultiFields.getLiveDocs(lookup), "id", idRef, 0);

		if (docs.nextDoc() == Integer.MAX_VALUE) {
			return null;
		}
		Document doc = lookup.document(docs.docID());
		float boost = Float.parseFloat(doc.get("boost"));

		switch (mode) {
		case max:
			boost = multiplier * boost / maxBoost;
			break;
		case total:
			boost = multiplier * boost / totalBoost;
			break;
		case none:
		}

		String termsString = doc.get("terms");
		TermFreq[] topTerms = null;
		if (termsString != null) {
			String[] termFreqs = termsString.split(",");
			topTerms = new TermFreq[termFreqs.length];
			for (int i = 0; i < termFreqs.length; i++) {
				String[] termFreq = termFreqs[i].split(":");
				TermFreq tf = new TermFreq(termFreq[0], Float.parseFloat(termFreq[1]), Float.parseFloat(termFreq[2]));
				topTerms[i] = tf;
			}
		}
		Boost b = new Boost(topTerms, boost, 0.0F);
		return b;
	}

	boolean safeDelete(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			boolean res = true;
			for (File f1 : files) {
				if (!safeDelete(f1)) {
					res = false;
					f1.deleteOnExit();
				}
			}
			if (!f.delete()) {
				f.deleteOnExit();
				res = false;
			}
			return res;
		}
		try {
			boolean res = f.delete();
			if (!res) {
				f.deleteOnExit();
			}

			return res;
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Can't delete old click indexes: ").append(e.getMessage()).toString());
		}
		return false;
	}

	public String getBoostData() {
		return boostData;
	}

	public String getBoostField() {
		return boostField;
	}

	public String getDocIdField() {
		return docIdField;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public static enum Mode {
		max,

		total,

		none;
	}

}
