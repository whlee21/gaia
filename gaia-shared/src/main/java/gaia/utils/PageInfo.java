package gaia.utils;

public class PageInfo {
	private long start;
	private int resultsPerPage;
	private long resultsFound;
	private int pageCount;
	private int currentPageNumber;

	public PageInfo(long resultsFound, int resultsPerPage, long start) {
		this.resultsFound = resultsFound;
		this.resultsPerPage = resultsPerPage;
		this.start = start;

		pageCount = ((int) Math.ceil(resultsFound / resultsPerPage));
		currentPageNumber = ((int) Math.ceil(start / resultsPerPage + (pageCount > 0 ? 1 : 0)));
	}

	public long getLower() {
		return start + 1L;
	}

	public long getUpper() {
		long upper = Math.min(resultsFound, start + Math.min(resultsFound, resultsPerPage));
		return upper;
	}

	public int getCurrentPagePlus(int plus) {
		return Math.min(currentPageNumber + plus, pageCount);
	}

	public int getCurrentPageMinus(int minus) {
		return Math.max(currentPageNumber - minus, 1);
	}

	public int getCurrentPageNumber() {
		return currentPageNumber;
	}

	public int getPageCount() {
		return pageCount;
	}

	public long getResultsFound() {
		return resultsFound;
	}

	public int getResultsPerPage() {
		return resultsPerPage;
	}

	public long getStart() {
		return start;
	}

	public int startForPage(int page) {
		int p = page;
		if (page > pageCount)
			page = pageCount;
		return (p - 1) * resultsPerPage;
	}
}
