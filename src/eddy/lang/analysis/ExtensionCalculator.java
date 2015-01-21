package eddy.lang.analysis;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import eddy.lang.Action;
import eddy.lang.Actor;
import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Purpose;
import eddy.lang.Role;
import eddy.lang.RoleValue;
import eddy.lang.RoleValueSet;
import eddy.lang.RoleValueSet.Singleton;
import eddy.lang.Rule;
import eddy.lang.Rule.Modality;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.CompilationProperties;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.CompilerConstants;
import eddy.lang.parser.Logger;
import eddy.lang.parser.ParseException;

/**
 * Computes the {@link Extension} from a {@link Policy}. The extension consists of all itemized
 * {@link Action} interpretations. An action is a valid itemization of a {@link Rule} if the
 * {@link Action} + {@link Rule.Modality} is subsumed by the {@link Rule}.
 * 
 * The extension is used by the {@link ConflictAnalyzer} to detect {@link Conflict}, and by
 * the {@link LimitationPrinciple} to compute the set of limiting rights.
 * 
 * @author Travis Breaux
 *
 */

public class ExtensionCalculator implements CompilerConstants, CompilationProperties {
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private OWLReasoner reasoner;
	private Compiler compiler;
	private RoleValueCastor castActor, castDatum, castPurpose;
	private TreeMap<Role.Type,RoleValueCastor> roleRangeMap = new TreeMap<Role.Type,RoleValueCastor>();
	private OWLClass classActor, classDatum, classPurpose;
	private final Logger logger = new Logger(new PrintWriter(System.err), Logger.WARN, this.getClass().getName() + ": ");
	private boolean computeExceptions = false;
	private boolean computeCompleteExtension = false;
	private boolean computeOnlyProhibitions = true;
	
	public ExtensionCalculator() {
		return;
	}
	
