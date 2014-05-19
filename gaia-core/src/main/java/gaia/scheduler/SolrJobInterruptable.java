package gaia.scheduler;

public class SolrJobInterruptable implements Interruptable {
	private boolean interrupted;

	public synchronized boolean isInterrupted() {
		return interrupted;
	}

	public synchronized void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}
}
