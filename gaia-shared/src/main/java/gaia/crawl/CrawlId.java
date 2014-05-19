package gaia.crawl;

import gaia.crawl.datasource.DataSourceId;

public class CrawlId {
	private String id;

	public CrawlId(String id) {
		this.id = id;
	}

	public CrawlId(DataSourceId dsId) {
		id = dsId.toString();
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (id == null ? 0 : id.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrawlId other = (CrawlId) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String toString() {
		return id;
	}
}
