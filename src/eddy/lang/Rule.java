package eddy.lang;

/**
 * Describes a rule, consisting of a {@link Modality} and {@link Action} object. Each rule is
 * part of a {@link Policy}, which contains multiple rules.
 * 
 * @author Travis Breaux
 */

public class Rule implements Comparable<Rule> {
	public final Modality modality;
	public final Action action;
	public final boolean only;
	public final String id;
	
	public Rule(String id, Modality modality, Action action, boolean only) {
		this.id = id;
		this.modality = modality;
		this.action = action;
		this.only = only;
	}
	
	public int compareTo(Rule rule) {
		return id.compareTo(rule.id);
	}
	
	public boolean equals(Rule rule) {
		return compareTo(rule) == 0;
	}
	
	public String toString() {
		String s = modality.toString();
		if (only) {
			s += " ONLY";
		}
		s += " " + action.toString();
		return s;
	}
	
	/**
	 * Describes the modality of a {@link Rule}. The modality may be expressed as one of:
	 * {@link Modality#PERMISSION}, if the action is permitted; {@link Modality#OBLIGATION},
	 * if the action is required; {@link Modality#REFRAINMENT}, if the action is prohibited;
	 * and {@link Modality#EXCLUSION}, if the action is not covered by the policy. In addition,
	 * exclusions may be further restricted to one of: {@link Modality#EXCLUSION_PERMISSION},
	 * if the policy excludes the permitted action; {@link Modality#EXCLUSION_OBLIGATION},
	 * if the policy excludes the required action; {@link Modality#EXCLUSION_REFRAINMENT},
	 * if the policy excludes the prohibited action. In the case of constrained exclusion,
	 * rules with a modality outside of the excluded modality may appear in the policy. For
	 * example, a policy may not contain permitted actions of the excluded type, but it may
	 * contain prohibitions over those same actions.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class Modality implements Comparable<Modality> {
		private final static boolean[][] conflict = new boolean[7][7];
		public final static Modality PERMISSION = new Modality(0);
		public final static Modality OBLIGATION = new Modality(1);
		public final static Modality REFRAINMENT = new Modality(2);
		public final static Modality EXCLUSION = new Modality(3);
		public final static Modality EXCLUSION_PERMISSION = new Modality(4);
		public final static Modality EXCLUSION_OBLIGATION = new Modality(5);
		public final static Modality EXCLUSION_REFRAINMENT = new Modality(6);
		private final int type;
		private final String[] label = new String[] {
			"P", "O", "R", "E", "EP", "EO", "ER"
		};
		
		static {
			// permissions and prohibitions
			conflict[0][2] = true;
			conflict[1][2] = true;
			conflict[2][0] = true;
			conflict[2][1] = true;
			// prescriptions and exclusions
			conflict[0][3] = true;
			conflict[1][3] = true;
			conflict[2][3] = true;
			conflict[3][0] = true;
			conflict[3][1] = true;
			conflict[3][2] = true;
			// exclusions of permission
			conflict[0][4] = true;
			conflict[1][4] = true;
			conflict[4][0] = true;
			conflict[4][1] = true;
			// exclusions of obligation
			conflict[1][5] = true;
			conflict[5][1] = true;
			// exclusions of prohibition
			conflict[2][6] = true;
			conflict[6][2] = true;
		}
		
		private Modality(int type) {
			this.type = type;
		}
		public int compareTo(Modality m) {
			return type - m.type;
		}
		public boolean conflictsWith(Modality m) {
			return conflict[type][m.type];
		}
		public boolean equals(Modality m) {
			return compareTo(m) == 0;
		}
		public String toString() {
			return label[type];
		}
	}
}
