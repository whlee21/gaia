package gaia.solr.click;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.ParallelAtomicReader;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickIndexReader extends DirectoryReader {
	private static final Logger LOG = LoggerFactory.getLogger(ClickIndexReader.class);
	private DirectoryReader main;
	private AtomicReader[] mainReaders;
	private AtomicReader[] parallelReaders;
	private AtomicReader[] clickReaders;
	private ExtDirectory dir;
	private String boostData;
	private long resourcesLastModified;
	private ClickIndexReaderFactory factory;
	// private boolean readOnly;
	private long version;
	int numBoosts;
	float maxBoost;
	float totalBoost;
	float totalPosBoost;
	float totalTimeBoost;
	int boostedDocs;
	private Map<String, File> resources;
	private File[] boosts;
	private File clickDir;

	public ClickIndexReader(ClickIndexReaderFactory factory, DirectoryReader main, AtomicReader[] clickReaders,
			AtomicReader[] parallelReaders, String boostData, File clickDir, Map<String, File> resources) throws IOException {
		super(main.directory(), parallelReaders);
		assert (assertSaneReaders(parallelReaders));

		this.factory = factory;
		this.main = main;
		this.parallelReaders = parallelReaders;
		this.clickReaders = clickReaders;

		mainReaders = getSequentialSubReaders(main);
		resourcesLastModified = main.getVersion();
		if (resources != null) {
			this.resources = resources;
			for (File f : resources.values()) {
				if (f.lastModified() > resourcesLastModified) {
					resourcesLastModified = f.lastModified();
				}
			}
		}
		version = resourcesLastModified;
		this.boostData = boostData;
		dir = new ExtDirectory(main.directory(), resources);
		this.clickDir = clickDir;
	}

	private boolean assertSaneReaders(AtomicReader[] readers) {
		for (int i = 0; i < readers.length; i++) {
			assert (readers[i] != null) : ("null subreader " + i);
			assert (readers[i].getRefCount() > 0) : ("0 refcount for subreader " + i);
		}
		return true;
	}

	static AtomicReader[] getSequentialSubReaders(IndexReader r) {
		List<AtomicReaderContext> leaves = r.leaves();
		AtomicReader[] subReaders = new AtomicReader[leaves.size()];
		for (int i = 0; i < leaves.size(); i++) {
			subReaders[i] = ((AtomicReaderContext) leaves.get(i)).reader();
		}
		return subReaders;
	}

	private long checkVersion() {
		boosts = factory.getBoostDataResources(main, boostData);
		if ((boosts == null) && (resources != null)) {
			resources = null;
			version = (resourcesLastModified + 1L);
			return version;
		}
		if (boosts != null) {
			if (resources == null) {
				resourcesLastModified = 0L;
				resources = new HashMap<String, File>();
			} else {
				resources.clear();
			}
			for (File f : boosts) {
				resources.put(f.getName(), f);
			}
		}
		long modified = 0L;
		if (resources != null) {
			for (File f : resources.values()) {
				if (f.lastModified() > modified) {
					modified = f.lastModified();
				}
			}
			if (modified > version)
				version = modified;
		} else {
			if (main.getRefCount() > 0)
				version = main.getVersion();
			else {
				version = 0L;
			}
			modified = version;
		}
		return modified;
	}

	public IndexReader getMain() {
		return main;
	}

	protected synchronized void doClose() throws IOException {
		if (main.getRefCount() > 0) {
			try {
				main.decRef();
			} catch (AlreadyClosedException ace) {
				System.err.println(main);
				for (AtomicReader r : getSequentialSubReaders(main)) {
					System.err.println(" - " + r + " refCnt=" + r.getRefCount());
				}
				System.err.flush();
			}
		}
		parallelReaders = null;
		if (clickDir != null)
			factory.safeDelete(clickDir);
	}

	public Directory getExtDirectory() {
		return dir;
	}

	public long getVersion() {
		checkVersion();
		return version;
	}

	public DirectoryReader doOpenIfChanged() throws CorruptIndexException, IOException {
		return doOpenIfChanged(null);
	}

	protected DirectoryReader doOpenIfChanged(IndexCommit commit) throws CorruptIndexException, IOException {
		return openIfChangedInternal(null, null, false);
	}

	protected DirectoryReader doOpenIfChanged(IndexWriter writer, boolean applyAllDeletes) throws CorruptIndexException,
			IOException {
		return openIfChangedInternal(null, writer, applyAllDeletes);
	}

	private synchronized DirectoryReader openIfChangedInternal(IndexCommit commit, IndexWriter writer,
			boolean applyAllDeletes) throws CorruptIndexException, IOException {
		boolean rebuild = false;
		long modifiedTime = checkVersion();

		boolean modified = modifiedTime > resourcesLastModified;
		DirectoryReader newMain;
		if (commit != null) {
			newMain = DirectoryReader.openIfChanged(main, commit);
		} else {
			if (writer != null)
				newMain = DirectoryReader.openIfChanged(main, writer, applyAllDeletes);
			else
				newMain = DirectoryReader.openIfChanged(main);
		}
		if (newMain != null) {
			rebuild = true;
		} else
			newMain = main;

		if ((modified) || (rebuild)) {
			AtomicReader[] parallelReaders = new AtomicReader[this.parallelReaders.length];
			System.arraycopy(parallelReaders, 0, parallelReaders, 0, parallelReaders.length);

			if (!rebuild)
				main.incRef();
			if ((rebuild) && (!modified)) {
				if (clickReaders == null) {
					LOG.info(" - NRT reopen, missing click data, single main index");
					return new ClickIndexReader(factory, newMain, null, getSequentialSubReaders(newMain), boostData, clickDir,
							resources);
				}

				AtomicReader[] newReaders = getSequentialSubReaders(newMain);
				if (writer != null) {
					Map<String, AtomicReader> newReadersMap = new HashMap<String, AtomicReader>();
					for (AtomicReader ar : newReaders) {
						newReadersMap.put(ar.getCombinedCoreAndDeletesKey().toString(), ar);
					}
					for (int i = 0; i < mainReaders.length; i++) {
						if (!newReadersMap.containsKey(mainReaders[i].getCombinedCoreAndDeletesKey().toString())) {
							parallelReaders[i] = null;
						} else {
							AtomicReader newReader = (AtomicReader) newReadersMap.remove(mainReaders[i]
									.getCombinedCoreAndDeletesKey().toString());

							if ((clickReaders != null) && (i < clickReaders.length)) {
								parallelReaders[i] = new ParallelAtomicReader(new AtomicReader[] { newReader, clickReaders[i] });
							} else
								parallelReaders[i] = newReader;
						}
					}

					if (newReadersMap.size() <= 1) {
						List<AtomicReader> newParallelReaders = new ArrayList<AtomicReader>(newReaders.length);
						for (int i = 0; i < parallelReaders.length; i++) {
							if (parallelReaders[i] != null) {
								newParallelReaders.add(parallelReaders[i]);
							}

						}

						newParallelReaders.addAll(newReadersMap.values());
						ClickIndexReader newCIR = new ClickIndexReader(factory, newMain, clickReaders,
								(AtomicReader[]) newParallelReaders.toArray(new AtomicReader[0]), boostData, clickDir, resources);

						LOG.info("Opened new NRT ClickIndexReader");
						return newCIR;
					}
				}
			}

			LOG.info(" - full rebuild from reopen()");
			return factory.reopen(newMain, rebuild);
		}
		LOG.info(" - returning old from reopen()");
		return null;
	}

	public boolean isCurrent() throws CorruptIndexException, IOException {
		return main.isCurrent();
	}

	public IndexCommit getIndexCommit() throws IOException {
		if (resources == null) {
			return main.getIndexCommit();
		}
		return new ClickIndexCommit(main.getIndexCommit(), resources);
	}

	private static class ExtIndexInput extends BufferedIndexInput {
		protected final Descriptor file;
		boolean isClone;
		protected final int chunkSize;

		public ExtIndexInput(File path) throws IOException {
			super("extIndexInput", 1024);
			file = new Descriptor(path, "r");
			chunkSize = 8192;
		}

		protected void readInternal(byte[] b, int offset, int len) throws IOException {
			synchronized (file) {
				long position = getFilePointer();
				if (position != file.position) {
					file.seek(position);
					file.position = position;
				}
				int total = 0;
				try {
					do {
						int readLength;
						if (total + chunkSize > len) {
							readLength = len - total;
						} else {
							readLength = chunkSize;
						}
						int i = file.read(b, offset + total, readLength);
						if (i == -1) {
							throw new IOException("read past EOF");
						}
						file.position += i;
						total += i;
					} while (total < len);
				} catch (OutOfMemoryError e) {
					OutOfMemoryError outOfMemoryError = new OutOfMemoryError(
							"OutOfMemoryError likely caused by the Sun VM Bug described in https://issues.apache.org/jira/browse/LUCENE-1566; try calling FSDirectory.setReadChunkSize with a a value smaller than the current chunks size ("
									+ chunkSize + ")");

					outOfMemoryError.initCause(e);
					throw outOfMemoryError;
				}
			}
		}

		protected void seekInternal(long pos) throws IOException {
		}

		public void close() throws IOException {
			if (!isClone)
				file.close();
		}

		public long length() {
			return file.length;
		}

		public BufferedIndexInput clone() {
			ExtIndexInput clone = (ExtIndexInput) super.clone();
			clone.isClone = true;
			return clone;
		}

		protected static class Descriptor extends RandomAccessFile {
			protected volatile boolean isOpen;
			long position;
			final long length;

			public Descriptor(File file, String mode) throws IOException {
				super(file, mode);
				isOpen = true;
				length = length();
			}

			public void close() throws IOException {
				if (isOpen) {
					isOpen = false;
					super.close();
				}
			}
		}
	}

	private static class ExtDirectory extends Directory {
		private Directory main;
		private Map<String, File> resources;

		public ExtDirectory(Directory main, Map<String, File> resources) {
			this.main = main;
			this.resources = resources;
		}

		public Directory getMainDirectory() {
			return main;
		}

		public void close() throws IOException {
			main.close();
		}

		public IndexOutput createOutput(String name, IOContext context) throws IOException {
			if ((resources != null) && (resources.containsKey(name))) {
				throw new IOException("Already exists.");
			}
			return main.createOutput(name, context);
		}

		public void deleteFile(String name) throws IOException {
			if ((resources != null) && (resources.containsKey(name))) {
				((File) resources.get(name)).delete();
				resources.remove(name);
				return;
			}
			main.deleteFile(name);
		}

		public boolean fileExists(String name) throws IOException {
			if ((resources != null) && (resources.containsKey(name))) {
				return ((File) resources.get(name)).exists();
			}
			return main.fileExists(name);
		}

		public long fileLength(String name) throws IOException {
			if ((resources != null) && (resources.containsKey(name))) {
				return ((File) resources.get(name)).length();
			}
			return main.fileLength(name);
		}

		public String[] listAll() throws IOException {
			if (resources == null) {
				return main.listAll();
			}
			String[] names = main.listAll();
			HashSet<String> all = new HashSet<String>(Arrays.asList(names));
			all.addAll(resources.keySet());
			return all.toArray(new String[all.size()]);
		}

		public IndexInput openInput(String name, IOContext context) throws IOException {
			if ((resources != null) && (resources.containsKey(name))) {
				return new ClickIndexReader.ExtIndexInput((File) resources.get(name));
			}
			return main.openInput(name, context);
		}

		public String toString() {
			return new StringBuilder().append(super.toString()).append("(main=").append(main).append(",resources=")
					.append(resources != null ? resources.keySet() : "none").append(")").toString();
		}

		public void sync(Collection<String> names) throws IOException {
			main.sync(names);
		}

		@Override
		public void clearLock(String name) throws IOException {
			main.clearLock(name);
		}

		@Override
		public LockFactory getLockFactory() {
			return main.getLockFactory();
		}

		@Override
		public Lock makeLock(String name) {
			return main.makeLock(name);
		}

		@Override
		public void setLockFactory(LockFactory lockFactory) throws IOException {
			main.setLockFactory(lockFactory);
		}
	}

	private static class ClickIndexCommit extends IndexCommit {
		IndexCommit in;
		Map<String, File> resources;

		ClickIndexCommit(IndexCommit in, Map<String, File> resources) {
			this.in = in;
			this.resources = resources;
		}

		public void delete() {
			in.delete();
		}

		public Directory getDirectory() {
			return in.getDirectory();
		}

		public Collection<String> getFileNames() throws IOException {
			if (resources == null) {
				return in.getFileNames();
			}
			Collection<String> names = in.getFileNames();
			ArrayList<String> list = new ArrayList<String>(names.size() + resources.size());

			list.addAll(names);
			list.addAll(resources.keySet());
			return list;
		}

		public long getGeneration() {
			return in.getGeneration();
		}

		public String getSegmentsFileName() {
			return in.getSegmentsFileName();
		}

		public Map<String, String> getUserData() throws IOException {
			return in.getUserData();
		}

		public boolean isDeleted() {
			return in.isDeleted();
		}

		public int getSegmentCount() {
			return in.getSegmentCount();
		}
	}
}
