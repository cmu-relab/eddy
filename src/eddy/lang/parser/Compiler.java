package eddy.lang.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import eddy.lang.Action;
import eddy.lang.Actor;
import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Purpose;
import eddy.lang.Role;
import eddy.lang.RoleValue;
import eddy.lang.RoleValueSet;
import eddy.lang.RoleValueVisitor;
import eddy.lang.Rule;
import eddy.lang.Rule.Modality;
import eddy.lang.Type;

/**
 * Translates {@link Policy} objects into {@link Compilation} objects. It is assumed that all
 * policy objects have a NAMESPACE attribute defined, otherwise the default namespace (see
 * {@link CompilerConstants#NS} will be used. 
 * 
 * The compiler employs the {@link RoleValueVisitor} pattern to compile the {@link Role} in an
 * {@link Action} object.
 * 
 * @author Travis Breaux
 */

public class Compiler implements CompilerConstants {
	private OWLOntology ontology = null;
	private OWLDataFactory factory;
	private OWLOntologyManager manager;
	private String ns;
	private final TreeMap<Modality,OWLClass> modality = new TreeMap<Modality,OWLClass>();
	private final TreeMap<Role.Type,OWLObjectProperty> roles = new TreeMap<Role.Type,OWLObjectProperty>();
	private final TreeSet<String> actions = new TreeSet<String>();
	private final TreeMap<Integer,TreeSet<String>> declared = new TreeMap<Integer,TreeSet<String>>();
	private final TreeMap<Integer,TreeSet<String>> undeclared = new TreeMap<Integer,TreeSet<String>>();
	public final OWLClass classActor, classDatum, classPurpose, classAction;
	public final OWLObjectProperty roleObject, roleSource, roleTarget, rolePurpose, roleInstrument;
	private final RoleValueCompiler valueCompiler = new RoleValueCompiler(this);
	
	public Compiler() {
		this(OWLManager.createOWLOntologyManager());
	}
	
	public Compiler(OWLOntology ontology) {
		this(ontology.getOWLOntologyManager());
		this.ontology = ontology;
		this.ns = ontology.getOntologyID().getOntologyIRI().toString();
	}
	
	public Compiler(OWLOntologyManager manager) {
		this.manager = manager;
		this.factory = manager.getOWLDataFactory();
		
		// declare the actor, datum and purpose classes and declaration repositories
		this.classActor = factory.getOWLClass(IRI.create(nsActor));
		this.classDatum = factory.getOWLClass(IRI.create(nsDatum));
		this.classPurpose = factory.getOWLClass(IRI.create(nsPurpose));
		this.classAction = factory.getOWLClass(IRI.create(nsAction));
		
		declared.put(Type.CLASS_ACTOR, new TreeSet<String>());
		declared.put(Type.CLASS_DATUM, new TreeSet<String>());
		declared.put(Type.CLASS_PURPOSE, new TreeSet<String>());
		undeclared.put(Type.CLASS_ACTOR, new TreeSet<String>());
		undeclared.put(Type.CLASS_DATUM, new TreeSet<String>());
		undeclared.put(Type.CLASS_PURPOSE, new TreeSet<String>());
		
		modality.put(Modality.PERMISSION, factory.getOWLClass(IRI.create(nsRight)));
		modality.put(Modality.OBLIGATION, factory.getOWLClass(IRI.create(nsObligation)));
		modality.put(Modality.REFRAINMENT, factory.getOWLClass(IRI.create(nsProhibition)));
		modality.put(Modality.EXCLUSION, factory.getOWLClass(IRI.create(nsExclusion)));
		modality.put(Modality.EXCLUSION_PERMISSION, factory.getOWLClass(IRI.create(nsExclusionOfRight)));
		modality.put(Modality.EXCLUSION_OBLIGATION, factory.getOWLClass(IRI.create(nsExclusionOfObligation)));
		modality.put(Modality.EXCLUSION_REFRAINMENT, factory.getOWLClass(IRI.create(nsExclusionOfProhibition)));
		
		// declare the role properties
		this.roleObject = factory.getOWLObjectProperty(IRI.create(nsHasObject));
		this.roleSource = factory.getOWLObjectProperty(IRI.create(nsHasSource));
		this.rolePurpose = factory.getOWLObjectProperty(IRI.create(nsHasPurpose));
		this.roleTarget = factory.getOWLObjectProperty(IRI.create(nsHasTarget));
		this.roleInstrument = factory.getOWLObjectProperty(IRI.create(nsHasInstrument));
		roles.put(Role.Type.OBJECT, roleObject);
		roles.put(Role.Type.SOURCE, roleSource);
		roles.put(Role.Type.PURPOSE, rolePurpose);
		roles.put(Role.Type.TARGET, roleTarget);
		roles.put(Role.Type.INSTRUMENT, roleInstrument);
	}
	
