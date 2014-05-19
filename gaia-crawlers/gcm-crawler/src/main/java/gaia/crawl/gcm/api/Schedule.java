package gaia.crawl.gcm.api;

public class Schedule {
	private int version;
	private boolean disabled = false;
	private String name;
	private Integer load;
	private Long retryDelay;
	private String timeIntervals;

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public Schedule(Boolean disabled, Integer load, Long retryDelay, String timeIntervals) {
		setDisabled(disabled.booleanValue());
		setLoad(load);
		setRetryDelay(retryDelay);
		setTimeIntervals(timeIntervals);
	}

	public Schedule(String fromString) {
		setFromString(fromString);
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getLoad() {
		return load;
	}

	public void setLoad(Integer load) {
		this.load = load;
	}

	public Long getRetryDelay() {
		return retryDelay;
	}

	public void setRetryDelay(Long retryDelay) {
		this.retryDelay = retryDelay;
	}

	public String getTimeIntervals() {
		return timeIntervals;
	}

	public void setTimeIntervals(String timeIntervals) {
		this.timeIntervals = timeIntervals;
	}

	public String toString() {
		return new StringBuilder().append(disabled ? "#" : "").append(name).append(":")
				.append(load != null ? Integer.toString(load.intValue()) : "").append(":")
				.append(load != null ? Long.toString(retryDelay.longValue()) : "").append(":")
				.append(timeIntervals).toString();
	}

	public void setFromString(String scheduleString) {
		String[] parts = scheduleString.split(":");
		if (parts.length > 1)
			setLoad(Integer.valueOf(Integer.parseInt(parts[1])));
		if (parts.length > 2)
			setRetryDelay(Long.valueOf(Long.parseLong(parts[2])));
		setDisabled(parts[0].startsWith("#"));
		setName(disabled ? parts[0].substring(1) : parts[0]);
		String timeIntervals = "";
		for (int i = 3; i < parts.length - 1; i++) {
			timeIntervals = new StringBuilder().append(timeIntervals).append(parts[i]).append(":").toString();
		}
		if (parts.length > 3) {
			timeIntervals = new StringBuilder().append(timeIntervals).append(parts[(parts.length - 1)]).toString();
		}
		setTimeIntervals(timeIntervals);
	}
}
