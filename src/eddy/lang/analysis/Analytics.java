package eddy.lang.analysis;

import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import eddy.lang.Policy;
import eddy.lang.Rule;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.CompilerConstants;

/**
 * Provides utility functions for querying a {@link Compilation} object.
 * 
 * @author Travis Breaux
 *
 */

public class Analytics implements CompilerConstants {
	
	public static ArrayList<Rule> findRules(Compilation comp, OWLNamedIndividual indiv) {
		OWLReasoner reasoner = comp.getReasoner();
		OWLDataFactory factory = comp.getOntology().getOWLOntologyManager().getOWLDataFactory();
		final OWLClass rule = factory.getOWLClass(IRI.create(nsRule));
		
		// find the types for the conflicted individual
		// recurse to include super classes, because conflicts may exist in subsumption hierarchies
		Set<OWLClass> types = reasoner.getTypes(indiv, false).getFlattened(); // why doesn't this return the types?
		
		// iterate over each id and lookup the rule from the policy
		ArrayList<Rule> rules = new ArrayList<Rule>();
		for (OWLClass c : types) {
			// check that each class is a subclass of Rule
			if (c.getIRI().equals(rule.getIRI())) {
				continue;
			}
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(c, rule);
			if (reasoner.isEntailed(axiom)) {
				String id = c.getIRI().getFragment().toString();
				Rule r = comp.getPolicy().getRule(id);
				if (r == null) {
					System.err.println("Could not find rule " + id );
				}
				rules.add(r);
			}
		}
		return rules;
	}
	
	public static ArrayList<Rule> findRules(Policy policy, OWLOntology onto, OWLNamedIndividual indiv) {
		OWLReasoner reasoner = new Reasoner(onto);
		OWLDataFactory factory = onto.getOWLOntologyManager().getOWLDataFactory();
		final OWLClass rule = factory.getOWLClass(IRI.create(nsRule));
		
		// find the types for the conflicted individual
		Set<OWLClass> types = reasoner.getTypes(indiv, true).getFlattened();

		// iterate over each id and lookup the rule from the policy
		ArrayList<Rule> rules = new ArrayList<Rule>();
		for (OWLClass c : types) {

			// check that each class is a subclass of Rule
			OWLAxiom axiom = factory.getOWLSubClassOfAxiom(c, rule);
			if (reasoner.isEntailed(axiom)) {
				String id = c.getIRI().getFragment().toString();
				Rule r = policy.getRule(id);
				rules.add(r);
			}
		}
		return rules;
	}
	
	/*
	public static Action findAction(Compilation comp, OWLNamedIndividual indiv) {
		OWLReasoner reasoner = comp.getReasoner();
		OWLDataFactory factory = comp.getOntology().getOWLOntologyManager().getOWLDataFactory();
		final OWLClass rule = factory.getOWLClass(IRI.create(nsRule));
		
		// find the types for the conflicted individual
		Set<OWLClassAssertionAxiom> set = comp.getOntology().getClassAssertionAxioms(indiv);
		for (OWLClassAssertionAxiom a : set) {
			OWLClassExpression expr = a.getClassExpression();
			
			// describe this expression as an Action
		}
		return null;
	}
	*/
	
	public static Set<OWLNamedIndividual> findIndividuals(Compilation comp, Rule rule) {
		OWLReasoner reasoner = comp.getReasoner();
		OWLDataFactory factory = comp.getOntology().getOWLOntologyManager().getOWLDataFactory();
		
		// create the class for this rule
		String ns = comp.getPolicy().getAttribute("NAMESPACE");
		OWLClass c = factory.getOWLClass(IRI.create(ns + "#" + rule.id));
		
		// find the corresponding instances
		Set<OWLNamedIndividual> set = reasoner.getInstances(c, false).getFlattened();
		return set;
	}
	
	public static Set<OWLNamedIndividual> findIndividuals(OWLOntology ontology, Rule rule) {
		OWLReasoner reasoner = new Reasoner(ontology);
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		
		// create the class for this rule
		String ns = ontology.getOntologyID().getOntologyIRI().toString();
		OWLClass c = factory.getOWLClass(IRI.create(ns + "#" + rule.id));
		
		// find the corresponding instances
		Set<OWLNamedIndividual> set = reasoner.getInstances(c, false).getFlattened();
		return set;
	}
}