	public Compilation compile(Policy policy) throws ParseException {
		// setup the lower ontology namespace
		this.ns = policy.getAttribute("NAMESPACE");
		if (this.ns == null) {
			this.ns = NS;
		}
		
		// create the ontology using the attribute namespace
		try {
			this.ontology = manager.createOntology(IRI.create(ns));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			throw new ParseException("Cannot create policy with namespace: " + ns);
		}

		// load the upper ontology
		OWLImportsDeclaration decl = factory.getOWLImportsDeclaration(IRI.create(NS));
		OWLOntologyLoaderConfiguration conf = new OWLOntologyLoaderConfiguration();

		manager.applyChange(new AddImport(ontology, decl));
		try {
			manager.loadOntology(IRI.create(NS));
			manager.makeLoadImportRequest(decl, conf);
		} catch (UnloadableImportException e) {
			throw new ParseException("Cannot load policy framework from: " + NS);
		} catch (OWLOntologyCreationException e) {
			throw new ParseException("Cannot load policy framework from: " + NS);
		}
		
		// process all the type axioms
		Type[] type = policy.types();
		for (int i = 0; i < type.length; i++) {
			compile(type[i]);
		}
		undeclared.get(Type.CLASS_ACTOR).removeAll(declared.get(Type.CLASS_ACTOR));
		undeclared.get(Type.CLASS_DATUM).removeAll(declared.get(Type.CLASS_DATUM));
		undeclared.get(Type.CLASS_PURPOSE).removeAll(declared.get(Type.CLASS_PURPOSE));

		// process all the rules
		Rule[] rule = policy.rules();
		for (int i = 0; i < rule.length; i++) {
			compile(rule[i]);
		}
		
		// declare undeclared top-level subclasses for actors and purposes
		TreeSet<String> names;
		names = undeclared.get(Type.CLASS_ACTOR);
		for (Iterator<String> i = names.iterator(); i.hasNext();) {
			String name = i.next();
			OWLClass sub = factory.getOWLClass(IRI.create(ns + "#" + name));
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(sub, classActor);
			manager.applyChange(new AddAxiom(ontology, axiom));
		}
		names = undeclared.get(Type.CLASS_PURPOSE);
		for (Iterator<String> i = names.iterator(); i.hasNext();) {
			String name = i.next();
			OWLClass sub = factory.getOWLClass(IRI.create(ns + "#" + name));
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(sub, classPurpose);
			manager.applyChange(new AddAxiom(ontology, axiom));
		}
		names = undeclared.get(Type.CLASS_DATUM);
		for (Iterator<String> i = names.iterator(); i.hasNext();) {
			String name = i.next();
			OWLClass sub = factory.getOWLClass(IRI.create(ns + "#" + name));
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(sub, classDatum);
			manager.applyChange(new AddAxiom(ontology, axiom));
		}
		
		Compilation comp = new Compilation(this, policy, ontology);
		assumeDisjointnessProperty(comp, classPurpose);
		return comp;
	}
	
	public void compile(Rule rule) throws ParseException {
		// select the modality class
		OWLClass modality = this.modality.get(rule.modality);
		
		// define the rule identity
		OWLClass identity = factory.getOWLClass(IRI.create(ns + "#" + rule.id));

		// declare the identity is a subclass of its modality and a rule
		OWLAxiom axiom1 = factory.getOWLSubClassOfAxiom(identity, modality);
		manager.applyChange(new AddAxiom(ontology, axiom1));
		
		final OWLClass ruleClass = factory.getOWLClass(IRI.create(nsRule));
		OWLAxiom axiom2 = factory.getOWLSubClassOfAxiom(identity, ruleClass);
		manager.applyChange(new AddAxiom(ontology, axiom2));

		OWLClassExpression expr = compile(rule.action);

		// declare the equivalence axiom for this rule
		OWLAxiom axiom3 = factory.getOWLEquivalentClassesAxiom(identity, expr);
		manager.applyChange(new AddAxiom(ontology, axiom3));
	}
	
