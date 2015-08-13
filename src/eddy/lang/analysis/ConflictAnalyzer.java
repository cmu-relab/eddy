package eddy.lang.analysis;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import eddy.lang.Action;
import eddy.lang.Policy;
import eddy.lang.Rule;
import eddy.lang.parser.CompilationProperties;
import eddy.lang.parser.CompilerConstants;
import eddy.lang.parser.ParseException;

/**
 * Analyzes a {@link eddy.lang.parser.Compilation} for conflicts between policy rules. This class
 * depends on computing a valid {@link Extension} using the {@link ExtensionCalculator}, 
 * wherein each rule interpretation is itemized as a separate, subsumed concept. If the
 * itemized concept is in the class of Rights and Prohibitions, then it is deemed to
 * represent a conflicting interpretation.
 * 
 * @author Travis Breaux
 */

public class ConflictAnalyzer implements CompilerConstants, CompilationProperties {
	private TreeSet<Conflict> conflicts;
	private OWLDataFactory factory;
	private OWLReasoner reasoner;
	private Policy policy;
	private OWLClass classRule, classConflict;
	private Extension ext;
	
	public ConflictAnalyzer() {
		return;
	}
	
	public ArrayList<Conflict> analyze(Extension ext) throws ParseException {
		// reset global variables
		this.conflicts = new TreeSet<Conflict>();
		
		// set the compilation elements
		this.ext = ext;
		this.policy = ext.getPolicy();
		this.reasoner = ext.getReasoner();
		
		OWLOntology ontology = ext.getOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		this.classConflict = factory.getOWLClass(IRI.create(nsConflict));
		this.classRule = factory.getOWLClass(IRI.create(nsRule));
		
		/* Every class is subsumed by conflict, therefore:
		 * 
		 * If the class is a rule, then its a subclass of a rule or 
		 * interpretation of a rule to which it conflicts.
		 * 
		 * If the class if a interpretation, then its a subclass of
		 * two or more rules or interpretations of rules to which
		 * it conflicts.
		 */
		
		Set<OWLClass> set = reasoner.getSubClasses(classConflict, false).getFlattened();
		for (OWLClass clazz : set) {
			// process conflicts for this class, and recurse
			processConflicts(clazz);
		}

		// set the conflict property
		ext.getProperties().setProperty(RULE_CONFLICTS, conflicts.size() + "");
		
		return new ArrayList<Conflict>(conflicts);
	}
	
	private void processConflicts(OWLClass clazz) {
		if (clazz.equals(factory.getOWLNothing())) {
			return;
		}
		
		// find all rules superordinate to this clazz
		Set<Rule> supers = findSuperOrdinates(clazz);
		
		// lookup the rule for this clazz
		String id = clazz.getIRI().getFragment();
		Rule rule1 = policy.getRule(id);
		
		// if this is a rule, document the directed conflicts
		if (rule1 != null) {
			for (Rule rule2 : supers) {
				if (!rule1.modality.conflictsWith(rule2.modality)) {
					continue;
				}
				Conflict.Type type = Conflict.Type.SUBSUMED_BY;
				Action action = rule1.action.clone();
				Conflict conflict = new Conflict(ext, type, rule1, rule2, id, action);
				conflicts.add(conflict);
			}
		}
		// else, document the conflicts of shared interpretation
		else {
			for (Rule rule2 : supers) {
				for (Rule r : supers) {
					if (rule2.equals(r) || !rule2.modality.conflictsWith(r.modality)) {
						continue;
					}
					Conflict.Type type = Conflict.Type.SHARED;
					Action action = ext.getAction(id);
					Conflict conflict = new Conflict(ext, type, r, rule2, id, action);
					conflicts.add(conflict);
				}
			}
		}
		
		// recurse on subclasses
		Set<OWLClass> set = reasoner.getSubClasses(clazz, false).getFlattened();
		for (OWLClass c : set) {
			processConflicts(c);
		}
	}
	
	private TreeSet<Rule> findSuperOrdinates(OWLClass clazz) {
		
		// find the rules that are super classes of the given rule clazz
		OWLClassExpression expr = factory.getOWLObjectIntersectionOf(classRule, clazz);
		Set<OWLClass> set = reasoner.getSuperClasses(expr, true).getFlattened();

		// identify the rule id and map this to the rule
		TreeSet<Rule> rules = new TreeSet<Rule>();
		for (OWLClass c : set) {
			String id = c.getIRI().getFragment();
			Rule rule = policy.getRule(id);
			if (rule != null) {
				rules.add(rule);
			}
		}
		return rules;
	}
}
