package eddy.lang.analysis;

import java.net.URI;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Role;
import eddy.lang.RoleValue;
import eddy.lang.RoleValueSet;
import eddy.lang.Rule;
import eddy.lang.Type;
import eddy.lang.net.Agent;
import eddy.lang.net.Agent.Party;
import eddy.lang.net.ServiceMap;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.CompilerFactory;
import eddy.lang.parser.ParseException;

/**
 * Find all data flows across two or more {@link Policy} objects. Each policy is described 
 * by an {@link Agent}, which also describes each {@link Agent.Party} with whom data is shared.
 * 
 * The analysis proceeds as follows: first, identify all flows for each policy; second,
 * for each flow, link the source and target roles in the source and target rules
 * to the actor role in the party description (see {@link Party#actor}), call these flow 
 * {@link CrossFlow}s; finally, match crossflows across parties to identify the list of 
 * connecting flows, called streams.
 * 
 * @author Travis Breaux
 *
 */

public class CrossFlowTracer extends Tracer {
	private final TreeMap<URI,Agent> agents = new TreeMap<URI,Agent>();
	private final TreeMap<URI,ArrayList<Flow>> flows = new TreeMap<URI,ArrayList<Flow>>();
	private final TreeMap<URI,ArrayList<Rule>> sourceRuleMap = new TreeMap<URI,ArrayList<Rule>>();
	private final TreeMap<URI,ArrayList<Rule>> targetRuleMap = new TreeMap<URI,ArrayList<Rule>>();
	private final TreeSet<String> crossSource = new TreeSet<String>();
	private final TreeSet<String> crossTarget = new TreeSet<String>();
	private final TreeMap<URI,Compilation> compilations = new TreeMap<URI,Compilation>();
	private boolean strictPurposing = false;
	private ArrayList<ServiceMap> mappings = new ArrayList<ServiceMap>();
	private CompilerFactory factory;
	
	public CrossFlowTracer() {
		return;
	}
	
	public void add(ServiceMap map) {
		mappings.add(map);
	}
	
	public void add(Agent agent) {
		agents.put(agent.uri, agent);
	}
	
	public void addCrossFlowSource(String source) {
		this.crossSource.add(source);
	}
	
	public void addCrossFlowTarget(String target) {
		this.crossTarget.add(target);
	}
	
	public ArrayList<CrossFlow> trace(CompilerFactory factory) throws ParseException {
		return trace(factory, null);
	}
	
	public ArrayList<CrossFlow> trace(CompilerFactory factory, Datum datum) throws ParseException {
		this.factory = factory;
		
		// identify all the internal flows for each agent
		ArrayList<CrossFlow> crossFlows = new ArrayList<CrossFlow>();
		
		for (Agent agent : agents.values()) {
			// compile the agent's policy
			Policy policy = agent.getPolicy();
			
			// create a new compiler for each policy
			compiler = factory.createCompiler();
			Compilation comp = compiler.compile(policy);
			compilations.put(agent.uri, comp);

			FlowTracer tracer = new FlowTracer();
			for (String s : crossTarget) {
				tracer.addSource(s);
			}
			for (String s : crossSource) {
				tracer.addTarget(s);
			}
			ArrayList<Flow> list = tracer.trace(comp);
			flows.put(agent.uri, list);
			
			// map the flows to cross flows
			
			for (Flow flow : list) {
				CrossFlow crossFlow = new CrossFlow(agent.uri, agent.uri, flow);
				crossFlows.add(crossFlow);
			}
			
			
			// add the source and target rules to the inverse cross flow map
			sourceRuleMap.put(agent.uri, new ArrayList<Rule>(tracer.getTargetRules()));
			targetRuleMap.put(agent.uri, new ArrayList<Rule>(tracer.getSourceRules()));
		}
		
		// find matching flow pairs using the service maps
		
		
		for (ServiceMap map : mappings) {
			Agent agent = agents.get(map.agent1);
			Agent party = agents.get(map.agent2);
			
			// set the compilation (used in traceRules) to the out-flow agent
			System.err.println("Tracing " + agent);
			
			// filter the source and target rules based on the restrictions
			ArrayList<Rule> sourceRules = new ArrayList<Rule>();
			ArrayList<Rule> sources = sourceRuleMap.get(agent.uri);
			Compilation comp1 = compilations.get(agent.uri);
			setCompilation(comp1);
			
			for (Rule source : sources) {
				// skip rules that do not cover the source action
				if (!crossSource.contains(source.action.name)) {
					continue;
				}
				
				// check that the source rule's target role is subsumed by the party's role constraint
				Flow.Mode roleCheck = getRoleRestriction(source, Role.Type.TARGET, map.role2);
				if (roleCheck == null) {
					continue;
				}
				sourceRules.add(source);
			}
			
			ArrayList<Rule> targetRules = new ArrayList<Rule>();
			ArrayList<Rule> targets = targetRuleMap.get(party.uri);
			Compilation comp2 = compilations.get(party.uri);
			setCompilation(comp2);
			
			for (Rule target : targets) {
				// skip rules that do not cover the source action
				if (!crossTarget.contains(target.action.name)) {
					continue;
				}
				
				// check that the source rule's target role is subsumed by the party's role constraint
				Flow.Mode roleCheck = getRoleRestriction(target, Role.Type.SOURCE, map.role1);
				if (roleCheck == null) {
					continue;
				}
				targetRules.add(target);
			}

			// find cross flows between the source and target rules
			Compilation comp = compileServiceMap(map, comp1, comp2);
			setCompilation(comp);
			Compiler c1 = comp1.getCompiler();
			Compiler c2 = comp2.getCompiler();
			
			for (Rule source : sourceRules) {
				for (Rule target : targetRules) {
					Flow flow = traceRules(c1, source, c2, target);
					if (flow != null) {
						CrossFlow crossFlow = new CrossFlow(agent.uri, party.uri, flow);
						crossFlows.add(crossFlow);
					}
				}
			}
		}
		return crossFlows;
	}
	
