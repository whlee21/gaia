package gaia.scheduler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusTriggerListener extends TriggerListenerSupport {
	private static transient Logger LOG = LoggerFactory.getLogger(StatusTriggerListener.class);

	Set<String> running = Collections.synchronizedSet(new HashSet<String>());

	public Set<String> getRunning() {
		return running;
	}

	public String getName() {
		return StatusTriggerListener.class.getName();
	}

	public boolean isRunning(String triggerName) {
		return running.contains(triggerName);
	}

	public void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext, int i) {
		running.remove(trigger.getKey().getName());
	}

	public void triggerFired(Trigger trigger, JobExecutionContext jobExecutionContext) {
		running.add(trigger.getKey().getName());
	}

	public void triggerMisfired(Trigger trigger) {
		LOG.warn("trigger missfired:" + trigger.getKey().getName());
		running.remove(trigger.getKey().getName());
	}
}
