package eddy.lang;

import java.util.ArrayList;

/**
 * Describes a mapping from the action {@link name} to {@link Role} objects. Each role
 * is a relationship between the action and another domain concept, such as an {@link Actor},
 * {@link Datum}, or {@link Purpose}.
 * 
 * @author Travis Breaux
 */

public class Action implements Cloneable,Comparable<Action> {
	private final ArrayList<Role> roles = new ArrayList<Role>();
	public final String name;
	
	public Action(String name) {
		this.name = name;
	}
	public int compareTo(Action a) {
		return name.compareTo(a.name);
	}
	public boolean equals(Action a) {
		return compareTo(a) == 0;
	}
	public void add(Role role) {
		for (int i = 0; i < roles.size(); i++) {
			if (roles.get(i).equals(role)) {
				roles.remove(i);
				roles.add(i, role);
				return;
			}
		}
		roles.add(role);
	}
	public Action clone() {
		Action a = new Action(name);
		for (int i = 0; i < roles.size(); i++) {
			a.roles.add(roles.get(i).clone());
		}
		return a;
	}
	
	public Role getRole(Role.Type type) {
		for (int i = 0; i < roles.size(); i++) {
			if (roles.get(i).type.equals(type)) {
				return roles.get(i);
			}
		}
		return null;
	}
	public Role[] roles() {
		return roles.toArray(new Role[roles.size()]);
	}
	
	public String toString() {
		StringBuffer s = new StringBuffer(name);
		
		for (int i = 0; i < roles.size(); i++) {
			s.append(" ");
			s.append(roles.get(i).toString());
		}
		return s.toString();
	}
}
