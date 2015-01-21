package eddy.lang.analysis;

import java.util.ArrayList;	
import java.util.TreeMap;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLClassExpression;

import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Role;
import eddy.lang.Role.Type;
import eddy.lang.Rule;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.CompilationProperties;
import eddy.lang.parser.CompilerConstants;
import eddy.lang.parser.ParseException;

/**
 * Traces data flows in a {@link Policy} from source to target {@link eddy.lang.Action}.
 * Each data flow is described by a {@link Flow} object, which includes the {@link Flow.Mode}.
 * 
 * Data flows are described in a policy by the non-empty intersection of all primary roles 
 * ({@link Type#OBJECT}, {@link Type#SOURCE}, {@link Type#TARGET}) across two rules: 
 * the "incoming" data flow rule, called the source rule, and the "outgoing" data flow 
 * rule, called the target rule. For example, to trace from collection to transfer, the 
 * collection rules are source rules and the transfer rules are target rules. Other 
 * types of actions can be included in a flow analysis, e.g., tracing from collection to use.
 * 
 * @author Travis Breaux
 *
 */

public class FlowTracer extends Tracer implements CompilerConstants, CompilationProperties {
	private final TreeSet<String> source = new TreeSet<String>();
	private final TreeSet<String> target = new TreeSet<String>();
	private final ArrayList<Rule> sourceRules = new ArrayList<Rule>();
	private final ArrayList<Rule> targetRules = new ArrayList<Rule>();
	protected boolean strictPurposing = false;
	
	public FlowTracer() {
		return;
	}
	
	public void addSource(String source) {
		this.source.add(source);
	}
	
	public void addTarget(String target) {
		this.target.add(target);
	}

	public ArrayList<Rule> getSourceRules() {
		return sourceRules;
	}
	
	public ArrayList<Rule> getTargetRules() {
		return targetRules;
	}
	
	public ArrayList<Flow> trace(Compilation comp) throws ParseException {
		return trace(comp, null);
	}
	
	public ArrayList<Flow> trace(Compilation comp, Datum datum) throws ParseException {
		ArrayList<Flow> flows = new ArrayList<Flow>();
		setCompilation(comp);
		
		sourceRules.clear();
		targetRules.clear();
		
		// filter the policy rules to those of interest
		Policy policy = comp.getPolicy();
		for (Rule rule : policy.rules()) {
			if (source.contains(rule.action.name)) {
				sourceRules.add(rule);
			}
			if (target.contains(rule.action.name)) {
				targetRules.add(rule);
			}
		}
		
		for (Rule s : sourceRules) {
			// apply datum constraint, if any
			if (datum != null) {
				Flow.Mode mode = getFlowRestriction(s, datum);
				if (mode == null) {
					continue;
				}
			}
			for (Rule t : targetRules) {
				// apply datum constraint, if any
				if (datum != null) {
					Flow.Mode mode = getFlowRestriction(t, datum);
					if (mode == null) {
						continue;
					}
				}
				Flow flow = traceRules(s, t);
				if (flow != null) {
					flows.add(flow);
				}
			}
		}
		
		return flows;
	}
	
	protected Flow traceRules(Rule source, Rule target) throws ParseException {
		final TreeMap<Role.Type,Flow.Mode> modes = new TreeMap<Role.Type,Flow.Mode>();
		final Role.Type[] type = new Role.Type[] { Type.OBJECT, Type.SOURCE, Type.PURPOSE };
		
		// setup the source role value descriptions
		OWLClassExpression[][] pair;
		if (strictPurposing) {
			pair = new OWLClassExpression[3][2];
		}
		else {
			pair = new OWLClassExpression[2][2];
		}

		// setup the source role value descriptions
		pair[0][0] = compiler.compile(source.action.getRole(Type.OBJECT).values);
		pair[1][0] = compiler.compile(source.action.getRole(Type.SOURCE).values);
		
		// setup the target role value descriptions
		pair[0][1] = compiler.compile(target.action.getRole(Type.OBJECT).values);
		pair[1][1] = compiler.compile(target.action.getRole(Type.SOURCE).values);
		

		if (strictPurposing) {
			pair[2][0] = compiler.compile(source.action.getRole(Type.PURPOSE).values);
			pair[2][1] = compiler.compile(target.action.getRole(Type.PURPOSE).values);
		}

		// clear the entailed flow modes: one for each role is a matching flow
		modes.clear();
		for (int i = 0; i < pair.length; i++) {
			Flow.Mode mode = getFlowRestriction(pair[i][0], pair[i][1]);
		
			if (mode != null) {
				modes.put(type[i], mode);
			}
			else {
				break;
			}
		}
		if (modes.size() == pair.length) {
			return new Flow(source, target, modes);
		}	
		return null;
	}
}