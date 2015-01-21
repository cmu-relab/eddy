package eddy.lang;

/**
 * Provides a purpose category, which is a {@link RoleValue} in a {@link Role}.
 * 
 * @author Travis Breaux
 *
 */

public class Purpose implements Comparable<Purpose>,RoleValue {
	public final static Purpose ANYTHING = new Purpose("anything");
	public final String name;
	
	public Purpose(String name) {
		this.name = name;
	}
	public void accept(RoleValueVisitor visitor) {
		visitor.visit(this);
	}
	public int compareTo(Purpose p) {
		return name.compareTo(p.name);
	}
	public boolean equals(Purpose p) {
		return compareTo(p) == 0;
	}
	public String toString() {
		return name;
	}
}
