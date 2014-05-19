package gaia.parser.gaia;

import java.util.ArrayList;
import java.util.List;

class GaiaTokenizer {
	GaiaQueryParser parser = null;
	public GaiaToken token = null;
	public GaiaToken prevToken = null;
	public String string = null;
	public int si = -1;
	public List<GaiaToken> tokens = new ArrayList<GaiaToken>();
	int tokenCount = 0;
	int wordCount = 0;
	int ti = -1;

	public boolean opUp = false;

	public boolean notUp = true;

	public boolean natUp = false;

	public GaiaTokenizer(GaiaQueryParser parser) {
		this.parser = parser;
	}

	public void setOpUp(boolean b) {
		opUp = b;
	}

	public void setNotUp(boolean b) {
		notUp = b;
	}

	public void setNatUp(boolean b) {
		natUp = b;
	}

	public void setup(String str) {
		cleanup();
		string = (str == null ? "" : str);
		parseTokens();
	}

	public void cleanup() {
		string = null;
		si = -1;
		ti = -1;
		token = null;
		prevToken = null;
		tokens.clear();
		tokenCount = 0;
		wordCount = 0;
	}

	public void restart(GaiaToken t) {
		if (t == null)
			ti = -1;
		else
			ti = (t.ti - 1);
		token = null;
		prevToken = null;
	}

	public void restartSkip(GaiaToken t) {
		if (t == null)
			ti = -1;
		else
			ti = t.ti;
		token = null;
		prevToken = t;
	}

	public int parseTokens() {
		int ti = 0;
		while (true) {
			GaiaToken nextToken = parseToken();
			nextToken.ti = ti;
			tokens.add(nextToken);
			ti++;
			if (nextToken.isEnd)
				break;
			if (nextToken.isWord) {
				if (wordCount >= parser.maxTerms) {
					int endOffset = nextToken.startOffset;
					GaiaToken endToken = new GaiaToken("", endOffset, nextToken.startOffset, "end");
					endToken.isEnd = true;
					tokens.set(ti - 1, endToken);

					String oldQuery = string;
					String newQuery = oldQuery.substring(0, endOffset);
					GaiaQueryParser.LOG.warn("Query truncated since it exceeded limit of " + parser.maxTerms
							+ " terms. Truncated query: <<" + newQuery + ">>");

					break;
				}
				wordCount += 1;
			}
		}

		tokenCount = tokens.size();

		restart(null);

		return tokenCount;
	}

	private GaiaToken nextToken() {
		if (token != null)
			prevToken = token;
		token = null;

		if (ti < tokenCount - 1) {
			ti += 1;
		}
		if (ti >= 0) {
			token = ((GaiaToken) tokens.get(ti));
		}
		return token;
	}

	public GaiaToken getToken() {
		if (token == null) {
			nextToken();
		}
		GaiaToken t = token;
		prevToken = token;
		token = null;
		return t;
	}

	public GaiaToken peekToken() {
		if (token == null) {
			nextToken();
		}
		return token;
	}

	public String peekTokenText() {
		if (token == null) {
			nextToken();
		}
		return token.text;
	}

	public String peekTokenType() {
		if (token == null) {
			nextToken();
		}
		return token.type;
	}

	public String peekTokenTypeOp() {
		if (token == null) {
			nextToken();
		}
		return token.opType;
	}

	public void skipToken() {
		if (token == null) {
			nextToken();
		}
		prevToken = token;
		token = null;
	}

