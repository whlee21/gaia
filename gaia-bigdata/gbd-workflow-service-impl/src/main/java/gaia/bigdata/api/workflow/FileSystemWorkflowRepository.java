package gaia.bigdata.api.workflow;

import com.google.inject.name.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class FileSystemWorkflowRepository {
	private final Path appRoot;
	private final Configuration conf;
	private final FileSystem fs;

	public FileSystemWorkflowRepository(@Named("Workflow URI") String workflowAppURI) throws IOException {
		appRoot = new Path(workflowAppURI);
		conf = new Configuration();
		fs = appRoot.getFileSystem(conf);
	}

	public List<String> listResources() throws IOException {
		FileStatus[] fstats = fs.listStatus(appRoot);
		List<String> out = new ArrayList<String>();
		for (FileStatus fstat : fstats) {
			if (fstat.isDirectory()) {
				out.add(fstat.getPath().getName());
			}
		}
		return out;
	}

	public void getResource() {
	}
}
