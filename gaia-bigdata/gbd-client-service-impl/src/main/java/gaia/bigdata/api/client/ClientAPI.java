package gaia.bigdata.api.client;

import gaia.bigdata.api.SDARole;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;

import java.util.List;

import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Enroler;
import org.restlet.security.Role;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ClientAPI extends API {
	private static transient Logger log = LoggerFactory.getLogger(ClientAPI.class);
	private Verifier verifier;
	private Enroler enroler;
	private ServiceLocator serviceLocator;

	@Inject
	public ClientAPI(ResourceFinder finder, ServiceLocator serviceLocator, Verifier verifier, Enroler enroler) {
		super(finder);
		List<Role> roles = getRoles();
		for (SDARole role : SDARole.values()) {
			roles.add(role.getRole());
		}
		this.verifier = verifier;
		this.enroler = enroler;
		this.serviceLocator = serviceLocator;
	}

	public String getAPIRoot() {
		return "/client";
	}

	protected void initAttachments() {
		attach("/classifier", router, ClientModelListSR.class, new Role[] { SDARole.ADMINISTRATOR.getRole(),
				SDARole.CLASSIFICATION.getRole() });
		attach("/classifier/{model}", router, ClientClassificationSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.CLASSIFICATION.getRole() });

		attach("/collections", router, ClientCollectionsSR.class, new Role[] { SDARole.ADMINISTRATOR.getRole(),
				SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole() });
		attach("/collections/{collection}", router, ClientCollectionSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole() });

		attach("/collections/{collection}/analysis", router, ClientAnalysisSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.ANALYSIS.getRole() });

		attach("/collections/{collection}/documents", router, ClientDocumentsSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.DOCUMENTS.getRole() });

		attach("/collections/{collection}/documents/doc/{id}", router, ClientDocumentSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.DOCUMENTS.getRole() });

		attach("/collections/{collection}/documents/retrieval", router, ClientDocumentsRetrievalSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.DOCUMENTS.getRole() });

		attach("/collections/{collection}/documents/deletion", router, ClientDocumentsDeletionSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.DOCUMENTS.getRole() });

		attach("/collections/{collection}/datasources", router, ClientDataSourcesSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.DATASOURCES.getRole() });

		attach("/collections/{collection}/datasources/{id}", router, ClientDataSourceSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole(), SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(),
						SDARole.DATASOURCES.getRole() });

		URIPayload gaia = serviceLocator.getServiceURI(ServiceType.GAIASEARCH.name());
		if ((gaia != null) && (gaia.uri != null)) {
			log.info("Attaching Gaia to " + gaia);
			redir(gaia.uri, router, "/collections/{collection}/datasources/{id}/schedule",
					"/collections/{collection}/datasources/{id}/schedule{rr}", new Role[] { SDARole.ADMINISTRATOR.getRole(),
							SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(), SDARole.DATASOURCES.getRole() });

			redir(gaia.uri, router, "/collections/{collection}/datasources/{id}/job",
					"/collections/{collection}/datasources/{id}/job{rr}", new Role[] { SDARole.ADMINISTRATOR.getRole(),
							SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(), SDARole.DATASOURCES.getRole() });

			redir(gaia.uri, router, "/collections/{collection}/datasources/{id}/status",
					"/collections/{collection}/datasources/{id}/status{rr}", new Role[] { SDARole.ADMINISTRATOR.getRole(),
							SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(), SDARole.DATASOURCES.getRole() });

			redir(gaia.uri, router, "/collections/{collection}/datasources/{id}/history",
					"/collections/{collection}/datasources/{id}/history{rr}", new Role[] { SDARole.ADMINISTRATOR.getRole(),
							SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(), SDARole.DATASOURCES.getRole() });

			redir(gaia.uri, router, "/collections/{collection}/datasources/{id}/crawldata",
					"/collections/{collection}/datasources/{id}/crawldata{rr}", new Role[] { SDARole.ADMINISTRATOR.getRole(),
							SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(), SDARole.DATASOURCES.getRole() });

			redir(gaia.uri, router, "/collections/{collection}/datasources/{id}/index",
					"/collections/{collection}/datasources/{id}/index{rr}", new Role[] { SDARole.ADMINISTRATOR.getRole(),
							SDARole.COLLECTIONS.getRole(), SDARole.DEFAULT.getRole(), SDARole.DATASOURCES.getRole() });
		} else {
			log.warn("Unable to locate " + ServiceType.GAIASEARCH.name());
		}

		attach("/workflows", router, ClientWorkflowsSR.class, new Role[] { SDARole.WORKFLOWS.getRole(),
				SDARole.DEFAULT.getRole(), SDARole.ADMINISTRATOR.getRole() });
		attach("/workflows/{workflow}", router, ClientWorkflowSR.class, new Role[] { SDARole.WORKFLOWS.getRole(),
				SDARole.DEFAULT.getRole(), SDARole.ADMINISTRATOR.getRole() });

		attach("/jobs", router, ClientJobsSR.class, new Role[] { SDARole.JOBS.getRole(), SDARole.DEFAULT.getRole(),
				SDARole.ADMINISTRATOR.getRole() });
		attach("/jobs/{id}", router, ClientJobSR.class, new Role[] { SDARole.JOBS.getRole(),
				SDARole.DEFAULT.getRole(), SDARole.ADMINISTRATOR.getRole() });

		attach("/admin/users", router, ClientUsersSR.class, new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/users/{username}", router, ClientUserSR.class, new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/users/{username}/roles", router, ClientUserRolesSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/users/{username}/roles/{rolename}", router, ClientUserRolesDeletionSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/info", router, ClientAdminInfoSR.class, new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/info/statistics", router, ClientAdminStatsSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/info/endpoints", router, ClientAdminEndpointsSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/info/services", router, ClientAdminServicesSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });

		attach("/admin/classifier", router, ClientClassifierModelsSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });

		attach("/admin/classifier/{model}", router, ClientClassifierModelSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });

		attach("/admin/classifierstate", router, ClientClassifierStateSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });
		attach("/admin/classifierstate/{model}", router, ClientClassifierModelStateSR.class,
				new Role[] { SDARole.ADMINISTRATOR.getRole() });
	}

	protected Restlet guardPossibly() {
		ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC, "SDA Client");

		guard.setVerifier(verifier);
		guard.setEnroler(enroler);

		guard.setNext(router);
		return guard;
	}

	public String getAPIName() {
		return ServiceType.CLIENT.name();
	}
}
