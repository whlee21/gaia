package gaia.parser.gaia;

class GaiaToken {
	public int ti = -1;
	public int startOffset = 0;
	public int endOffset = 0;
	public String type = "";
	public String opType = "";
	public String text = "";
	public boolean isEnd = false;
	public boolean isWord = false;
	public boolean isRelOp = false;
	public boolean isWild = false;
	public boolean hasWildStarSuffix = false;
	public boolean isAllStarWild = false;

	public GaiaToken() {
	}

	public GaiaToken(String text, int startOffset, int endOffset, String type, String opType) {
		this.text = text;
		this.type = type;
		this.opType = opType;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	public GaiaToken(String text, int startOffset, int endOffset, String type) {
		this.text = text;
		this.type = type;
		this.opType = type;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	public boolean isAlpha() {
		if (text == null) {
			return false;
		}
		int n = text.length();
		for (int i = 0; i < n; i++)
			if (!Character.isLetter(text.charAt(i)))
				return false;
		return true;
	}

	public boolean isAlphaNumeric() {
		if (text == null) {
			return false;
		}
		int n = text.length();
		for (int i = 0; i < n; i++)
			if (!Character.isLetterOrDigit(text.charAt(i)))
				return false;
		return true;
	}
}
