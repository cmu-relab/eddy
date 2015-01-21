package eddy.lang;

/**
 * Describes a data category, which is a {@link RoleValue} in a {@link Role}.
 * 
 * @author Travis Breaux
 *
 */

public class Datum implements Comparable<Datum>,RoleValue {
	public final static Datum ANYTHING = new Datum("anything");
	public final String name;
	
	public Datum(String name) {
		this.name = name;
	}
	public void accept(RoleValueVisitor visitor) {
		visitor.visit(this);
	}
	public int compareTo(Datum d) {
		return name.compareTo(d.name);
	}
	public boolean equals(Datum d) {
		return compareTo(d) == 0;
	}
	public String getName() {
		return name;
	}
	public String toString() {
		return name;
	}
}