	public OWLClassExpression compile(Role role) throws ParseException {
		OWLObjectProperty prop = roles.get(role.type);
		OWLClassExpression expr = compile(role.values);
		return factory.getOWLObjectSomeValuesFrom(prop, expr);
	}
	
	public OWLClassExpression compile(RoleValueSet values) throws ParseException {
		final TreeSet<OWLClassExpression> set = new TreeSet<OWLClassExpression>();
		set.clear();
		
		/* By declaring the parent as a singleton, we force the compiler to
		 * compile this value set into a one expression after processing 
		 * all the children
		 */
		compile(values, RoleValueSet.Type.SINGLE, set);
		return set.first();
	}
	
	private void compile(RoleValueSet values, RoleValueSet.Type parentType, TreeSet<OWLClassExpression> set) throws ParseException {
		if (values.isSingle()) {
			RoleValue value = values.getValue();
			OWLClassExpression expr = compile(value);
			set.add(expr);
		}
		else if (values.type.equals(parentType)) {
			compile(values.getLHS(), values.type, set);
			compile(values.getRHS(), values.type, set);
		}
		else {
			OWLClassExpression expr;
			switch (values.type) {
				case COMPLEMENT: {
					compile(values.getRHS(), values.type, set);
					expr = factory.getOWLObjectComplementOf(set.first());
					set.clear();
					compile(values.getLHS(), values.type, set);
					set.add(expr);
					expr = factory.getOWLObjectIntersectionOf(set);
					break;
				}
				case INTERSECT: {
					compile(values.getLHS(), values.type, set);
					compile(values.getRHS(), values.type, set);
					expr = factory.getOWLObjectIntersectionOf(set);
					break;
				}
				case UNION: {
					compile(values.getLHS(), values.type, set);
					compile(values.getRHS(), values.type, set);
					expr = factory.getOWLObjectUnionOf(set);
					break;
				}
				default: {
					throw new ParseException("Unrecognized role value set type: " + values.type);
				}
			}
			set.clear();
			set.add(expr);
		}
	}
	
	public OWLClassExpression compile(RoleValue value) throws ParseException {
		value.accept(valueCompiler);
		return valueCompiler.expr;
	}
	
	public OWLClassExpression compile(Action action) throws ParseException {
		TreeSet<OWLClassExpression> set = new TreeSet<OWLClassExpression>();

		// define the action
		OWLClass act = factory.getOWLClass(IRI.create(ns + "#" + action.name));
		set.add(act);
		
		// if the action is undefined, extend the upper ontology with this action
		if (ontology != null && !actions.contains(action.name)) {
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(act, classAction);
			manager.applyChange(new AddAxiom(ontology, axiom));
			actions.add(action.name);
		}
		
		Role[] role = action.roles();
		for (int i = 0; i < role.length; i++) {
			// map the role to an owl class expression
			set.add(compile(role[i]));
		}
		
		// declare the rule expression from the action and roles
		OWLClassExpression expr = factory.getOWLObjectIntersectionOf(set);
		return expr;
	}
	
	public void compile(Type type) throws ParseException {
		// create the owl class for the lhs and rhs
		OWLClass lhs = factory.getOWLClass(IRI.create(ns + "#" + type.lhs));
		
		// all left-hand side names are added to the undeclared repository
		switch (type.op) {
			case Type.SUBCLASS:
				// all subclass types are declared
				declared.get(type.type).add(type.lhs);
				
				for (int i = 0; i < type.rhs.length; i++) {
					OWLClass rhs = factory.getOWLClass(IRI.create(ns + "#" + type.rhs[i]));
					OWLAxiom axiom = factory.getOWLSubClassOfAxiom(lhs, rhs);
					manager.applyChange(new AddAxiom(ontology, axiom));
					undeclared.get(type.type).add(type.rhs[i]);
				}
				break;
			case Type.SUPERCLASS:
				// all superclass types are undeclared, until declared later
				undeclared.get(type.type).add(type.lhs);
				
				for (int i = 0; i < type.rhs.length; i++) {
					OWLClass rhs = factory.getOWLClass(IRI.create(ns + "#" + type.rhs[i]));
					OWLAxiom axiom = factory.getOWLSubClassOfAxiom(rhs, lhs);
					manager.applyChange(new AddAxiom(ontology, axiom));
					declared.get(type.type).add(type.rhs[i]);
				}
				break;
			case Type.DISJOINT:
				undeclared.get(type.type).add(type.lhs);
				for (int i = 0; i < type.rhs.length; i++) {
					OWLClass rhs = factory.getOWLClass(IRI.create(ns + "#" + type.rhs[i]));
					OWLAxiom axiom = factory.getOWLDisjointClassesAxiom(lhs, rhs);
					manager.applyChange(new AddAxiom(ontology, axiom));
					undeclared.get(type.type).add(type.rhs[i]);
				}
				break;
			case Type.EQUIVALENT:
				// all superclass types are undeclared, until declared later ???
				undeclared.get(type.type).add(type.lhs);
				for (int i = 0; i < type.rhs.length; i++) {
					OWLClass rhs = factory.getOWLClass(IRI.create(ns + "#" + type.rhs[i]));
					OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(rhs, lhs);
					manager.applyChange(new AddAxiom(ontology, axiom));
					undeclared.get(type.type).add(type.rhs[i]);
				}
				break;
				
			default:
				throw new ParseException("Unrecognized type operator: " + type.op);
		}
	}
	
