package gaia.crawl.gcm.api;

public class ConnectorStatus extends CMResponse {
	private String name;
	private String type;
	private int status;
	private Schedule schedule;

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public ConnectorStatus() {
	}

	public ConnectorStatus(String name, String type, int status, Schedule schedule) {
		this.name = name;
		this.type = type;
		this.status = status;
		this.schedule = schedule;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public int getStatus() {
		return this.status;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}
}
