package eddy.lang.net;

import java.net.URI;
import java.util.ArrayList;

import eddy.lang.Actor;
import eddy.lang.Policy;

/**
 * Describes a mapping between a policy and the parties with whom data is shared. The 
 * {@link Policy} is mapped to an identifying {@link URI} that distinguishes the policy
 * among other policies. In addition, the policy is linked to one or more {@link Party}
 * objects, which describe the direction of the flow, the actor role of the sender or
 * recipient, their URI, and the {@link ServiceMap} or thesaurus that aligns terminology 
 * between the two policies.
 * 
 * @author Travis Breaux
 *
 */

public class Agent {
	public final URI uri;
	private Policy policy = new Policy();
	private final ArrayList<Party> parties = new ArrayList<Party>();
	
	public Agent(URI uri) {
		this.uri = uri;
	}
	
	public void add(Party party) {
		parties.add(party);
	}
	
	public Policy getPolicy() {
		return policy;
	}
	
	public ArrayList<Party> parties() {
		return parties;
	}
	
	public void setPolicy(Policy policy) {
		this.policy = policy.clone();
	}
	
	public String toString() {
		return uri.toASCIIString();
	}
	/**
	 * Describes a data relationship with another party. This includes the direction of the 
	 * flow, the actor role of the sender or recipient, their URI, and the {@link ServiceMap} 
	 * or thesaurus that aligns terminology between the two policies.
	 * 
	 * @author Travis Breaux
	 *
	 */
	public static class Party {
		/**
		 * Describes the direction of the data relationship. If the data flows to this
		 * agent, then the direction is {@link Direction#IN}; else, the data flows from this agent
		 * and the direction is {@link Direction#OUT}.
		 * 
		 * @author Travis Breaux
		 *
		 */
		public enum Direction { IN, OUT };
		public final URI uri;
		public final Actor actor;
		public final ServiceMap map;
		public final Direction flow;
		
		public Party(Direction flow, Actor role, URI agent, ServiceMap map) {
			this.flow = flow;
			this.uri = agent;
			this.actor = role;
			this.map = map;
		}
		public String toString() {
			return uri.toASCIIString();
		}
	}
}
