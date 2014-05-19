package gaia.parser.gaia;

import java.util.ArrayList;

public class ParserFieldList extends ArrayList<ParserField> {
	private static final long serialVersionUID = 1L;
	public boolean sort = true;

	public ParserFieldList() {
		sort = true;
	}

	public ParserFieldList(boolean sort) {
		this.sort = sort;
	}

	public boolean add(ParserField pf) {
		if (sort) {
			int n = size();
			for (int i = 0; i < n; i++) {
				ParserField pf1 = (ParserField) get(i);
				int compare = pf.fieldName().compareTo(pf1.fieldName());
				if (compare == 0) {
					return true;
				}
				if (compare < 0) {
					add(i, pf);
					return true;
				}
			}

		}

		return super.add(pf);
	}
}