	private Flow.Mode getRoleRestriction(Rule rule, Role.Type type, RoleValue value) throws ParseException {
		RoleValueSet set = rule.action.getRole(type).values;
		OWLClassExpression source = compiler.compile(set);
		OWLClassExpression target = compiler.compile(value);
		return getFlowRestriction(source, target);
	}
	
	private Compilation compileServiceMap(ServiceMap map, Compilation comp1, Compilation comp2) throws ParseException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology;
		
		try {
			ontology = manager.createOntology();
		}
		catch (OWLOntologyCreationException e) {
			throw new ParseException(e.getMessage());
		}

		OWLDataFactory factory = manager.getOWLDataFactory();
		Compilation comp = new Compilation(this.factory.createCompiler(), null, ontology);
		
		Type[] type = map.types();
		for (int i = 0; i < type.length; i++) {
			switch (type[i].op){
				case Type.EQUIVALENT: {
					OWLClass clazz1 = factory.getOWLClass(IRI.create(map.agent1.toString() + "#" + type[i].lhs));
					OWLClass clazz2 = factory.getOWLClass(IRI.create(map.agent2.toString() + "#" + type[i].rhs[0]));
					OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(clazz1, clazz2);
					manager.addAxiom(ontology, axiom);
					break;
				}
				case Type.SUBCLASS: {
					OWLClass clazz1 = factory.getOWLClass(IRI.create(map.agent1.toString() + "#" + type[i].lhs));
					OWLClass clazz2 = factory.getOWLClass(IRI.create(map.agent2.toString() + "#" + type[i].rhs[0]));
					OWLAxiom axiom = factory.getOWLSubClassOfAxiom(clazz1, clazz2);
					manager.addAxiom(ontology, axiom);
					break;
				}
				case Type.SUPERCLASS: {
					OWLClass clazz1 = factory.getOWLClass(IRI.create(map.agent1.toString() + "#" + type[i].lhs));
					OWLClass clazz2 = factory.getOWLClass(IRI.create(map.agent2.toString() + "#" + type[i].rhs[0]));
					OWLAxiom axiom = factory.getOWLSubClassOfAxiom(clazz2, clazz1);
					manager.addAxiom(ontology, axiom);
					break;
				}
			}
		}
		OWLOntology onto1 = comp1.getOntology();
		OWLOntology onto2 = comp2.getOntology();
		manager.addAxioms(comp.getOntology(), onto1.getAxioms());
		manager.addAxioms(comp.getOntology(), onto2.getAxioms());
		comp.getReasoner().flush();
		return comp;
	}
	
	protected Flow traceRules(Compiler comp1, Rule source, Compiler comp2, Rule target) throws ParseException {
		final TreeMap<Role.Type,Flow.Mode> modes = new TreeMap<Role.Type,Flow.Mode>();
		final Role.Type[] type = new Role.Type[] { Role.Type.OBJECT, Role.Type.PURPOSE };
		
		// setup the source role value descriptions
		OWLClassExpression[][] pair;
		if (strictPurposing) {
			pair = new OWLClassExpression[2][2];
		}
		else {
			pair = new OWLClassExpression[1][2];
		}

		// setup the source role value descriptions
		pair[0][0] = comp1.compile(source.action.getRole(Role.Type.OBJECT).values);
		
		// setup the target role value descriptions
		pair[0][1] = comp2.compile(target.action.getRole(Role.Type.OBJECT).values);
		

		if (strictPurposing) {
			pair[2][0] = comp1.compile(source.action.getRole(Role.Type.PURPOSE).values);
			pair[2][1] = comp2.compile(target.action.getRole(Role.Type.PURPOSE).values);
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
	
	/**
	 * Describes a {@link Flow} between two {@link Policy} objects.
	 * 
	 * @author Travis Breaux
	 *
	 */
	public static class CrossFlow extends Flow {
		public final URI sourceURI;
		public final URI targetURI;
		
		public CrossFlow(URI sourceURI, URI targetURI, Flow flow) {
			super(flow.source, flow.target, flow.modes);
			this.sourceURI = sourceURI;
			this.targetURI = targetURI;
		}
		
		public String toString() {
			return "Flow(" + sourceURI.toString() + "!" + source.id + ":" +
				targetURI.toString() + "!" + target.id + modes;
		}
	}
}