	public OWLClassExpression compile(Datum datum) {
		OWLClassExpression dat;
		
		// return the generic datum class, if appropriate
		if (datum.equals(Datum.ANYTHING)) {
			return classDatum;
		}
		// define and declare the actor subclass
		else {
			dat = factory.getOWLClass(IRI.create(ns + "#" + datum.name));
			
			// if not declared, then note for later declaration
			if (!declared.get(Type.CLASS_DATUM).contains(datum.name)) {
				undeclared.get(Type.CLASS_DATUM).add(datum.name);
			}
		}
		
		return dat;
	}
	
	public OWLClassExpression compile(Modality mod) {
		return this.modality.get(mod);
	}
	
	public OWLClassExpression compile(Actor actor) {
		OWLClassExpression act;
		
		// reuse the generic actor class, if appropriate
		if (actor.equals(Actor.ANYONE)) {
			act = classActor;
		}
		// define and declare the actor subclass
		else {
			if (actor.name.equals("Actor")) {
				System.err.println();
			}
			act = factory.getOWLClass(IRI.create(ns + "#" + actor.name));
			
			// if not declared, then note for later declaration
			if (!declared.get(Type.CLASS_ACTOR).contains(actor.name)) {
				undeclared.get(Type.CLASS_ACTOR).add(actor.name);
			}
		}
		
		return act;
	}
	
	public OWLClassExpression compile(Purpose purpose) {
		OWLClassExpression purp;
		
		// reuse the generic datum class, if appropriate
		if (purpose.equals(Purpose.ANYTHING)) {
			purp = classPurpose;
		}
		// define and declare the actor subclass
		else {
			purp = factory.getOWLClass(IRI.create(ns + "#" + purpose.name));

			// if not declared, then note for later declaration
			if (!declared.get(Type.CLASS_PURPOSE).contains(purpose.name)) {
				undeclared.get(Type.CLASS_PURPOSE).add(purpose.name);
			}
		}

		return purp;
	}
	
	public OWLOntologyManager getManager() {
		return manager;
	}
	
	public OWLObjectProperty getRole(Role.Type type) {
		return roles.get(type);
	}
	
	public ArrayList<String> getUndeclaredTypes(Integer type) {
		return new ArrayList<String>(undeclared.get(type));
	}

	private void assumeDisjointnessProperty(Compilation comp, OWLClass c) {
		if (c.equals(factory.getOWLNothing())) {
			return;
		}
		
		OWLReasoner reasoner = comp.getReasoner();
		OWLOntology ontology = comp.getOntology();
		Set<OWLClass> set = reasoner.getSubClasses(c, true).getFlattened();
		
		// exclude the bottom class
		set.remove(factory.getOWLNothing());
		if (set.size() <= 1) {
			return;
		}

		// define each sibling class in the hierarchy to be disjoint
		OWLAxiom axiom = factory.getOWLDisjointClassesAxiom(set);
		manager.applyChange(new AddAxiom(ontology, axiom));
		
		// recurse on all subclasses
		for (OWLClass e : set) {
			assumeDisjointnessProperty(comp, e);
		}
	}
	
	private static class RoleValueCompiler implements RoleValueVisitor {
		private OWLClassExpression expr;
		private Compiler compiler;
		
		public RoleValueCompiler(Compiler compiler) {
			this.compiler = compiler;
		}
		
		public void visit(Actor actor) {
			expr = compiler.compile(actor);
		}
		public void visit(Datum datum) {
			expr = compiler.compile(datum);
		}
		public void visit(Purpose purpose) {
			expr = compiler.compile(purpose);
		}
	}
}
