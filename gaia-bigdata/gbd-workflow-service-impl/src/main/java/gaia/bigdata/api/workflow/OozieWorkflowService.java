package gaia.bigdata.api.workflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.bigdata.services.ServiceType;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OozieWorkflowService extends BaseService implements WorkflowService {
	private static transient Logger log = LoggerFactory.getLogger(OozieWorkflowService.class);
	private static final String OOZIE_JOB_PROPERTIES_FILE = "job.properties";
	static final String WORKFLOW_APP_URI = "workflow.app.uri";
	static final String OOZIE_APP_URI = "oozie.app.uri";
	static final String OOZIE_HADOOP_NAMENODE = "oozie.nameNode";
	static final String OOZIE_HADOOP_JOBTRACKER = "oozie.jobTracker";
	static final String OOZIE_HADOOP_QUEUENAME = "oozie.queueName";
	private final Path appPath;
	private final Path oozieAppPath;
	private final FileSystem fs;
	private Map<String, Workflow> workflows = Collections.synchronizedMap(new HashMap<String, Workflow>());

	@Inject
	public OozieWorkflowService(gaia.commons.api.Configuration config, ServiceLocator locator) throws IOException {
		super(config, locator);
		if ((config.getProperties().containsKey(WORKFLOW_APP_URI)) && (config.getProperties().containsKey(OOZIE_APP_URI))) {
			appPath = new Path(config.getProperties().getProperty(WORKFLOW_APP_URI));
			oozieAppPath = new Path(config.getProperties().getProperty(OOZIE_APP_URI));
			fs = appPath.getFileSystem(new org.apache.hadoop.conf.Configuration());
		} else {
			throw new IllegalArgumentException("Missing one or more required config values: workflow.app.uri, oozie.app.uri");
		}
		refreshWorkflows();
	}

	public String getType() {
		return ServiceType.WORKFLOW.name();
	}

	public void refreshWorkflows() throws IOException {
		synchronized (workflows) {
			FileStatus[] fstats = fs.globStatus(new Path(appPath, "*/workflow.xml"));
			addWorkflows(fstats);
			FileStatus[] fstatsForSubworkflows = fs.globStatus(new Path(appPath, "*/*/*/workflow.xml"));
			addWorkflows(fstatsForSubworkflows);
		}
	}

	private void addWorkflows(FileStatus[] fstats) throws IOException {
		for (FileStatus fstat : fstats) {
			log.debug("Refreshing Workflows...  Add workflow: " + fstat.getPath().toString());
			Path parentPath = fstat.getPath().getParent();
			FileStatus parentFStat = fs.getFileStatus(parentPath);
			String name = parentPath.getName();
			Properties props = new Properties();
			Path propsPath = new Path(parentPath, OOZIE_JOB_PROPERTIES_FILE);
			if (fs.exists(propsPath)) {
				props.load(fs.open(propsPath));
			}
			log.debug("Refreshing Workflows...  Add workflow: " + name + ": " + parentPath.toUri());
			workflows.put(name, new Workflow(name, parentPath.toUri(), props, new Date(parentFStat.getModificationTime())));
		}
	}

	public Collection<Workflow> listWorkflows() throws IOException {
		refreshWorkflows();
		return workflows.values();
	}

	public Workflow getWorkflowById(String id) {
		return (Workflow) workflows.get(id);
	}

	public Properties loadWorkflowProperties(Workflow workflow) throws IOException {
		Properties props = new Properties();
		props.putAll(workflow.getProperties());
		props.put("workflowId", workflow.getId());
		if ((config.getProperties().containsKey(OOZIE_HADOOP_NAMENODE))
				&& (config.getProperties().containsKey(OOZIE_HADOOP_JOBTRACKER))
				&& (config.getProperties().containsKey(OOZIE_HADOOP_QUEUENAME))) {
			props.put("nameNode", config.getProperties().getProperty(OOZIE_HADOOP_NAMENODE));
			props.put("jobTracker", config.getProperties().getProperty(OOZIE_HADOOP_JOBTRACKER));
			props.put("queueName", config.getProperties().getProperty(OOZIE_HADOOP_QUEUENAME));
		} else {
			throw new IllegalArgumentException(
					"Missing one or more required config values: oozie.nameNode, oozie.jobTracker, oozie.queueName");
		}

		props.put("oozie.wf.application.path", "${nameNode}/" + new Path(oozieAppPath, workflow.getId()).toString());
		log.info("Resolved Workflow properties: {}", props);
		return props;
	}
}
