package gaia.crawl.fs.ds;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jets3t.service.S3ObjectsChunk;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3FS extends FS {
	private static final Logger LOG = LoggerFactory.getLogger(S3FS.class);
	private static final String FOLDER_SUFFIX = "_$folder$";
	S3Service service;

	S3FS(DataSource fsds) throws IOException {
		super(fsds);
		String accessKey = (String) fsds.getProperty("username");
		String secretKey = (String) fsds.getProperty("password");
		initCredentials(accessKey, secretKey);
	}

	private void initCredentials(String accessKey, String secretKey) throws IOException {
		AWSCredentials awsCredentials = new AWSCredentials(accessKey, secretKey);
		try {
			service = new RestS3Service(awsCredentials);
		} catch (S3ServiceException e) {
			throw new IOException(e);
		}
	}

	S3FS(String username, String password) throws IOException {
		super(null);
		initCredentials(username, password);
	}

	public FSObject get(String path) throws IOException {
		URI u = null;
		try {
			u = new URI(path);
		} catch (URISyntaxException use) {
			throw new IOException(use);
		}
		String buck = u.getHost();
		try {
			S3Bucket b = service.getBucket(buck);
			if (b == null) {
				return null;
			}
			String objKey = u.getPath();
			String objKeySlash = null;
			if ((objKey.endsWith("/")) && (objKey.length() > 1)) {
				objKeySlash = objKey;
				objKey = objKey.substring(0, objKey.length() - 1);
				objKey = objKey + FOLDER_SUFFIX;
			}
			if (objKey.startsWith("/")) {
				objKey = objKey.substring(1);
				if (objKeySlash != null) {
					objKeySlash = objKeySlash.substring(1);
				}
			}
			S3ServiceException exc = null;
			S3Object o = null;
			try {
				o = service.getObject(b, objKey);
			} catch (S3ServiceException se) {
				if (objKey.endsWith(FOLDER_SUFFIX)) {
					objKey = objKeySlash;
					try {
						o = service.getObject(b, objKey);
					} catch (S3ServiceException se1) {
						S3ObjectsChunk chunk = service.listObjectsChunked(b.getName(), objKey, "/", 10L, null);
						S3Object[] objs = chunk.getObjects();
						if ((objs != null) && (objs.length > 0)) {
							return new S3FSObject(b, objKey, service);
						}
					}
				}
			}
			if (o == null) {
				return null;
			}
			return new S3FSObject(b, o, service);
		} catch (S3ServiceException e) {
			throw new IOException(u.toString() + ": " + e.getS3ErrorCode() + " - " + e.getS3ErrorMessage());
		}
	}

	public void close() {
		service = null;
	}

	static class S3FSObject extends FSObject {
		private S3Object o;
		private S3Bucket b;
		private S3Service service;
		private String path;

		S3FSObject(S3Bucket b, S3Object o, S3Service service) {
			this.b = b;
			this.o = o;
			this.service = service;
			String fName = o.getKey();
			if (fName.endsWith(FOLDER_SUFFIX)) {
				fName = fName.substring(0, fName.indexOf(FOLDER_SUFFIX));
				directory = true;
			} else if ((fName.endsWith("/")) || (fName.equals(""))) {
				directory = true;
			} else {
				directory = false;
			}
			path = fName;
			if (fName.endsWith("/")) {
				fName = fName.substring(0, fName.length() - 1);
			}
			int idx = fName.lastIndexOf('/');
			if (idx != -1) {
				fName = fName.substring(idx);
			}
			name = fName;
			uri = ("s3://" + o.getBucketName() + "/" + path);
			owner = (o.getOwner() != null ? o.getOwner().getDisplayName() : "");
			acls = EMPTY_ACLS;
			size = o.getContentLength();
			lastModified = o.getLastModifiedDate().getTime();
		}

		S3FSObject(S3Bucket b, String pseudoFolder, S3Service service) {
			this.b = b;
			this.path = pseudoFolder;
			this.service = service;
			name = path;
			directory = true;
			uri = ("s3://" + b.getName() + "/" + path);
			owner = "";
			size = 0L;
			acls = EMPTY_ACLS;
			lastModified = 0L;
		}

		public Iterable<FSObject> getChildren() throws IOException {
			try {
				String objKey = path;
				if ((directory) && (objKey.length() > 0) && (!objKey.endsWith("/"))) {
					objKey = objKey + "/";
				}
				S3ObjectsChunk chunk = service.listObjectsChunked(b.getName(), objKey, "/", Integer.MAX_VALUE, null);
				S3Object[] objs = chunk.getObjects();
				if ((objs == null) || ((objs.length == 0) && (chunk.getCommonPrefixes() == null))) {
					return null;
				}
				List<FSObject> res = new ArrayList<FSObject>();
				Set<String> names = new HashSet<String>();
				if (objs != null) {
					for (int i = 0; i < objs.length; i++) {
						res.add(new S3FSObject(b, objs[i], service));
						String name = objs[i].getKey();
						if (name.endsWith(FOLDER_SUFFIX)) {
							name = name.substring(0, name.length() - FOLDER_SUFFIX.length()) + "/";
						}
						names.add(name);
					}
				}
				if (chunk.getCommonPrefixes() != null) {
					for (String s : chunk.getCommonPrefixes()) {
						if (!names.contains(s)) {
							res.add(new S3FSObject(b, s, service));
						}
					}
				}
				return res;
			} catch (S3ServiceException e) {
				throw new IOException(e);
			}
		}

		public InputStream open() throws IOException {
			if (o == null)
				return null;
			try {
				S3Object so = service.getObject(b, o.getKey());
				return so.getDataInputStream();
			} catch (S3ServiceException e) {
				throw new IOException(e);
			}
		}
	}
}
