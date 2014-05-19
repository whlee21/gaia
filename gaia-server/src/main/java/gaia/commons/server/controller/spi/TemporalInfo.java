package gaia.commons.server.controller.spi;

public interface TemporalInfo {

	/**
	 * Get the start of the requested time range. The time is given in seconds
	 * since the Unix epoch.
	 * 
	 * @return the start time in seconds
	 */
	Long getStartTime();

	/**
	 * Get the end of the requested time range. The time is given in seconds
	 * since the Unix epoch.
	 * 
	 * @return the end time in seconds
	 */
	Long getEndTime();

	/**
	 * Get the requested time between each data point of the temporal data. The
	 * time is given in seconds.
	 * 
	 * @return the step time in seconds
	 */
	Long getStep();
}
