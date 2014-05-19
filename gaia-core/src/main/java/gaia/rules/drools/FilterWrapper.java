package gaia.rules.drools;

import org.apache.lucene.search.Query;

public class FilterWrapper {
	private Query filter;

	public FilterWrapper(Query filter) {
		this.filter = filter;
	}

	public Query getFilter() {
		return filter;
	}

	public void setFilter(Query filter) {
		this.filter = filter;
	}
}
