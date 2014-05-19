package gaia.rules.drools.stateless;

import gaia.rules.drools.BaseDroolsRulesEngine;

import java.util.Collection;
import java.util.List;

import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.CommandExecutor;

public class StatelessDroolsRulesEngine extends BaseDroolsRulesEngine {
	protected CommandExecutor getSession() {
		return createStatelessSession();
	}

	protected void execute(CommandExecutor ksession, List<Command<?>> cmds, Collection<Object> facts) {
		cmds.add(CommandFactory.newInsertElements(facts));
		Command batchCmd = CommandFactory.newBatchExecution(cmds);
		ksession.execute(batchCmd);
	}
}
