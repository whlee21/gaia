package gaia.crawl.aperture;

import gaia.Constants;
import gaia.crawl.datasource.DataSourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.openrdf.rdf2go.RepositoryModel;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.SailException;
import org.openrdf.sail.nativerdf.NativeStore;
import org.semanticdesktop.aperture.accessor.base.ModelAccessData;
import org.semanticdesktop.aperture.datasource.filesystem.FileSystemDataSource;
import org.semanticdesktop.aperture.datasource.web.WebDataSource;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class ApertureRepos {
	private static final Logger LOG = LoggerFactory.getLogger(ApertureRepos.class);

	private final Map<File, Repository> repos = new HashMap<File, Repository>();
	private final Map<File, Integer> repoRefCounts = new HashMap<File, Integer>();
	private final Map<File, Boolean> firstInits = new HashMap<File, Boolean>();

	public Repository initRepo(gaia.crawl.datasource.DataSource ds) throws Exception {
		File linkStore = getLinkStoreDir(ds.getCollection());

		LOG.info("link.store: " + linkStore + " linkStore.path: " + linkStore.getCanonicalPath());

		Repository repo = null;
		synchronized (repos) {
			if (firstInits.get(linkStore) == null) {
				firstInits.put(linkStore, Boolean.valueOf(true));
				File lockFolder = new File(linkStore, "lock");
				if (lockFolder.exists()) {
					try {
						LOG.warn("Attempting to remove existing lock:" + lockFolder);
						FileUtils.deleteDirectory(lockFolder);
					} catch (IOException e) {
						LOG.error("Exception", e);
					}
				}
			}

			repo = (Repository) repos.get(linkStore);
			if (repo == null) {
				NativeStore store = new NativeStore(linkStore);
				repo = new SailRepository(store);
				try {
					repo.initialize();
				} catch (RepositoryException e) {
					try {
						store.shutDown();
					} catch (SailException e2) {
						LOG.error("Exception", e2);
					}
					LOG.error("Exception", e);
					throw e;
				}
				repos.put(linkStore, repo);
				repoRefCounts.put(linkStore, Integer.valueOf(1));
			} else {
				Integer refCnt = (Integer) repoRefCounts.get(linkStore);
				repoRefCounts.put(linkStore, refCnt = Integer.valueOf(refCnt.intValue() + 1));
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Link Store: " + linkStore);
		}

		return repo;
	}

	public synchronized void close(Repository repo, gaia.crawl.datasource.DataSource ds) throws Exception {
		File linkStore = getLinkStoreDir(ds.getCollection());

		LOG.debug("repoStore: " + linkStore);
		if ((linkStore != null) && (linkStore.exists())) {
			Integer refCnt = (Integer) repoRefCounts.get(linkStore);
			if (refCnt != null) {
				refCnt = Integer.valueOf(refCnt.intValue() - 1);
			}
			LOG.info("refCnt: " + refCnt);
			if ((refCnt == null) || (refCnt.intValue() == 0)) {
				if (refCnt != null) {
					try {
						LOG.info("Shutting down Aperture repo@" + Integer.toHexString(repo.hashCode()) + ": " + linkStore);
						repo.shutDown();

						File lockFolder = new File(linkStore, "lock");
						lockFolder = new File(linkStore, "lock");
						if (lockFolder.exists())
							try {
								LOG.warn("Attempting to remove existing lock:" + lockFolder);
								FileUtils.deleteDirectory(lockFolder);
							} catch (IOException e) {
								LOG.error("Exception", e);
							}
					} catch (RepositoryException e) {
						LOG.error("Exception", e);
					}
					repos.remove(linkStore);
					repoRefCounts.remove(linkStore);
				} else {
					LOG.info("Already closed and removed");
				}
			} else {
				LOG.info("Not shutting down Aperture repo@ " + Integer.toHexString(repo.hashCode()) + ", repoStore: "
						+ linkStore + " yet since refCnt " + refCnt + " > 0");

				repoRefCounts.put(linkStore, refCnt);
			}
		} else {
			try {
				repo.shutDown();
			} catch (RepositoryException e) {
				LOG.error("Exception", e);
			}
		}
	}

	public synchronized void clearRepository(String collection) throws IOException {
		LOG.info("Clearing Aperture repo for collection '" + collection + "' ...");
		File linkStore = getLinkStoreDir(collection);
		Repository repo = (Repository) repos.get(linkStore);
		if (repo == null) {
			LOG.info("Repo ID not in use: " + linkStore);
			if (linkStore.exists()) {
				LOG.info("Removing not in use repo in " + linkStore);
				FileUtils.deleteDirectory(linkStore);
			}
			return;
		}

		repos.remove(linkStore);
		repoRefCounts.remove(linkStore);
		firstInits.remove(linkStore);
		try {
			repo.shutDown();
		} catch (RepositoryException e) {
			LOG.warn("Can't shut down repo@" + Integer.toHexString(repo.hashCode()) + " in " + linkStore + ": "
					+ e.getMessage());
		}

		FileUtils.deleteDirectory(linkStore);

		LOG.info("Done clearing Aperture repo@" + Integer.toHexString(repo.hashCode()) + " for collection '" + collection
				+ "'.");
	}

	public void clearRepositoryForUrl(gaia.crawl.datasource.DataSource ds) throws IOException {
		File linkStore = getLinkStoreDir(ds.getCollection());
		Repository repo = (Repository) repos.get(linkStore);
		if (repo == null) {
			LOG.info("Clearing for dsId=" + ds.getDataSourceId() + " but nonexistent repo ID: " + linkStore);
			if (linkStore.exists()) {
				LOG.warn("Possible leak - repo ID missing but files still exist: " + linkStore);
			}
			return;
		}
		String file = null;
		String url = DataSourceUtils.getSourceUri(ds);
		if (url == null) {
			url = (String) ds.getProperty("url");
		}

		if (url != null) {
			if (url.startsWith("file:")) {
				file = DataSourceUtils.getSource(ds);
			}
			LOG.info("cleaning repo@" + Integer.toHexString(repo.hashCode()) + " for url " + url);
			org.semanticdesktop.aperture.datasource.DataSource aDs = getDataSource(url, file);
			if (aDs != null) {
				String id = aDs.getID().toString();
				RepositoryModel model = new RepositoryModel(aDs.getID(), repo);
				model.open();
				model.setAutocommit(false);
				long sizeBeforeClear = model.size();

				ModelAccessData accessData = new ModelAccessData(model);
				accessData.clear();

				model.removeAll();
				model.commit();
				long sizeAfterClear = model.size();
				model.close();

				LOG.info("Cleared repo@" + Integer.toHexString(repo.hashCode()) + " for URL: " + url + " id: " + id
						+ " size before clear: " + sizeBeforeClear + " size after clear: " + sizeAfterClear);
			} else {
				LOG.warn("Could not create a semanticdesktop DataSource for url " + url);
			}
		} else {
			LOG.warn("No url for dataSource " + ds + " - not cleaning the repo@" + Integer.toHexString(repo.hashCode())
					+ " repoStore: " + linkStore);
		}
	}

	public long repositorySize(gaia.crawl.datasource.DataSource ds) throws IOException {
		File linkStore = getLinkStoreDir(ds.getCollection());
		Repository repo = (Repository) repos.get(linkStore);
		if (repo == null) {
			return 0L;
		}
		String url = null;
		String file = null;
		url = DataSourceUtils.getSourceUri(ds);
		if (url == null) {
			url = (String) ds.getProperty("url");
		}
		if (url != null) {
			if (url.startsWith("file:")) {
				file = DataSourceUtils.getSource(ds);
			}
			org.semanticdesktop.aperture.datasource.DataSource aDs = getDataSource(url, file);

			RepositoryModel model = new RepositoryModel(aDs.getID(), repo);
			model.open();
			long size = model.size();
			model.close();
			return size;
		}

		throw new RuntimeException("No url for dataSource " + ds + " - not cleaning the repo " + linkStore);
	}

	private static File getLinkStoreDir(String coreName) {
		return new File(Constants.GAIA_DATA_HOME, coreName + File.separator + "crawlstore");
	}

	private static org.semanticdesktop.aperture.datasource.DataSource getDataSource(String uri, String file) {
		org.semanticdesktop.aperture.datasource.DataSource result = null;

		if (uri.startsWith("http")) {
			RDFContainer container = ApertureCrawler.newInstance(uri);
			result = new WebDataSource();
			result.setConfiguration(container);
			((WebDataSource) result).setRootUrl(uri);
		} else if (uri.startsWith("file")) {
			RDFContainer container = ApertureCrawler.newInstance(uri);
			result = new FileSystemDataSource();
			result.setConfiguration(container);
			((FileSystemDataSource) result).setRootFolder(file);
		}
		return result;
	}
}
