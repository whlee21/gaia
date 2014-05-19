package gaia.crawl.aperture;

import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSourceAPI;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.DataSourceUtils;
import gaia.crawl.http.robots.RobotsCache;
import gaia.crawl.impl.TikaParserController;
import gaia.utils.UrlUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.ontoware.rdf2go.ModelFactory;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.URI;
import org.openrdf.rdf2go.RepositoryModel;
import org.openrdf.repository.Repository;
import org.semanticdesktop.aperture.accessor.base.ModelAccessData;
import org.semanticdesktop.aperture.crawler.CrawlReport;
import org.semanticdesktop.aperture.crawler.Crawler;
import org.semanticdesktop.aperture.crawler.CrawlerFactory;
import org.semanticdesktop.aperture.crawler.ExitCode;
import org.semanticdesktop.aperture.crawler.base.CrawlReportBase;
import org.semanticdesktop.aperture.crawler.web.WebCrawlerFactory;
import org.semanticdesktop.aperture.datasource.config.ConfigurationUtil;
import org.semanticdesktop.aperture.datasource.config.DomainBoundaries;
import org.semanticdesktop.aperture.datasource.config.RegExpPattern;
import org.semanticdesktop.aperture.datasource.config.SubstringCondition;
import org.semanticdesktop.aperture.datasource.config.SubstringPattern;
import org.semanticdesktop.aperture.datasource.filesystem.FileSystemDataSource;
import org.semanticdesktop.aperture.datasource.web.WebDataSource;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApertureCrawler {
	private static transient Logger LOG = LoggerFactory.getLogger(ApertureCrawler.class);
	private final ApertureCrawlState crawlState;
	private CrawlerController cc;
	private boolean restricted;
	private static final ModelFactory factory = RDF2Go.getModelFactory();
	private static final RobotsCache robots = new RobotsCache();

	public ApertureCrawler(CrawlerController cc, ApertureCrawlState crawlState, boolean restricted) {
		this.crawlState = crawlState;
		this.restricted = restricted;
		this.cc = cc;
	}

	public void crawl() throws Exception {
		crawlState.getStatus().running();
		ExitCode exitCode = null;
		String type = (String) crawlState.getDataSource().getProperty("type");
		LOG.debug(new StringBuilder().append("type: ").append(type).toString());
		if (type == null) {
			String message = "Missing crawl type";
			LOG.error(message);
			throw new Exception(message);
		}
		UpdateController updateController;
		if ((crawlState.getProcessor() != null) && (crawlState.getProcessor().getUpdateController() != null)) {
			updateController = crawlState.getProcessor().getUpdateController();
		} else {
			updateController = UpdateController.create(cc, crawlState.getDataSource());
			updateController.start();
		}
		UUID uuid = UUID.randomUUID();
		SolrApertureCallbackHandler loader = null;

		String defaultField = "body";
		gaia.crawl.datasource.DataSource ds = crawlState.getDataSource();
		String batchId = crawlState.getId().toString();
		boolean logExtraDetail = ds.getBoolean("log_extra_detail", false);
		boolean failUnsupportedFileTypes = ds.getBoolean("fail_unsupported_file_types", false);
		boolean warnUnknownMimeTypes = ds.getBoolean("warn_unknown_mime_types", false);
		boolean addFailedDocs = ds.getBoolean("add_failed_docs", false);
		int maxRetries = ds.getInt("max_retries", 3);

		TikaParserController tika = new TikaParserController(updateController);
		tika.init(crawlState);
		tika.setAddFailedDocs(addFailedDocs);
		loader = new SolrApertureCallbackHandler(tika, updateController, defaultField, crawlState.getDataSource(),
				crawlState, batchId, uuid.toString(), logExtraDetail, failUnsupportedFileTypes, warnUnknownMimeTypes,
				addFailedDocs, maxRetries);

		CrawlReport crawlReport = null;
		boolean cantRunCrawl = false;

		long now = System.currentTimeMillis();
		String crawlUrl = DataSourceUtils.getSourceUri(ds);
		Throwable validateError = null;
		Throwable crawlError = null;
		try {
			UrlUtils.validateUrl("Crawl could not be run (invalid root URL)", crawlUrl);
		} catch (Throwable t) {
			validateError = t;
		}
		if (validateError == null) {
			try {
				crawlReport = processURL(crawlUrl, loader);
			} catch (Throwable e) {
				crawlError = e;
			}
		}
		if ((validateError != null) || (crawlError != null)) {
			CrawlReportBase dummyReport = new CrawlReportBase();
			dummyReport.setCrawlStarted(now);
			dummyReport.setCrawlStopped(System.currentTimeMillis());
			dummyReport.setExitCode(ExitCode.FATAL_ERROR);
			dummyReport.setFatalErrorCause(validateError != null ? validateError : crawlError);
			crawlReport = dummyReport;
			String msg = crawlUrl;
			if (validateError != null) {
				msg = new StringBuilder().append(msg).append(" - ").append(validateError.getMessage()).toString();
			}
			loader.addFailedParsingDoc(crawlUrl, null, crawlError, msg, CrawlStatus.Counter.Failed);
			cantRunCrawl = true;
		}

		LOG.debug(new StringBuilder().append("crawlReport: ").append(crawlReport == null ? "null" : crawlReport.toString())
				.toString());
		boolean commit = crawlState.getDataSource().getBoolean("commit_on_finish", true);

		if (crawlReport != null) {
			exitCode = crawlReport.getExitCode();
			LOG.debug(new StringBuilder().append("REPORT: crawlStarted: ").append(crawlReport.getCrawlStarted())
					.append(" crawlStopped: ").append(crawlReport.getCrawlStopped()).append(" numNew: ")
					.append(crawlReport.getNewCount()).append(" numUpdated: ").append(crawlReport.getChangedCount())
					.append(" numDeleted: ").append(crawlReport.getRemovedCount()).append(" numUnchanged: ")
					.append(crawlReport.getUnchangedCount()).append(" exitCode: ").append(exitCode).toString());

			CrawlStatus crawlStatus = crawlState.getStatus();

			if (exitCode == ExitCode.COMPLETED) {
				updateController.finish(commit);
				crawlStatus.end(CrawlStatus.JobState.FINISHED);
			} else if (exitCode == ExitCode.STOP_REQUESTED) {
				CrawlStatus.JobState st = crawlStatus.getState();
				Exception e;
				if (st != CrawlStatus.JobState.ABORTING)
					e = new Exception("Crawl STOPPED prematurely at user request");
				else {
					e = new Exception("Crawl ABORTED prematurely at user request");
				}
				loader.addFailedParsingDoc(crawlUrl, null, e, null, CrawlStatus.Counter.Failed);
				if (st != CrawlStatus.JobState.ABORTING) {
					updateController.finish(commit);
					crawlStatus.end(CrawlStatus.JobState.STOPPED);
				} else {
					updateController.finish(false);
					crawlStatus.end(CrawlStatus.JobState.ABORTED);
				}
			} else if (exitCode == ExitCode.FATAL_ERROR) {
				Throwable throwable = crawlReport.getFatalErrorCause();

				if (!cantRunCrawl) {
					Throwable cause = throwable.getCause();
					Exception e = new Exception(new StringBuilder().append("Crawl aborted prematurely due to EXCEPTION: ")
							.append(throwable)
							.append(cause == null ? "" : new StringBuilder().append(" cause: ").append(cause).toString()).toString());
					loader.addFailedParsingDoc(crawlUrl, null, e, null, CrawlStatus.Counter.Failed);
				}
				updateController.finish(false);
				crawlStatus.failed(crawlReport.getFatalErrorCause());
			} else {
				updateController.finish(commit);
				crawlStatus.end(CrawlStatus.JobState.UNKNOWN);
			}
		} else {
			LOG.debug("No crawlReport");
			updateController.finish(commit);
		}
	}

	protected CrawlReport processURL(String url, AbstractApertureCallbackHandler loader) throws IOException {
		gaia.crawl.datasource.DataSource ds = crawlState.getDataSource();
		org.semanticdesktop.aperture.datasource.DataSource dataSource = null;
		CrawlReport result = null;
		Model model = null;
		try {
			long maxBytes = DataSourceUtils.getMaxBytes(ds);
			if (maxBytes < 0L) {
				maxBytes = Long.MAX_VALUE;
			}
			int maxDocs = ds.getInt("max_docs");
			if (maxDocs < 0) {
				maxDocs = Integer.MAX_VALUE;
			}
			loader.setMaxDocs(maxDocs);
			String poiSize = System.getProperty("aperture.poiUtil.bufferSize");
			if (StringUtils.isNotEmpty(poiSize)) {
				long maxPoiSize = Long.parseLong(poiSize);
				if (maxBytes > maxPoiSize) {
					System.setProperty("aperture.poiUtil.bufferSize", String.valueOf(maxBytes));
				}
			}

			int depth = DataSourceUtils.getCrawlDepth(ds);
			if (depth < 0) {
				depth = Integer.MAX_VALUE;
			}

			if ((url.startsWith("http")) || (url.startsWith("https"))) {
				dataSource = getDataSource(url, maxBytes, depth);
				LOG.debug(new StringBuilder().append("dataSource: ").append(dataSource.toString()).append(" maxBytes: ")
						.append(maxBytes).append(" depth: ").append(depth).toString());
			} else if (url.startsWith("file:/")) {
				boolean symLinks = ds.getBoolean("follow_links");
				File f = CrawlerUtils.resolveRelativePath(DataSourceUtils.getPath(ds));
				dataSource = getDataSource(url, f.toString(), symLinks, maxBytes, depth);
			}

			DomainBoundaries boundaries = getDomainBoundaries(ds, restricted);
			if ((!boundaries.getIncludePatterns().isEmpty()) || (!boundaries.getExcludePatterns().isEmpty())) {
				ConfigurationUtil.setDomainBoundaries(boundaries, dataSource.getConfiguration());
			}
			loader.setDomainBoundaries(boundaries);
			Repository repo = crawlState.getRepo();
			URI repoDataSourceId = dataSource.getID();
			LOG.debug(new StringBuilder().append("Repository data source ID: ").append(repoDataSourceId.toString())
					.toString());
			model = new RepositoryModel(repoDataSourceId, repo);
			model.open();

			crawlState.setCrawler(getCrawler(model, dataSource, loader));
			Crawler crawler = crawlState.getCrawler();
			if (crawler != null) {
				long start = System.currentTimeMillis();
				crawler.crawl();
				long finish = System.currentTimeMillis();
				if (LOG.isInfoEnabled()) {
					LOG.info(new StringBuilder().append("Crawl of ").append(url).append(" took ").append(finish - start)
							.append(" ms").toString());
				}
				result = crawler.getCrawlReport();

				ExitCode exitCode = result.getExitCode();
				if (exitCode == ExitCode.COMPLETED)
					LOG.info("Aperture crawl ExitCode: COMPLETED");
				else if (exitCode == ExitCode.STOP_REQUESTED)
					LOG.info("Aperture crawl ExitCode: STOP_REQUESTED");
				else if (exitCode == ExitCode.FATAL_ERROR) {
					LOG.error(new StringBuilder().append("Aperture crawl ExitCode: FATAL_ERROR: ")
							.append(result.getFatalErrorCause()).toString());
				} else {
					LOG.error(new StringBuilder().append("Unexpected Aperture ExitCode: ").append(exitCode.toString()).toString());
				}

			}

		} finally {
			if (model != null) {
				model.close();
			}
			if (dataSource != null) {
				dataSource.dispose();
			}
			synchronized (crawlState) {
				crawlState.setCrawler(null);
			}
		}
		return result;
	}

	protected Crawler getCrawler(Model model, org.semanticdesktop.aperture.datasource.DataSource source,
			AbstractApertureCallbackHandler loader) {
		Crawler result = null;
		Set factories = crawlState.crawlerRegistry.get(source.getType());

		if (factories.size() > 0) {
			CrawlerFactory factory = (CrawlerFactory) factories.iterator().next();
			if ((factory instanceof WebCrawlerFactory)) {
				((WebCrawlerFactory) factory).setLinkExtractorRegistry(crawlState.linkRegistry);
				((WebCrawlerFactory) factory).setMimeTypeIdentifier(crawlState.mimeTypeIdentifier);
			}
			result = factory.getCrawler(source);
			result.setCrawlerHandler(loader);

			result.setDataAccessorRegistry(new GaiaDataAccessorRegistry(robots, crawlState.getDataSource(), crawlState));
		} else {
			throw new IllegalStateException(new StringBuilder().append("No factory available for source type:")
					.append(source.getType()).toString());
		}

		try {
			ModelAccessData accessData = newAccessData(model, source);
			result.setAccessData(accessData);
		} catch (IOException e) {
			LOG.error("Exception", e);
		}

		return result;
	}

	private ModelAccessData newAccessData(Model model, org.semanticdesktop.aperture.datasource.DataSource source)
			throws IOException {
		ModelAccessData accessData = new ModelAccessData(model);
		accessData.initialize();

		return accessData;
	}

	private static org.semanticdesktop.aperture.datasource.DataSource getDataSource(String url, String file,
			boolean followSymbolicLinks, long maxBytes, int depth) {
		FileSystemDataSource result = new FileSystemDataSource();
		RDFContainer container = newInstance(url);
		result.setConfiguration(container);

		result.setRootFolder(file);
		result.setFollowSymbolicLinks(Boolean.valueOf(followSymbolicLinks));
		result.setMaximumSize(Long.valueOf(maxBytes));
		result.setMaximumDepth(Integer.valueOf(depth));
		return result;
	}

	private static org.semanticdesktop.aperture.datasource.DataSource getDataSource(String url, long maxBytes, int depth) {
		WebDataSource result = new WebDataSource();
		RDFContainer container = newInstance(url);

		result.setConfiguration(container);
		result.setRootUrl(url);
		result.setMaximumSize(Long.valueOf(maxBytes));
		result.setMaximumDepth(Integer.valueOf(depth));

		return result;
	}

	static DomainBoundaries getDomainBoundaries(gaia.crawl.datasource.DataSource ds, boolean restricted) {
		List<String> includes = DataSourceUtils.getIncludePattern(ds);
		List<String> excludes = DataSourceUtils.getExcludePattern(ds);
		DataSourceAPI.Bounds b = DataSourceUtils.getCrawlBounds(ds);
		String pat = ApertureDataSourceUtil.getDomainPattern(ds);
		DomainBoundaries boundaries = new DomainBoundaries();
		boolean fileDS = ds.getType().equals(DataSourceSpec.Type.file.toString());
		if (includes != null) {
			for (String include : includes) {
				include = fixPattern(ds, fileDS, pat, include);
				boundaries.addIncludePattern(new RegExpPattern(include));
			}
		}
		if (excludes != null) {
			for (String exclude : excludes) {
				exclude = fixPattern(ds, fileDS, pat, exclude);
				boundaries.addExcludePattern(new RegExpPattern(exclude));
			}
		}

		if (b == DataSourceAPI.Bounds.tree) {
			if (fileDS) {
				boundaries.addExcludePattern(new SubstringPattern(pat, SubstringCondition.DOES_NOT_CONTAIN));
			} else if (!pat.matches("^http[s]?://www\\.")) {
				pat = pat.substring(pat.indexOf("://") + 3);
				pat = new StringBuilder().append("http[s]?://(?!(www\\.)?\\Q").append(pat).append("\\E).*").toString();
				boundaries.addExcludePattern(new RegExpPattern(pat));
			} else {
				boundaries.addExcludePattern(new SubstringPattern(pat, SubstringCondition.DOES_NOT_CONTAIN));
			}

		}

		if (restricted) {
			boundaries.addExcludePattern(new RegExpPattern("http[s]?://localhost.*"));
			boundaries.addExcludePattern(new RegExpPattern("http[s]?://127(\\.\\d+){3}.*"));
			try {
				InetAddress localhost = Inet4Address.getLocalHost();
				String name = localhost.getHostAddress();
				if (!StringUtils.isBlank(name)) {
					boundaries.addExcludePattern(new RegExpPattern(new StringBuilder().append("http[s]?://").append(name)
							.append(".*").toString()));
				}
				name = localhost.getHostName();
				if (!StringUtils.isBlank(name)) {
					boundaries.addExcludePattern(new RegExpPattern(new StringBuilder().append("http[s]?://").append(name)
							.append(".*").toString()));
				}
				name = localhost.getCanonicalHostName();
				if (!StringUtils.isBlank(name))
					boundaries.addExcludePattern(new RegExpPattern(new StringBuilder().append("http[s]?://").append(name)
							.append(".*").toString()));
			} catch (UnknownHostException e) {
				LOG.debug("Can't retrieve localhost's IPv4 address", e);
			}
			try {
				InetAddress localhost = Inet6Address.getLocalHost();
				String name = localhost.getHostAddress();
				if (!StringUtils.isBlank(name)) {
					boundaries.addExcludePattern(new RegExpPattern(new StringBuilder().append("http[s]?://").append(name)
							.append(".*").toString()));
				}
				name = localhost.getHostName();
				if (!StringUtils.isBlank(name)) {
					boundaries.addExcludePattern(new RegExpPattern(new StringBuilder().append("http[s]?://").append(name)
							.append(".*").toString()));
				}
				name = localhost.getCanonicalHostName();
				if (!StringUtils.isBlank(name))
					boundaries.addExcludePattern(new RegExpPattern(new StringBuilder().append("http[s]?://").append(name)
							.append(".*").toString()));
			} catch (UnknownHostException e) {
				LOG.debug("Can't retrieve localhost's IPv6 address", e);
			}
		}
		return boundaries;
	}

	private static String fixPattern(gaia.crawl.datasource.DataSource ds, boolean fileDS, String domainPattern,
			String pattern) {
		if (fileDS) {
			if (!pattern.startsWith("file:")) {
				if (pattern.startsWith("/"))
					pattern = new StringBuilder().append("file:").append(pattern).toString();
				else {
					pattern = new StringBuilder().append("file:\\Q").append(domainPattern)
							.append(domainPattern.endsWith("/") ? "" : "/").append("\\E").append(pattern).toString();
				}
			} else if (!pattern.startsWith("file:/")) {
				pattern = new StringBuilder().append("file:\\Q").append(domainPattern)
						.append(domainPattern.endsWith("/") ? "" : "/").append("\\E").append(pattern.substring(5)).toString();
			}

		} else if (!pattern.startsWith("http")) {
			pattern = new StringBuilder().append("http[s]?://.*").append(pattern).toString();
		}

		pattern = new StringBuilder().append("([a-z0-9]{2,6}:)*").append(pattern).toString();
		return pattern;
	}

	static RDFContainerImpl newInstance(String uri) {
		try {
			Model newModel = factory.createModel();
			newModel.open();
			return new RDFContainerImpl(newModel, uri);
		} catch (ModelRuntimeException me) {
			throw new RuntimeException(me);
		}
	}
}
