package gaia.crawl.gcm.api;

import java.util.ArrayList;
import java.util.List;

public class ConnectorInstance extends ConnectorType {
	private String name;
	private int status;
	private List<Schedule> schedules = new ArrayList<Schedule>();

	public List<Schedule> getSchedules() {
		return this.schedules;
	}

	public void setSchedules(ArrayList<Schedule> schedules) {
		this.schedules = schedules;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String toString() {
		return super.toString() + ":" + this.name;
	}
}
