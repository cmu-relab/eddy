package eddy.lang.analysis;

import java.util.TreeMap;

import eddy.lang.Action;
import eddy.lang.Rule;

/**
 * Describes a conflict between two rules. Conflicts are the analytic result of 
 * the {@link ConflictAnalyzer}.
 * 
 * @author Travis Breaux
 *
 */

public class Conflict implements Comparable<Conflict> {
	/**
	 * Describes the type of conflict. The type is one of: {@link #SHARED}, if the conflict
	 * occurs over a shared interpretation (intersection); {@link #DIRECTED}, if the conflict
	 * occurs over a subsumption relation between two interpretations; {@link #EQUIVALENT},
	 * if the conflict occurs over an equivalent interpetetation between two rules; and
	 * {@link #EXTENDED}, if the conflict is an interpretation that exists outside a limitation.
	 * For example, if a limitation principle is applied and an action is permitted in excess
	 * of the set of limiting rights, this action would be reported as a conflict with a
	 * type {@link EXTENDED}.
	 * 
	 * @author Travis Breaux
	 *
	 */
	public enum Type {SHARED, DIRECTED, EQUIVALENT, EXTENDED};
	public final Type type;
	public final Rule rule1, rule2;
	public final TreeMap<String,Action> actions = new TreeMap<String,Action>();
	
	public Conflict(Type type, Rule rule1, Rule rule2, String id, Action action) {
		this.type = type;
		this.rule1 = rule1;
		this.rule2 = rule2;
		this.actions.put(id, action);
	}
	
	public Conflict(Type type, Rule rule, String id, Action action) {
		this.type = type;
		this.rule1 = rule;
		this.rule2 = rule;
		this.actions.put(id, action);
	}
	
	public int compareTo(Conflict c) {
		int x = rule1.id.compareTo(c.rule1.id);
		if (x != 0) {
			return x;
		}
		return rule2.id.compareTo(c.rule2.id);
	}
	
	public boolean equals(Conflict c) {
		return compareTo(c) == 0;
	}
	
	public String toString() {
		String s = type + " " + rule1.id + "," + rule2.id;
		if (type == Type.SHARED) {
			return s + " at " + actions.keySet().toString();
		}
		return s;
	}
}