	private void setupClassesAndCastors() {
		classActor = factory.getOWLClass(IRI.create(nsActor));
		classDatum = factory.getOWLClass(IRI.create(nsDatum));
		classPurpose = factory.getOWLClass(IRI.create(nsPurpose));
		
		castActor = new RoleValueCastor() {
			public RoleValue cast(OWLClass c) {
				if (!c.equals(classActor)) {
					return new Actor(c.getIRI().getFragment());
				}
				else {
					return Actor.ANYONE;
				}
			}
		};
		castDatum = new RoleValueCastor() {
			public RoleValue cast(OWLClass c) {
				if (!c.equals(classDatum)) {
					return new Datum(c.getIRI().getFragment());
				}
				else {
					return Datum.ANYTHING;
				}
			}
		};
		castPurpose = new RoleValueCastor() {
			public RoleValue cast(OWLClass c) {
				if (!c.equals(classPurpose)) {
					return new Purpose(c.getIRI().getFragment());
				}
				else {
					return Purpose.ANYTHING;
				}
			}
		};
		
		roleRangeMap.put(Role.Type.OBJECT, castDatum);
		roleRangeMap.put(Role.Type.SOURCE, castActor);
		roleRangeMap.put(Role.Type.TARGET, castActor);
		roleRangeMap.put(Role.Type.PURPOSE, castPurpose);
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	public ArrayList<Action> compute(Compilation comp) throws ParseException {
		// create the extension from every combination of action and role value
		ArrayList<Action> actions = new ArrayList<Action>();

		if (computeCompleteExtension) {
			Action action = new Action("COLLECT");
			action.add(new Role(Role.Type.OBJECT, "", new Singleton(Datum.ANYTHING)));
			action.add(new Role(Role.Type.SOURCE, "FROM", new Singleton(Actor.ANYONE)));
			action.add(new Role(Role.Type.PURPOSE, "FOR", new Singleton(Purpose.ANYTHING)));
			actions.add(action);
			action = new Action("USE");
			action.add(new Role(Role.Type.OBJECT, "", new Singleton(Datum.ANYTHING)));
			action.add(new Role(Role.Type.SOURCE, "FROM", new Singleton(Actor.ANYONE)));
			action.add(new Role(Role.Type.PURPOSE, "FOR", new Singleton(Purpose.ANYTHING)));
			actions.add(action);
			action = new Action("TRANSFER");
			action.add(new Role(Role.Type.OBJECT, "", new Singleton(Datum.ANYTHING)));
			action.add(new Role(Role.Type.SOURCE, "FROM", new Singleton(Actor.ANYONE)));
			action.add(new Role(Role.Type.TARGET, "TO", new Singleton(Actor.ANYONE)));
			action.add(new Role(Role.Type.PURPOSE, "FOR", new Singleton(Purpose.ANYTHING)));
			actions.add(action);
		}
		else {
			TreeMap<String,Action> actionMap = new TreeMap<String,Action>();
			Rule[] rule = comp.getPolicy().rules();
			for (int i = 0; i < rule.length; i++) {
				if (computeOnlyProhibitions && !rule[i].modality.equals(Modality.REFRAINMENT)) {
					continue;
				}

				String s = rule[i].action.toString();
				if (!actionMap.containsKey(s)) {
					actionMap.put(s, rule[i].action.clone());
				}
			}
			actions.addAll(actionMap.values());
		}
		return compute(comp, actions);
	}
	
	public ArrayList<Action> compute(Compilation comp, ArrayList<Action> range) throws ParseException {
		this.ontology = comp.getOntology();
		this.manager = ontology.getOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		this.reasoner = comp.getReasoner();
		this.compiler = comp.getCompiler();
		
		// setup the default classes and role value castors
		setupClassesAndCastors();
		
		ArrayList<Action> actions = new ArrayList<Action>();
		
		logger.log(Logger.DEBUG, "Computing extension for " + range.size() + " action(s)");
		for (Action action : range) {
			// create an action from this rule
			Role[] role = action.roles();
			@SuppressWarnings("unchecked")
			ArrayList<RoleValueSet>[] values = new ArrayList[role.length];
			for (int j = 0; j < role.length; j++) {
				values[j] = new ArrayList<RoleValueSet>();
				
				if (!computeExceptions) {
					computeRoleValueRangeExceptionless(values[j], roleRangeMap.get(role[j].type), role[j].values);
				}
				else {
					computeRoleValueRange(values[j], roleRangeMap.get(role[j].type), role[j].values);
				}
			}
			
			// compute the actions from the product of role values
			actions.addAll(compute(action.name, role, values));
		}
		
		// filter actions to remove duplicates
		TreeMap<String,Action> map = new TreeMap<String,Action>();
		for (Action a : actions) {
			map.put(a.toString(), a);
		}
		actions.clear();
		actions.addAll(map.values());
		
		// save statistics for reporting purposes
		comp.getProperties().setProperty(CompilationProperties.EXT_COMPUTED, "true");
		comp.getProperties().setProperty(CompilationProperties.EXT_SIZE, "" + actions.size());
		return actions;

	}
	
	private List<Action> compute(String name, Role[] role, ArrayList<RoleValueSet>[] sets) {
		List<Action> actions = new ArrayList<Action>();
		
		// for the first role, create the initial set of actions
		for (int i = 0; i < sets[0].size(); i++) {
			Action a = new Action(name);
			Role r = new Role(role[0].type, role[0].prefix, sets[0].get(i));
			a.add(r);
			actions.add(a);
		}
		// for each subsequent role, create the remaining actions using the role product
		for (int i = 1; i < sets.length; i++) {
			int mark = actions.size();
			
			for (int j = 0; j < mark; j++) {
				
				for (int k = 0; k < sets[i].size(); k++) {
					Action a = actions.get(j).clone();
					Role r = new Role(role[i].type, role[i].prefix, sets[i].get(k));
					a.add(r);
					actions.add(a);
				}
			}
			actions = actions.subList(mark, actions.size());
		}
		return actions;
	}

	private void computeRoleValueRange(ArrayList<RoleValueSet> ranges, RoleValueCastor castor, RoleValueSet vset) throws ParseException {
		// compute the range as c \ union of subclasses and recurse for each subclass

		/* We use vset.first() to remove any exceptions; that said, its possible the vset
		 * describes something else, such as a union or intersection, in which case, we may
		 * lose the complete interpretation. A better implementation would account for
		 * these special cases.
		 */
		
		OWLClassExpression c = compiler.compile(vset.first());
		Set<OWLClass> except = reasoner.getSubClasses(c, true).getFlattened();
		except.remove(factory.getOWLNothing());
		
		if (except.size() == 0) {
			ranges.add(vset);
			return;
		}
		
		// process the first subclass as a singleton exception
		OWLClass d = except.iterator().next();
		except.remove(d);
		RoleValue value = castor.cast(d);
		RoleValueSet set = new RoleValueSet.Singleton(value);
		
		// recurse on the first subclass
		computeRoleValueRange(ranges, castor, set);
		
		// for all additional subclasses, build a union
		for (OWLClass e : except) {
			value = castor.cast(e);
			set = new RoleValueSet.Union(value, set);
			
			// recurse on each additional subclasses
			computeRoleValueRange(ranges, castor, new RoleValueSet.Singleton(value));
		}

		set = new RoleValueSet.Complement(vset, set);
		ranges.add(set);
	}
	
	private void computeRoleValueRangeExceptionless(ArrayList<RoleValueSet> ranges, RoleValueCastor castor, RoleValueSet vset) throws ParseException {
		// compute the range as c \ union of subclasses and recurse for each subclass
		OWLClassExpression c = compiler.compile(vset.first());
		Set<OWLClass> subs = reasoner.getSubClasses(c, true).getFlattened();
		subs.remove(factory.getOWLNothing());
		
		// initialize the ranges with the high-level role value
		ranges.add(new RoleValueSet.Singleton(vset.first()));
				
		// process all subclasses as singletons
		
		for (OWLClass d : subs) {
			RoleValue value = castor.cast(d);
			RoleValueSet set = new RoleValueSet.Singleton(value);
			ranges.add(set);
		}
	}
	
	abstract class RoleValueCastor {
		public abstract RoleValue cast(OWLClass c);
	}
	
	public static TreeMap<String,TreeSet<Rule>> findRules(Compilation comp, Set<String> ext) {
		TreeMap<String,TreeSet<Rule>> map = new TreeMap<String,TreeSet<Rule>>();
		
		// setup the reasoner classes
		String ns = comp.getOntology().getOntologyID().getOntologyIRI().toString();
		OWLReasoner reasoner = comp.getReasoner();
		OWLDataFactory factory = comp.getOntology().getOWLOntologyManager().getOWLDataFactory();
		Policy policy = comp.getPolicy();
		
		/* For each extension id, find all the superclasses of id that are also
		 * subclasses of rule
		 */
		for (String extID : ext) {
			OWLClass expr = factory.getOWLClass(IRI.create(ns + "#" + extID));
			Set<OWLClass> set = reasoner.getSuperClasses(expr, true).getFlattened();
			
			for (OWLClass c : set) {
				String id = c.getIRI().getFragment();
				Rule rule = policy.getRule(id);

				if (rule == null) {
					continue;
				}
				TreeSet<Rule> rules = map.get(extID);
				if (rules == null) {
					rules = new TreeSet<Rule>();
					map.put(extID, rules);
				}
				rules.add(rule);
			}
		}
		return map;
	}
	
	public static TreeMap<Rule,TreeSet<String>> findExtension(Compilation comp, List<Rule> rules) {
		TreeMap<Rule,TreeSet<String>> map = new TreeMap<Rule,TreeSet<String>>();
		
		// setup the reasoner classes
		String ns = comp.getOntology().getOntologyID().getOntologyIRI().toString();
		OWLReasoner reasoner = comp.getReasoner();
		OWLDataFactory factory = comp.getOntology().getOWLOntologyManager().getOWLDataFactory();
		
		/* For each rule, find all the subclasses of the rule id that are also
		 * subclasses of the extension
		 */
		for (Rule rule : rules) {
			OWLClass id = factory.getOWLClass(IRI.create(ns + "#" + rule.id));
			Set<OWLClass> set = reasoner.getSubClasses(id, true).getFlattened();
			
			for (OWLClass c : set) {
				TreeSet<String> ids = map.get(rule);
				if (ids == null) {
					ids = new TreeSet<String>();
					map.put(rule, ids);
				}
				ids.add(c.getIRI().getFragment());
			}
		}
		
		return map;
	}
	
	public Extension extend(Compilation comp) throws ParseException {
		ArrayList<Action> actions = compute(comp);
		Extension ext = extend(comp, actions, 0);
		return ext;
	}
	
	public static Extension extend(Compilation comp, List<Action> actions, int counter) {
		// copy the ontology before performing extension
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLOntology ontology = null;
		OWLOntology onto = comp.getOntology();
		String ns = onto.getOntologyID().getOntologyIRI().toString();
		boolean useLocal = false;
		
		// load the upper ontology
		if (useLocal) {
			IRI docIRI = IRI.create("http://gaius.isri.cmu.edu/2011/8/policy-base.owl");
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(new File("study/policy-base.owl")));
			manager.addIRIMapper(mapper);
		}
		
		IRI iri = IRI.create(Compiler.NS);
		OWLImportsDeclaration decl = factory.getOWLImportsDeclaration(iri);
		OWLOntologyLoaderConfiguration conf = new OWLOntologyLoaderConfiguration();

		try {
			ontology = manager.createOntology(IRI.create(ns));
			manager.applyChange(new AddImport(ontology, decl));
			manager.loadOntology(iri);
			manager.makeLoadImportRequest(decl, conf);

		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// copy the axioms from the existing ontology
		manager.addAxioms(ontology, onto.getAxioms());
		
		// compile the extension into the new ontology
		Compiler compiler = new Compiler(ontology);
		TreeMap<String,Action> extMap = new TreeMap<String,Action>();
		
		// for each action, create a new equivalence class to index that action
		for (Action a : actions) {
			try {
				OWLClassExpression expr = compiler.compile(a);
				OWLClassExpression id = factory.getOWLClass(IRI.create(ns + "#x" + counter));
				OWLAxiom axiom1 = factory.getOWLEquivalentClassesAxiom(id, expr);
				manager.addAxiom(ontology, axiom1);
				extMap.put("x" + counter, a);
				counter++;
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		comp.getProperties().setProperty(CompilationProperties.EXT_COMPUTED, "true");
		comp.getProperties().setProperty(CompilationProperties.EXT_SIZE, counter + "");
		
		return new Extension(compiler, comp.getPolicy(), ontology, extMap);
	}
}