	private GaiaToken parseToken() {
		if (token != null)
			prevToken = token;
		token = null;
		boolean sawNonStar = false;
		boolean sawNonWild = false;
		boolean mixedWild = false;
		boolean sawQuestionMark = false;
		boolean isWild = false;
		boolean hasWildStarSuffix = false;
		boolean isAllStarWild = false;

		if (si == -1) {
			si = 0;
		}

		if (si == string.length()) {
			token = new GaiaToken("", si, si, "end");
			token.isEnd = true;
			return token;
		}

		int len = string.length();
		for (; si < len; si += 1) {
			char ch = string.charAt(si);

			ch = GaiaQueryParserUtils.mapUnicodePunctToAscii(ch);

			if (!Character.isWhitespace(ch)) {
				if (((ch == '+') || (ch == '-')) && (si < len - 1) && (Character.isDigit(string.charAt(si + 1)))) {
					int si1 = si + 2;
					while ((si1 < len) && (Character.isDigit(string.charAt(si1))))
						si1++;
					char ch1 = '\000';
					if (si1 < len) {
						ch1 = string.charAt(si1);
					}

					if ((ch1 != 0) && (",.)]}&|%~^".indexOf(ch1) < 0) && (!Character.isWhitespace(ch1))) {
						String str = Character.toString(ch);
						token = new GaiaToken(str, si, si + 1, str);
						si += 1;
						break;
					}
				} else {
					if ((ch == '(') || (ch == ')') || (ch == '+') || (ch == '-') || (ch == '[') || (ch == ']') || (ch == '{')
							|| (ch == '}') || (ch == ':') || (ch == '"') || (ch == '~') || (ch == '^')) {
						String str = Character.toString(ch);
						token = new GaiaToken(str, si, si + 1, str);
						si += 1;
						break;
					}
					if (ch == '&') {
						if ((si + 1 < string.length()) && (string.charAt(si + 1) == '&')) {
							String str = string.substring(si, si + 2);
							token = new GaiaToken(str, si, si + 2, str);
							si += 2;
							break;
						}
						String str = string.substring(si, si + 1);
						token = new GaiaToken(str, si, si + 1, "word");
						si += 1;

						break;
					}
					if (ch == '|') {
						if ((si + 1 < string.length()) && (string.charAt(si + 1) == '|')) {
							String str = string.substring(si, si + 2);
							token = new GaiaToken(str, si, si + 2, str);
							si += 2;
							break;
						}
						String str = string.substring(si, si + 1);
						token = new GaiaToken(str, si, si + 1, "word");
						si += 1;

						break;
					}
					if (ch == '!') {
						if ((si + 1 < string.length()) && (string.charAt(si + 1) == '=')) {
							String str = string.substring(si, si + 2);
							token = new GaiaToken(str, si, si + 2, str);
							token.isRelOp = true;
							si += 2;
							break;
						}
						String str = string.substring(si, si + 1);
						token = new GaiaToken(str, si, si + 1, str);
						si += 1;

						break;
					}
					if (ch == '<') {
						if ((si + 1 < string.length()) && (string.charAt(si + 1) == '=')) {
							String str = string.substring(si, si + 2);
							token = new GaiaToken(str, si, si + 2, str);
							si += 2;
						} else {
							String str = string.substring(si, si + 1);
							token = new GaiaToken(str, si, si + 1, str);
							si += 1;
						}
						token.isRelOp = true;
						break;
					}
					if (ch == '>') {
						if ((si + 1 < string.length()) && (string.charAt(si + 1) == '=')) {
							String str = string.substring(si, si + 2);
							token = new GaiaToken(str, si, si + 2, str);
							si += 2;
						} else {
							String str = string.substring(si, si + 1);
							token = new GaiaToken(str, si, si + 1, str);
							si += 1;
						}
						token.isRelOp = true;
						break;
					}
					if (ch == '=') {
						if ((si + 1 < string.length()) && (string.charAt(si + 1) == '=')) {
							String str = string.substring(si, si + 2);
							token = new GaiaToken(str, si, si + 2, str);
							token.isRelOp = true;
							si += 2;
							break;
						}
						String str = string.substring(si, si + 1);
						token = new GaiaToken(str, si, si + 1, str);
						si += 1;

						break;
					}

				}

				int si1 = si;

				String word = "";

				for (; si1 < len; si1++) {
					char ch1 = string.charAt(si1);

					ch1 = GaiaQueryParserUtils.mapUnicodePunctToAscii(ch1);

					char ch2 = '\000';
					if (si1 < string.length() - 1) {
						ch2 = string.charAt(si1 + 1);
					}

					ch2 = GaiaQueryParserUtils.mapUnicodePunctToAscii(ch2);

					if (ch1 == '\\') {
						si1++;
						if (si1 < string.length()) {
							if ((ch2 == 'u') || (ch2 == 'U')) {
								int ch3 = 0;
								for (int ui = 0; (ui < 4) && (si1 + 1 < string.length()); ui++) {
									ch1 = string.charAt(si1 + 1);
									int ch4 = 0;
									if ((ch1 >= '0') && (ch1 <= '9')) {
										ch4 = ch1 - '0';
									} else if ((ch1 >= 'a') && (ch1 <= 'f')) {
										ch4 = ch1 - 'a' + 10;
									} else {
										if ((ch1 < 'A') || (ch1 > 'F'))
											break;
										ch4 = ch1 - 'A' + 10;
									}

									ch3 = ch3 * 16 + ch4;
									si1++;
								}

								ch2 = (char) ch3;
							} else if ((ch2 == '*') || (ch2 == '?')) {
								mixedWild = true;
								sawNonStar = true;
							}

							if (ch2 != 0)
								word = word + ch2;
						}
					} else if (ch1 == ':') {
						if (((si1 == 0) || (si1 >= string.length() - 1) || (!Character.isDigit(string.charAt(si1 - 1)))
								|| (!Character.isDigit(string.charAt(si1 + 1))) || (!Character.isDigit(word.charAt(0))))
								&& (!word.equalsIgnoreCase("http"))
								&& (!word.equalsIgnoreCase("https"))
								&& (!word.equalsIgnoreCase("ftp"))) {
							break;
						}
						word = word + ch1;
					} else {
						if ((Character.isWhitespace(ch1)) || (ch1 == '(') || (ch1 == ')') || (ch1 == '"') || (ch1 == '~')
								|| (ch1 == '^') || (ch1 == '{') || (ch1 == '}') || (ch1 == '[') || (ch1 == ']') || (ch1 == '<')
								|| (ch1 == '>') || (ch1 == '=') || ((ch1 == '|') && (ch2 == '|')) || ((ch1 == '&') && (ch2 == '&'))) {
							break;
						}

						if (ch1 == '?') {
							int nt = tokens.size();
							if ((ch2 == 0) && (parser.minStripTrailQMark != -1) && (nt >= parser.minStripTrailQMark - 1)) {
								GaiaToken t1 = null;
								GaiaToken t2 = null;
								GaiaToken t3 = null;
								if (nt >= 1) {
									t1 = (GaiaToken) tokens.get(0);
									if (nt >= 2) {
										t2 = (GaiaToken) tokens.get(1);
										if (nt >= 3) {
											t3 = (GaiaToken) tokens.get(2);
										}
									}
								}

								if ((t2 != null) && (t2.opType.equals(":"))) {
									if (nt - 2 >= parser.minStripTrailQMark - 1) {
										t1 = t3;
									} else
										t1 = null;
								}
								if ((t1 != null) && (t1.type.equals("word"))) {
									String text = t1.text;
									if ((text.equalsIgnoreCase("who")) || (text.equalsIgnoreCase("what"))
											|| (text.equalsIgnoreCase("when")) || (text.equalsIgnoreCase("where"))
											|| (text.equalsIgnoreCase("why")) || (text.equalsIgnoreCase("how"))) {
										if (!parser.logOutput)
											continue;
										GaiaQueryParser.LOG.info("Ignoring trailing question mark for natural language question: <<"
												+ string + ">>");
										continue;
									}
								}

							}

							if ((sawNonWild) || (isAllStarWild))
								mixedWild = true;
							isWild = true;
							sawNonStar = true;
							hasWildStarSuffix = false;
							isAllStarWild = false;
							sawQuestionMark = true;
						} else if (ch1 == '*') {
							if (!sawNonStar)
								isAllStarWild = true;
							if ((sawNonStar) && (!mixedWild) && (!sawQuestionMark))
								hasWildStarSuffix = true;
							isWild = true;
						} else {
							if (isWild)
								mixedWild = true;
							hasWildStarSuffix = false;
							isAllStarWild = false;
							sawNonStar = true;
							sawNonWild = true;
						}
						word = word + ch1;
					}

				}

				String opType;
				if (word.length() != 0) {
					opType = "word";
					if (word.equals("AND"))
						opType = "&&";
					else if ((!opUp) && (!natUp) && (word.equalsIgnoreCase("and")))
						opType = "&&";
					else if ((natUp) && (prevToken != null) && (!prevToken.type.equals("word")) && (word.equalsIgnoreCase("and")))
						opType = "&&";
					else if (word.equals("OR"))
						opType = "||";
					else if ((!opUp) && (!natUp) && (word.equalsIgnoreCase("or")))
						opType = "||";
					else if ((natUp) && (prevToken != null) && (!prevToken.type.equals("word")) && (word.equalsIgnoreCase("or")))
						opType = "||";
					else if (word.equals("NOT"))
						opType = "!";
					else if ((!notUp) && (!opUp) && (!natUp) && (word.equalsIgnoreCase("not")))
						opType = "!";
					else if ((natUp) && (!notUp) && (prevToken != null) && (!prevToken.isWord) && (word.equalsIgnoreCase("not")))
						opType = "!";
					else if (word.equals("NEAR"))
						opType = "near";
					else if ((!opUp) && (word.equalsIgnoreCase("near")))
						opType = "near";
					else if (word.equals("AFTER"))
						opType = "after";
					else if ((!opUp) && (word.equalsIgnoreCase("after")))
						opType = "after";
					else if (word.equals("BEFORE"))
						opType = "before";
					else if ((!opUp) && (word.equalsIgnoreCase("before")))
						opType = "before";
					else if (word.equals("TO"))
						opType = "to";
					else if ((!opUp) && (word.equalsIgnoreCase("to"))) {
						opType = "to";
					}

					token = new GaiaToken(word, si, si1, "word", opType);
					token.isWord = true;
					if (isWild) {
						token.isWild = isWild;
						token.hasWildStarSuffix = hasWildStarSuffix;
						token.isAllStarWild = isAllStarWild;
					}
				}
				si = si1;
				break;
			}
		}

		if (token == null) {
			token = new GaiaToken("", si, si, "end");
			token.isEnd = true;
		}

		return token;
	}

