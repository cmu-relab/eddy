package eddy.lang;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Describes a Policy comprised on {@link Rule} objects and {@link Type} definitions.
 * 
 * @author Travis Breaux
 *
 */

public class Policy implements Cloneable {
	private ArrayList<Rule> rules = new ArrayList<Rule>();
	private TreeMap<String,Rule> ruleMap = new TreeMap<String,Rule>();
	private TreeMap<String,String> attrs = new TreeMap<String,String>();
	private ArrayList<Type> types = new ArrayList<Type>();
	public final String id;
	
	public Policy() {
		this("policy-" + System.currentTimeMillis());
	}
	
	public Policy(String id) {
		this.id = id;
	}
	
	public void add(Type type) {
		types.add(type);
	}
	
	public void add(Rule rule) {
		ruleMap.put(rule.id, rule);
		rules.add(rule);
	}
	
	public String[] attributes() {
		return attrs.keySet().toArray(new String[attrs.size()]);
	}
	
	public Policy clone() {
		Policy policy = new Policy();
		
		for (String name : attrs.keySet()) {
			policy.setAttribute(name, attrs.get(name));
		}
		for (Rule rule : rules) {
			policy.add(rule);
		}
		for (Type type : types) {
			policy.add(type);
		}
		return policy;
	}
	
	public Type[] types() {
		return types.toArray(new Type[types.size()]);
	}
	
	public String getAttribute(String name) {
		return attrs.get(name);
	}
	
	public void setAttribute(String name, String value) {
		attrs.put(name, value);
	}
	
	public Rule getRule(String id) {
		return ruleMap.get(id);
	}
	
	public Rule[] rules() {
		return rules.toArray(new Rule[rules.size()]);
	}
	
	public String toString() {
		String s = "SPEC HEADER\n";

		// serialize the policy header attributes
		for (String attr : attrs.keySet()) {
			s += "\tATTR " + attr + " " + attrs.get(attr) + "\n";
		}
		// serialize the policy header types
		for (Type type : types) {
			s += "\t" + type.toString() + "\n";
		}

		// serialize the policy body rules
		s += "SPEC POLICY\n";
		for (Rule rule : rules) {
			s += "\t" + rule.toString() + "\n";
		}
		return s;
	}
}
