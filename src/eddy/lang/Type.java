package eddy.lang;

/**
 * Describes a conceptual relationship between two or more {@link RoleValue}s. This includes whether
 * the concepts are {@link Actor}, {@link Datum}, or {@link Purpose}, and whether the
 * left-hand concept subsumes, is subsumed by, is disjoint from, or is equivalent to the
 * concepts on the right-hand side.
 * 
 * @author Travis Breaux
 */

public class Type {
	public static final int SUBCLASS = 0;
	public static final int SUPERCLASS = 1;
	public static final int DISJOINT = 2;
	public static final int EQUIVALENT = 3;
	public static final int CLASS_ACTOR = 0;
	public static final int CLASS_DATUM = 1;
	public static final int CLASS_PURPOSE = 2;
	private static final String[] labelOp = {"<", ">", "\\"};
	private static final String[] labelClass = new String[] { "A", "D", "P" };

	public final int type;
	public final String lhs;
	public final String[] rhs;
	public final int op;
	
	
	public Type(int type, String lhs, int op, String[] rhs) {
		this.type = type;
		this.lhs = lhs;
		this.op = op;
		this.rhs = rhs;
	}
	
	public String toString() {
		String s = labelClass[type] + " " + lhs.toString() + " " + labelOp[op] + " " + rhs[0];
		
		for (int i = 1; i < rhs.length; i++) {
			s += "," + rhs[i];
		}
		return s;
	}
}
