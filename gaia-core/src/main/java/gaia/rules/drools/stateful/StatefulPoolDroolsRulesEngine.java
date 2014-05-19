package gaia.rules.drools.stateful;

import gaia.rules.drools.BaseDroolsRulesEngine;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulPoolDroolsRulesEngine extends BaseDroolsRulesEngine {
	private static final Logger LOG = LoggerFactory.getLogger(StatefulPoolDroolsRulesEngine.class);
	private static final String POOL_SIZE = "poolSize";
	private static final int MAX_SESSIONS_DEFAULT = 10;
	protected BlockingQueue<StatefulKnowledgeSession> ksessions;

	public void init(String engineName, NamedList args, SolrCore core) throws Exception {
		super.init(engineName, args, core);
		Integer poolSize = (Integer) args.get(POOL_SIZE);
		if (poolSize == null) {
			poolSize = Integer.valueOf(MAX_SESSIONS_DEFAULT);
		}
		LOG.info("Creating a pool with '" + poolSize + "' sessions for rules engine '" + engineName + "'");
		ksessions = new ArrayBlockingQueue<StatefulKnowledgeSession>(poolSize.intValue());
		for (int i = 0; i < poolSize.intValue(); i++)
			ksessions.add(kbase.newStatefulKnowledgeSession());
	}

	protected CommandExecutor getSession() {
		try {
			StatefulKnowledgeSession ksession = (StatefulKnowledgeSession) ksessions.poll(60L, TimeUnit.SECONDS);
			if (ksession != null) {
				return ksession;
			}
			throw new RuntimeException("Unnable to get a session");
		} catch (InterruptedException e) {
			throw new RuntimeException("Unnable to get a session", e);
		}
	}

	protected void execute(CommandExecutor ksession, List<Command<?>> cmds, Collection<Object> facts) {
		cmds.add(CommandFactory.newInsertElements(facts, "allfacts", false, null));

		cmds.add(CommandFactory.newFireAllRules());
		Command batchCmd = CommandFactory.newBatchExecution(cmds);
		StatefulKnowledgeSession sksession = (StatefulKnowledgeSession) ksession;
		sksession.execute(batchCmd);
		// Iterable handles = sksession.getFactHandles();
		for (FactHandle handle : sksession.getFactHandles()) {
			if (handle != null) {
				sksession.retract(handle);
			}
		}
		assert (sksession.getObjects().isEmpty());
		assert (sksession.getFactHandles().isEmpty());
		ksessions.add(sksession);
	}

	public void close() throws IOException {
		super.close();
		for (StatefulKnowledgeSession ksession : ksessions)
			ksession.dispose();
	}
}
