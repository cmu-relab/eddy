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
	 * occurs over a shared interpretation (intersection); {@link #SUBSUMES} or {@link #SUBSUMED_BY}, 
	 * if the conflict occurs over a subsumption relation between two interpretations; 
	 * {@link #EQUIVALENT}, or if the conflict occurs over an equivalent interpetetation 
	 * between two rules.
	 * 
	 * @author Travis Breaux
	 *
	 */
	public enum Type {SHARED, SUBSUMES, SUBSUMED_BY, EQUIVALENT};
	public final Extension ext;
	public final Type type;
	public final Rule rule1, rule2;
	public final TreeMap<String,Action> actions = new TreeMap<String,Action>();
	
	public Conflict(Extension ext, Type type, Rule rule1, Rule rule2, String id, Action action) {
		this.ext = ext;
		this.actions.put(id, action);
		
		// sort the rules, so the exclusion / permission is rule1, in that order.
		if (rule2.modality.isExclusion() || rule2.modality.isPermissible()) {
			Rule rule3 = rule2;
			rule2 = rule1;
			rule1 = rule3;
			
			// if swapping rules under subsumption, reverse direction of relation
			if (type == Type.SUBSUMES) {
				type = Type.SUBSUMED_BY;
			}
		}

		this.rule1 = rule1;
		this.rule2 = rule2;
		this.type = type;
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