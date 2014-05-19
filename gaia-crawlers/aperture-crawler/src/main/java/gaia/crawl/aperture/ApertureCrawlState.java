package gaia.crawl.aperture;

import gaia.crawl.CrawlException;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.datasource.DataSource;
import gaia.utils.IdGenerator;
import gaia.utils.IdGeneratorFactory;

import org.openrdf.repository.Repository;
import org.semanticdesktop.aperture.crawler.Crawler;
import org.semanticdesktop.aperture.crawler.CrawlerRegistry;
import org.semanticdesktop.aperture.crawler.impl.DefaultCrawlerRegistry;
import org.semanticdesktop.aperture.datasource.DataSourceRegistry;
import org.semanticdesktop.aperture.datasource.impl.DefaultDataSourceRegistry;
import org.semanticdesktop.aperture.extractor.ExtractorRegistry;
import org.semanticdesktop.aperture.extractor.impl.DefaultExtractorRegistry;
import org.semanticdesktop.aperture.extractor.xmp.XMPExtractorFactory;
import org.semanticdesktop.aperture.hypertext.linkextractor.LinkExtractorRegistry;
import org.semanticdesktop.aperture.hypertext.linkextractor.impl.DefaultLinkExtractorRegistry;
import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;
import org.semanticdesktop.aperture.subcrawler.SubCrawlerRegistry;
import org.semanticdesktop.aperture.subcrawler.impl.DefaultSubCrawlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public final class ApertureCrawlState extends CrawlState {
	private static transient Logger LOG = LoggerFactory.getLogger(ApertureCrawlState.class);

	final MimeTypeIdentifier mimeTypeIdentifier = new MagicMimeTypeIdentifier();
	final ExtractorRegistry extractorRegistry = new DefaultExtractorRegistry();
	final LinkExtractorRegistry linkRegistry = new DefaultLinkExtractorRegistry();
	final DataSourceRegistry dataSourceRegistry = new DefaultDataSourceRegistry();
	final XMPExtractorFactory xmpExtractorFactory = new XMPExtractorFactory();
	final SubCrawlerRegistry subCrawlerRegistry = new DefaultSubCrawlerRegistry();
	final CrawlerRegistry crawlerRegistry = new DefaultCrawlerRegistry();
	private ApertureRepos repos;
	private Repository repo;
	private Crawler crawler = null;

	private static IdGeneratorFactory idGeneratorFactory = new IdGeneratorFactory();

	@Inject
	public ApertureCrawlState(ApertureRepos repos) {
		this.repos = repos;
	}

	public synchronized void init(DataSource ds, CrawlProcessor processor, HistoryRecorder recorder) throws Exception {
		super.init(ds, processor, recorder);
		if ((repo == null) || (!repo.isWritable())) {
			repo = repos.initRepo(ds);
			LOG.info("Inited repo@" + Integer.toHexString(repo.hashCode()));
		}
	}

	public synchronized void abort() {
		if (crawler != null) {
			getStatus().setState(CrawlStatus.JobState.ABORTING);
			crawler.stop();
		}
	}

	public synchronized void stop() {
		if (crawler != null) {
			getStatus().setState(CrawlStatus.JobState.STOPPING);
			crawler.stop();
		}
	}

	public synchronized void close() throws CrawlException {
		if (crawler != null) {
			crawler.stop();
			crawler = null;
		}
		try {
			if ((repo != null) && (repo.isWritable())) {
				LOG.info("Closing repo@" + Integer.toHexString(repo.hashCode()));
				repos.close(repo, ds);
			}
			repo = null;
		} catch (Exception e) {
			throw new CrawlException("Error closing repository", e);
		}
	}

	public Repository getRepo() {
		return repo;
	}

	protected synchronized IdGenerator<?> getIdGenerator() {
		IdGenerator<?> generator = idGeneratorFactory.getGenerator(ds.getDataSourceId(), ds.getFieldMapping());

		return generator;
	}

	protected final synchronized Crawler getCrawler() {
		return crawler;
	}

	protected final synchronized void setCrawler(Crawler crawler) {
		this.crawler = crawler;
	}

	public synchronized void setDataSource(DataSource ds) throws Exception {
		super.setDataSource(ds);
		if ((repo == null) || (!repo.isWritable()))
			repo = repos.initRepo(ds);
	}
}
