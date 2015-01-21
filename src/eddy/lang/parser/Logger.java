package eddy.lang.parser;

import java.io.PrintWriter;

/**
 * Provides simple logging facilities across parser, compiler and analysis classes.
 * 
 * @author Travis Breaux
 *
 */

public class Logger {
	public final static int VERBOSE = 0;
	public final static int DEBUG = 1;
	public final static int WARN = 2;
	public final static int INFO = 3;
	public final static int NONE = Integer.MAX_VALUE;
	private int logLevel = WARN;
	private PrintWriter out;
	private String prefix = "";
	
	public Logger(PrintWriter out) {
		this.out = out;
	}
	
	public Logger(PrintWriter out, int level) {
		this.out = out;
		this.logLevel = level;
	}
	
	public Logger(PrintWriter out, int level, String prefix) {
		this.out = out;
		this.logLevel = level;
		this.prefix = prefix;
	}

	public void log(int level, String msg) {
		if (level >= logLevel) {
			out.println(prefix + msg);
			out.flush();
		}
	}
	
	public void setLogLevel(int level) {
		logLevel = level;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
