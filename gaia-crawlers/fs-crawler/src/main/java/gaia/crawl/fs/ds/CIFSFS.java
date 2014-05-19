package gaia.crawl.fs.ds;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;
import gaia.security.WindowsACLQueryFilterer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import jcifs.UniAddress;
import jcifs.smb.ACE;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CIFSFS extends FS {
	private static final Logger LOG = LoggerFactory.getLogger(CIFSFS.class);
	private final NtlmPasswordAuthentication authentication;
	private final URI root;
	private final String host;

	public CIFSFS(DataSource fsds) throws IOException {
		super(fsds);

		String urlString = (String) fsds.getProperty("url");
		String username = (String) fsds.getProperty("username");
		String password = (String) fsds.getProperty("password");
		String domainName = (String) fsds.getProperty("windows_domain", "");
		LOG.info("url:" + urlString);
		LOG.info("username:" + username);
		LOG.info("domain:" + domainName);
		URI tmpRoot;
		try {
			tmpRoot = new URI(urlString);
		} catch (URISyntaxException e1) {
			throw new RuntimeException(e1);
		}
		String tmpHost = tmpRoot.getHost();

		UniAddress uniaddr = null;
		NtlmPasswordAuthentication auth = null;
		try {
			uniaddr = UniAddress.getByName(tmpHost);
			auth = new NtlmPasswordAuthentication(domainName, username, password);

			logon(tmpRoot, uniaddr, auth);
		} catch (SmbException e) {
			String ipAddr = InetAddress.getByName(tmpHost).getHostAddress();
			try {
				tmpRoot = replaceHost(tmpRoot, ipAddr);
			} catch (URISyntaxException e1) {
				throw new RuntimeException(e1);
			}
			uniaddr = UniAddress.getByName(ipAddr);
			auth = new NtlmPasswordAuthentication(domainName, username, password);
			try {
				logon(tmpRoot, uniaddr, auth);
			} catch (SmbException smbe) {
				LOG.error("Could not authenticate.");
				throw smbe;
			}
		}

		authentication = auth;
		root = tmpRoot;
		host = (tmpHost.equals(tmpRoot.getHost()) ? null : tmpHost);
	}

	private void logon(URI rootURI, UniAddress uniaddr, NtlmPasswordAuthentication auth) throws SmbException {
		LOG.info("Trying to authenticate with " + uniaddr);

		if (rootURI.getPort() != -1)
			SmbSession.logon(uniaddr, rootURI.getPort(), auth);
		else
			SmbSession.logon(uniaddr, auth);
	}

	static final URI replaceHost(URI original, String host) throws URISyntaxException {
		return new URI(original.toString().replace(original.getHost(), host));
	}

	public FSObject get(String path) throws IOException {
		URI uri = root.resolve(path);
		try {
			String finalPath = SmbPathValidator.validate(replaceHost(uri, root.getHost()).toString());
			SmbFile file = new GaiaSmbFile(finalPath, authentication, host);
			if (!file.exists()) {
				throw new FileNotFoundException("The path: " + path + " does not exist.");
			}
			return new CifsFSObject(file);
		} catch (SmbException smbe) {
			LOG.warn("Problem accessing path: " + path, smbe);
			throw new FileNotFoundException("Problem accessing path: " + path + " - " + smbe.getMessage());
		} catch (URISyntaxException e) {
			LOG.warn("Problem accessing path: " + path, e);
			throw new IOException("Problem accessing path: " + path + " - " + e.getMessage(), e);
		}
	}

	public void close() {
	}

	private static class CifsFSObject extends FSObject {
		private final SmbFile smbFile;

		CifsFSObject(SmbFile file) throws IOException {
			try {
				smbFile = file;
				size = smbFile.getContentLength();
				uri = smbFile.getCanonicalPath();
				lastModified = smbFile.getLastModified();
				directory = smbFile.isDirectory();
				name = smbFile.getName();
				List<String> aclList = new ArrayList<String>();
				try {
					ACE[] aces = smbFile.getSecurity(true);
					for (ACE ace : aces)
						aclList.add(new StringBuilder()
								.append(ace.isAllow() ? WindowsACLQueryFilterer.ALLOWPREFIX : WindowsACLQueryFilterer.DENYPREFIX)
								.append(ace.getSID()).toString());
				} catch (Throwable t) {
					LOG.warn("Could not read ACL from resource: {}", uri);
				}
				acls = ((String[]) aclList.toArray(new String[aclList.size()]));
			} catch (IOException e) {
				LOG.warn(new StringBuilder().append("Could not access smb file from resource: ").append(uri).toString(), e);
				throw e;
			}
		}

		public Iterable<FSObject> getChildren() throws Throwable {
			try {
				SmbFile[] smbFiles = smbFile.listFiles();

				LOG.debug(new StringBuilder().append(uri).append(" -> children = ").append(smbFiles.length).toString());

				List<FSObject> files = new ArrayList<FSObject>(smbFiles.length);

				for (int i = 0; i < smbFiles.length; i++) {
					files.add(new CifsFSObject(smbFiles[i]));
				}
				return files;
			} catch (Throwable e) {
				LOG.warn(new StringBuilder().append("Could not read children for resource: ").append(uri).toString(), e);

				if ((e instanceof SmbAuthException)) {
					throw new AuthException(e.getMessage());
				}
				throw e;
			}
		}

		public InputStream open() throws Throwable {
			try {
				return smbFile.getInputStream();
			} catch (Throwable e) {
				LOG.warn(new StringBuilder().append("Could not read input stream for resource: ").append(uri).toString(), e);
				if ((e instanceof SmbAuthException)) {
					throw new AuthException(e.getMessage());
				}
				throw e;
			}
		}
	}
}
