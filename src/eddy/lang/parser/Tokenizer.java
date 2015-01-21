package eddy.lang.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Tokenizes policy text into symbols for parsing in a {@link Parser} object.
 * 
 * @author Travis Breaux
 *
 */

public class Tokenizer {
	private final TreeMap<String,Integer> wordTypes = new TreeMap<String,Integer>();
	
	private final static String[] symbol = new String[] {
		"EOF", 
		"NEWLINE", 
		"TAB", 
		"COMMA",
		"ACTION", 
		"ROLE", 
		"WORD", 
		"EQUALS", 
		"LESS", 
		"LESS_EQ", 
		"GREATER", 
		"GREATER_EQ",
		"TILDE", 
		"BACKSLASH",
		"PLUS"
	};	
	public Tokenizer() {
		wordTypes.put("COLLECT", Token.ACTION);
		wordTypes.put("TRANSFER", Token.ACTION);
		wordTypes.put("RETAIN", Token.ACTION);
		wordTypes.put("USE", Token.ACTION);
		wordTypes.put("COMPUTE", Token.ACTION);
		wordTypes.put("MERGE", Token.ACTION);
		wordTypes.put("CREATE", Token.ACTION);
		wordTypes.put("FOR", Token.ROLE);
		wordTypes.put("TO", Token.ROLE);
		wordTypes.put("FROM", Token.ROLE);
		wordTypes.put("WHERE", Token.ROLE);
		wordTypes.put("USING", Token.ROLE);
	}
	
	public ArrayList<Token> tokenize(Reader reader) throws ParseException {
		ArrayList<Token> tokens = new ArrayList<Token>();
		StringBuffer buffer = new StringBuffer();
		Token token = null;
		int line = 1, ch;
		int mark = -1;
		char c;
		
		try {
			while ((ch = reader.read()) > -1) {
				//This will result in a new line being interpreted as two new lines in windows 
				//but for this language it doesn't matter 
				if(ch=='\r')
					ch='\n';
				
				c = (char) ch;
				
				if (mark > -1) {
					switch (c) {
						case '\n':
						
							if (buffer.charAt(mark) == '#') {
								// this is a line comment
								buffer.replace(mark, buffer.length(), "");
								mark = -1;
								token = new Token(Token.NEWLINE, System.getProperty("line.seperator"), line);
							}
							break;
						case '/':
							if (buffer.charAt(buffer.length() - 1) == '*') {
								// this is a block comment
								buffer.replace(mark, buffer.length(), "");
								mark = -1;
							}
							break;
						default:
							buffer.append(c);
					}
				}
				else {
					switch (c) {
						case ' ':
							if (buffer.length() > 0) {
								if (wordTypes.containsKey(buffer.toString())) {
									tokens.add(new Token(wordTypes.get(buffer.toString()), buffer.toString(), line));
								}
								else {
									tokens.add(new Token(Token.WORD, buffer.toString(), line));
								}
								buffer.setLength(0);
							}
							break;
						case '\t':
							token = new Token(Token.TAB, "\\t", line);
							break;
						case '#':
							mark = buffer.length();
							buffer.append(c);
							break;
						case '\n':
							token = new Token(Token.NEWLINE, "\\n", line);
							break;
						case ',':
							token = new Token(Token.COMMA, ",", line);
							break;
						case '=':
							int lastType = tokens.get(tokens.size() - 1).type;
							if (lastType == Token.LESS) {
								tokens.remove(tokens.size() - 1);
								token = new Token(Token.LESS_EQ, "<=", line);
							}
							else if (lastType == Token.GREATER) {
								tokens.remove(tokens.size() - 1);
								token = new Token(Token.GREATER_EQ, ">=", line);
							}
							else {
								token = new Token(Token.EQUALS, "=", line);
							}
							break;
						case '<':
							token = new Token(Token.LESS, "<", line);
							break;
						case '>':
							token = new Token(Token.GREATER, ">", line);
							break;
						case '~':
							token = new Token(Token.TILDE, "~", line);
							break;
						case '+':
							token = new Token(Token.PLUS, "+", line);
							break;
						case '\\':
							token = new Token(Token.BACKSLASH, "\\", line);
							break;
						case '*':
							if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '/') {
								mark = buffer.length() - 1;
								buffer.append(c);
							}
							break;
						default:
							buffer.append(c);
					}
				}
				if (token != null && mark < 0) {
					if (buffer.length() > 0) {
						if (wordTypes.containsKey(buffer.toString())) {
							tokens.add(new Token(wordTypes.get(buffer.toString()), buffer.toString(), line));
						}
						else {
							tokens.add(new Token(Token.WORD, buffer.toString(), line));
						}
						buffer.setLength(0);
					}
					if (token.type == Token.NEWLINE) {
						line++;
						// remove leading tabs
						for (int i = tokens.size() - 1; i >= 0; i--) {
							if (tokens.get(i).type == Token.TAB) {
								tokens.remove(i);
							}
							else {
								break;
							}
						}
					}
					tokens.add(token);
					token = null;
				}
			}
		} catch (IOException e) {
			throw new ParseException("Error: Cannot tokenize input from reader");
		}
		
		// consume any remaining buffer content
		if (buffer.length() > 0) {
			if (wordTypes.containsKey(buffer.toString())) {
				tokens.add(new Token(wordTypes.get(buffer.toString()), buffer.toString(), line));
			}
			else {
				tokens.add(new Token(Token.WORD, buffer.toString(), line));
			}
			buffer.setLength(0);
		}
		
		int last = tokens.size() - 1;
		while (tokens.get(last).type == Token.NEWLINE || tokens.get(last).type == Token.TAB) {
			tokens.remove(last);
			last--;
		}
		tokens.add(new Token(Token.NEWLINE, "\\n", line));
		tokens.add(new Token(Token.EOF, "", line));
		return tokens;
	}
	
	/**
	 * Describes a tokenized policy fragment obtained by the {@link Tokenizer}. Each token
	 * is parsed independenlty or in sequence by a {@link Parser}.
	 * 
	 * @author Travis Breaux
	 *
	 */
	public static class Token {
		public final static int EOF = 0;
		public final static int NEWLINE = 1;
		public final static int TAB = 2;
		public final static int COMMA = 3;
		public final static int ACTION = 4;
		public final static int ROLE = 5;
		public final static int WORD = 6;
		public final static int EQUALS = 7;
		public final static int LESS = 8;
		public final static int LESS_EQ = 9;
		public final static int GREATER = 10;
		public final static int GREATER_EQ = 11;
		public final static int TILDE = 12;
		public final static int BACKSLASH = 13;
		public final static int PLUS = 14;
		public final String text;
		public final int type;
		public final int line;
		
		public Token(int type, String text, int line) {
			this.type = type;
			this.text = text;
			this.line = line;
		}
		
		public boolean matches(int type, String text) {
			return this.type == type && this.text.equals(text);
		}
		
		public String toString() {
			return symbol[type] + "(" + text + ")";
		}
	}
}
