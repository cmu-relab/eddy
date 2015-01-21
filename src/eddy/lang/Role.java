package eddy.lang;

/**
 * Describes a mapping from a {@link Action} to a {@link RoleValue}. This includes
 * the object, source, purpose, target, etc. of an action.
 * 
 * @author Travis Breaux
 *
 */

public class Role implements Cloneable,Comparable<Role> {
	/**
	 * Describes the type of the role. This includes one of: {@link #OBJECT}, {@link #SOURCE},
	 * {@link #TARGET} or {@link #PURPOSE}.
	 * 
	 * @author Travis Breaux
	 *
	 */
	public static enum Type { OBJECT, SOURCE, PURPOSE, TARGET, INSTRUMENT };
	public final String prefix;
	public final RoleValueSet values;
	public final Type type;
	
	public Role(Type type, String prefix, RoleValueSet values) {
		this.type = type;
		this.prefix = prefix;
		this.values = values;
	}
	public int compareTo(Role role) {
		return type.compareTo(role.type);
	}
	public boolean equals(Role role) {
		return compareTo(role) == 0;
	}
	public RoleValue getRangeRestriction() {
		switch (type) {
			case OBJECT:
				return Datum.ANYTHING;
			case SOURCE:
				return Actor.ANYONE;
			case PURPOSE:
				return Purpose.ANYTHING;
			case TARGET:
				return Actor.ANYONE;
			case INSTRUMENT:
				return null;
		}
		return null;
	}
	public Role clone() {
		return new Role(type, prefix, values.clone());
	}
	public String toString() {
		if (prefix != null && prefix.length() > 0) {
			return prefix + " " + values;
		}
		else {
			return "" + values;
		}
	}
}
