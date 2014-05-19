package gaia.rules.drools;

import gaia.rules.RulesEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.update.AddUpdateCommand;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.command.Command;
import org.drools.definition.KnowledgePackage;
import org.drools.io.Resource;
import org.drools.io.impl.InputStreamResource;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDroolsRulesEngine extends RulesEngine {
	private static transient Logger LOG = LoggerFactory.getLogger(BaseDroolsRulesEngine.class);
	public static final String RULES = "rules";
	public static final String FILE = "file";
	public static final String FACT_COLLECTOR = "factCollector";
	protected KnowledgeBase kbase;
	protected IndexSchema schema;
	protected boolean reload = false;
	protected FactCollector factCollector;

	public void init(String engineName, NamedList args, SolrCore core) throws Exception {
		super.init(engineName, args, core);
		createKnowledgeBase(core);
		createFactCollector((NamedList) args.get(FACT_COLLECTOR), core);
	}

	protected void createFactCollector(NamedList args, SolrCore core) throws IllegalAccessException,
			InstantiationException {
		String className;
		if (args != null) {
			Object arg = args.get("class");
			if (arg == null)
				className = FactCollector.class.getName();
			else
				className = arg.toString();
		} else {
			className = FactCollector.class.getName();
		}
		LOG.info("Instantiating FactCollector: " + className);
		factCollector = ((FactCollector) core.getResourceLoader().findClass(className, FactCollector.class).newInstance());
		factCollector.init(args, core);
	}

	protected void createKnowledgeBase(SolrCore core) throws IOException {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		NamedList rules = (NamedList) initArgs.get(RULES);
		hasRules = true;
		if ((rules != null) && (rules.size() > 0)) {
			List<String> all = rules.getAll(FILE);
			for (String file : all) {
				LOG.info("Loading rules from: " + file);
				SolrResourceLoader solrLoader = core.getResourceLoader();
				InputStream ruleRes = solrLoader.openResource(file);

				if (ruleRes != null) {
					Resource droolsResource = new InputStreamResource(ruleRes);
					kbuilder.add(droolsResource, ResourceType.DRL);

					if (kbuilder.hasErrors()) {
						LOG.error(kbuilder.getErrors().toString());
						throw new RuntimeException("Error constructing the rules KnowledgeBase");
					}
				} else {
					throw new IOException("Can't load rule file (" + file + ") from classpath using the SolrResourceLoader");
				}
			}
			if (emptyRulesSet(kbuilder)) {
				LOG.info("No rules defined for Drools engine");
				hasRules = false;
				kbase = null;
				return;
			}
		} else {
			LOG.info("No rules defined for Drools engine");
			hasRules = false;
			kbase = null;
			return;
		}
		kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
	}

	private boolean emptyRulesSet(KnowledgeBuilder kbuilder) {
		boolean foundRules = false;
		Iterator<KnowledgePackage> kpackages = kbuilder.getKnowledgePackages().iterator();
		while ((kpackages.hasNext()) && (!foundRules)) {
			foundRules = !((KnowledgePackage) kpackages.next()).getRules().isEmpty();
		}
		return !foundRules;
	}

	protected abstract CommandExecutor getSession();

	protected StatelessKnowledgeSession createStatelessSession() {
		return kbase.newStatelessKnowledgeSession();
	}

	protected StatefulKnowledgeSession createStatefulSession() {
		return kbase.newStatefulKnowledgeSession();
	}

	protected abstract void execute(CommandExecutor paramCommandExecutor, List<Command<?>> paramList,
			Collection<Object> paramCollection);

	public void findLandingPage(ResponseBuilder rb) {
		LOG.info("Executing findLandingPage in engine: " + engineName);
		CommandExecutor ksession = getSession();
		processRules(rb, ksession);
	}

	public void prepareSearch(ResponseBuilder rb) {
		LOG.info("Executing prepareSearch in engine: " + engineName);
		CommandExecutor ksession = getSession();
		processRules(rb, ksession);
	}

	protected void processRules(ResponseBuilder rb, CommandExecutor ksession) {
		List<Command<?>> cmds = new ArrayList<Command<?>>();
		Collection<Object> facts = new ArrayList<Object>();
		factCollector.addFacts(rb, facts);
		execute(ksession, cmds, facts);
	}

	public void postSearch(ResponseBuilder rb) {
		LOG.info("Executing postSearch in engine: " + engineName);
		CommandExecutor ksession = getSession();
		processRules(rb, ksession);
	}

	public void transformDocument(SolrDocument doc, int docId) {
		CommandExecutor ksession = getSession();
		List<Command<?>> cmds = new ArrayList<Command<?>>();
		Collection<Object> facts = new ArrayList<Object>();
		factCollector.addFacts(doc, docId, facts);
		execute(ksession, cmds, facts);
	}

	public void prepareDocument(AddUpdateCommand addUpCmd) {
		List<Command<?>> cmds = new ArrayList<Command<?>>();
		CommandExecutor ksession = getSession();
		Collection<Object> facts = new ArrayList<Object>();
		factCollector.addFacts(addUpCmd, facts);
		execute(ksession, cmds, facts);
	}
}
