package gaia.bigdata.api.workflow;

import gaia.bigdata.api.State;
import gaia.bigdata.api.job.JobsResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Map;
import java.util.Properties;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class WorkflowServerResource extends BaseServerResource implements WorkflowResource {
	private Workflow workflow;
	private final WorkflowService workflowClient;
	private final ServiceLocator locator;

	@Inject
	public WorkflowServerResource(Configuration configuration, WorkflowService client, ServiceLocator locator) {
		super(configuration);
		this.workflowClient = client;
		this.locator = locator;
	}

	protected void doInit() {
		String workflowId = (String) getRequest().getAttributes().get("workflow");
		try {
			workflow = workflowClient.getWorkflowById(workflowId);
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
		if (workflow == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Unknown workflow: " + workflowId);
	}

	public Workflow retrieve() {
		return workflow;
	}

	public State submit(Map<String, Object> entity) {
		RestletContainer<JobsResource> resourceRc = RestletUtil.wrap(JobsResource.class,
				locator.getServiceURI(ServiceType.JOB.name()), "");
		try {
			Properties props = workflowClient.loadWorkflowProperties(workflow);
			boolean hasSolrZKHost = false;
			boolean hasZKHost = false;
			String key;
			if (entity != null) {
				for (Map.Entry<String, Object> entry : entity.entrySet()) {
					key = (String) entry.getKey();

					if (key.equals("solr_zkHost")) {
						hasSolrZKHost = true;
					}

					if (key.equals("zkhost")) {
						hasZKHost = true;
					}
					if (props.containsKey(key))
						props.put(key, entry.getValue());
				}
			}
			if (!hasSolrZKHost) {
				props.put("solr_zkHost", configuration.getProperties().getProperty("solr.zk.connect"));
			}

			if (!hasZKHost) {
				props.put("zkhost", configuration.getProperties().getProperty("hbase.zk.connect"));
			}
			JobsResource resource = (JobsResource) resourceRc.getWrapped();
			State returnValue = resource.send(props);
			return returnValue;
		} catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} finally {
			RestletUtil.release(resourceRc);
		}
	}
}
