package eddy.lang.parser;

/**
 * Provides exception handling due to parser and compiler errors at runtime.
 * 
 * @author Travis Breaux
 *
 */

public class ParseException extends Exception {
	public final static long serialVersionUID = 1;
	public int line;
	
	public ParseException(String s) {
		super(s);
	}
	
	public ParseException(String s, int line) {
		super(s);
		this.line = line;
	}
	
	public String getMessage() {
		if (line > 0) {
			return super.getMessage() + " on line " + line;
		} else {
			return super.getMessage();
		}
	}
}
