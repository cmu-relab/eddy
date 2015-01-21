package eddy.lang.net;

import java.net.URI;
import java.util.ArrayList;

import eddy.lang.Actor;
import eddy.lang.RoleValue;
import eddy.lang.Type;

/**
 * Provides a mapping between terminology from two agents. Each agent is assigned an
 * {@link Actor} role that constrains their interpretation in the other agent's 
 * {@link eddy.lang.Policy}. Terminology is described by one or more {@link Type} objects, wherein
 * the left-hand side describes {@link #agent1}'s terminology and right-hand side
 * describes {@link #agent2}'s terminology.
 * 
 * @author Travis Breaux
 *
 */

public class ServiceMap {
	private final ArrayList<Type> map = new ArrayList<Type>();
	public final URI agent1;
	public final URI agent2;
	public final RoleValue role1;
	public final RoleValue role2;
	
	public ServiceMap(URI agent1, Actor role1, URI agent2, Actor role2) {
		this.agent1 = agent1;
		this.agent2 = agent2;
		this.role1 = role1;
		this.role2 = role2;
	}
	
	public void add(Type type) {
		map.add(type);
	}
	
	public Type[] types() {
		return map.toArray(new Type[map.size()]);
	}
}
