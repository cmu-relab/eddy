package eddy.lang;

/**
 * Describes an actor category, which is a {@link RoleValue} in a {@link Role}.
 * 
 * @author Travis Breaux
 *
 */

public class Actor implements Comparable<Actor>,RoleValue {
	public final static Actor ANYONE = new Actor("anyone");
	public final String name;
	
	public Actor(String name) {
		this.name = name;
	}
	public void accept(RoleValueVisitor visitor) {
		visitor.visit(this);
	}
	public int compareTo(Actor a) {
		return name.compareTo(a.name);
	}
	public boolean equals(Actor a) {
		return compareTo(a) == 0;
	}
	public String toString() {
		return name;
	}
}
