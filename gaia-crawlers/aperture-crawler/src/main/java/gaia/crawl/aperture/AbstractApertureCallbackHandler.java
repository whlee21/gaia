package gaia.crawl.aperture;

import org.semanticdesktop.aperture.accessor.RDFContainerFactory;
import org.semanticdesktop.aperture.crawler.Crawler;
import org.semanticdesktop.aperture.crawler.CrawlerHandler;
import org.semanticdesktop.aperture.crawler.ExitCode;
import org.semanticdesktop.aperture.datasource.config.DomainBoundaries;

public abstract class AbstractApertureCallbackHandler implements RDFContainerFactory, CrawlerHandler {
	protected int maxDocs = Integer.MAX_VALUE;

	public void setMaxDocs(int maxDocs) {
		this.maxDocs = maxDocs;
	}

	public void crawlStarted(Crawler crawler) {
	}

	public void crawlStopped(Crawler crawler, ExitCode exitCode) {
	}

	public void accessingObject(Crawler crawler, String url) {
	}

	public void clearFinished(Crawler crawler, ExitCode exitCode) {
	}

	public void clearingObject(Crawler crawler, String url) {
	}

	public void clearStarted(Crawler crawler) {
	}

	public RDFContainerFactory getRDFContainerFactory(Crawler crawler, String url) {
		return this;
	}

	public void objectNotModified(Crawler crawler, String url) {
	}

	public void objectRemoved(Crawler crawler, String url) {
	}

	public void setDomainBoundaries(DomainBoundaries boundaries) {
	}
}