	public GaiaToken parseQuotedStringToken() {
		String startText = peekTokenText();
		if (!startText.equals("\"")) {
			return null;
		}

		int si0 = token.startOffset;

		String text = "";

		int n = string.length();
		for (si = (si0 + 1); si < n; si += 1) {
			char ch = string.charAt(si);

			if (ch == '"') {
				si += 1;
				break;
			}

			if (ch == '\\') {
				si += 1;
				if (si >= n) {
					break;
				}
				char ch2 = string.charAt(si);
				if (si < string.length()) {
					if ((ch2 == 'u') || (ch2 == 'U')) {
						int ch3 = 0;
						for (int ui = 0; (ui < 4) && (si + 1 < string.length()); ui++) {
							ch = string.charAt(si + 1);
							int ch4 = 0;
							if ((ch >= '0') && (ch <= '9')) {
								ch4 = ch - '0';
							} else if ((ch >= 'a') && (ch <= 'f')) {
								ch4 = ch - 'a' + 10;
							} else {
								if ((ch < 'A') || (ch > 'F'))
									break;
								ch4 = ch - 'A' + 10;
							}

							ch3 = ch3 * 16 + ch4;
							si += 1;
						}

						ch2 = (char) ch3;
					}

					if (ch2 != 0)
						text = text + ch2;
				}
			} else {
				text = text + ch;
			}

		}

		int ti1 = token.ti;
		token = new GaiaToken(text, si0, si, "string");
		token.ti = ti1;
		tokens.set(ti1, token);
		ti = ti1;

		int i1 = tokenCount;
		for (int i = ti + 1; i < i1; i++) {
			GaiaToken tok = (GaiaToken) tokens.get(ti + 1);
			if (si <= tok.startOffset)
				break;
			tokens.remove(ti + 1);
			tokenCount -= 1;
			if (tok.type.equals("word")) {
				wordCount -= 1;
			}

		}

		for (int i = ti + 1; i < tokenCount; i++) {
			GaiaToken tok = (GaiaToken) tokens.get(i);
			tok.ti = i;
		}

		return token;
	}

	public char getCharAt(int i) {
		if ((string == null) || (i < 0) || (i >= string.length()))
			return '\000';
		return string.charAt(i);
	}

	public boolean isSpaceAfter() {
		if (token == null)
			peekToken();
		return isSpaceAfter(token);
	}

	public boolean isSpaceAfter(GaiaToken t) {
		if (t == null)
			return false;
		char ch = getCharAt(t.endOffset);
		return ch == ' ';
	}

	public boolean isSpaceBefore() {
		if (token == null)
			peekToken();
		return isSpaceBefore(token);
	}

	public boolean isSpaceBefore(GaiaToken t) {
		if (t == null)
			return false;
		char ch = getCharAt(t.startOffset - 1);
		return ch == ' ';
	}
}
