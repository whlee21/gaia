package gaia.crawl.fs.ds;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.log4j.Logger;

public class GaiaSmbFile extends SmbFile {
	private final String host;
	private static Logger LOG = Logger.getLogger(GaiaSmbFile.class.getName());

	public String getCanonicalPath() {
		if (host == null)
			return super.getCanonicalPath();
		try {
			String finalPath = SmbPathValidator.invert(super.getCanonicalPath());

			return CIFSFS.replaceHost(new URI(finalPath), host).toString();
		} catch (URISyntaxException e) {
			LOG.debug("Error: ", e);
			throw new RuntimeException(e);
		}
	}

	public GaiaSmbFile(SmbFile context, String name, String host) throws MalformedURLException, UnknownHostException {
		super(context, name);
		this.host = host;
	}

	public GaiaSmbFile(String uri, NtlmPasswordAuthentication authentication, String hostInPath)
			throws MalformedURLException {
		super(uri, authentication);
		host = hostInPath;
	}

	public SmbFile[] listFiles() throws SmbException {
		SmbFile[] originalFiles = super.listFiles();

		GaiaSmbFile[] files = new GaiaSmbFile[originalFiles.length];

		for (int i = 0; i < files.length; i++) {
			try {
				files[i] = new GaiaSmbFile(this, originalFiles[i].getCanonicalPath(), host);
			} catch (MalformedURLException e) {
				LOG.debug("Error: ", e);
				throw new RuntimeException(e);
			} catch (UnknownHostException e) {
				LOG.debug("Error: ", e);
				throw new RuntimeException(e);
			}
		}

		return files;
	}
}
