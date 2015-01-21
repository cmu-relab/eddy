package eddy.lang.analysis;

import java.util.Set;
import java.util.TreeMap;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import eddy.lang.Rule;
import eddy.lang.Type;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.CompilationProperties;
import eddy.lang.parser.CompilerConstants;

/**
 * Computes a set of statistics from the {@link Compilation}.
 * 
 * @author Travis Breaux
 *
 */

public class CompilationProfile implements CompilerConstants, CompilationProperties {
	public static void computeProfile(Compilation compilation) {
		OWLDataFactory factory = compilation.getOntology().getOWLOntologyManager().getOWLDataFactory();
		
		// define the final classes for building queries
		OWLClassExpression classRight = factory.getOWLClass(IRI.create(nsRight));
		OWLClassExpression classObligation = factory.getOWLClass(IRI.create(nsObligation));
		OWLClassExpression classProhibition = factory.getOWLClass(IRI.create(nsProhibition));
		OWLClassExpression classExclusionOfRight = factory.getOWLClass(IRI.create(nsExclusionOfRight));
		OWLClassExpression classExclusionOfObligation = factory.getOWLClass(IRI.create(nsExclusionOfObligation));
		OWLClassExpression classExclusionOfProhibition = factory.getOWLClass(IRI.create(nsExclusionOfProhibition));
		OWLClass classRule = factory.getOWLClass(IRI.create(nsRule));
		
		classRight = factory.getOWLObjectIntersectionOf(classRight, classRule);
		classObligation = factory.getOWLObjectIntersectionOf(classObligation, classRule);
		classProhibition = factory.getOWLObjectIntersectionOf(classProhibition, classRule);
		classExclusionOfRight = factory.getOWLObjectIntersectionOf(classExclusionOfRight, classRule);
		classExclusionOfObligation = factory.getOWLObjectIntersectionOf(classExclusionOfObligation, classRule);
		classExclusionOfProhibition = factory.getOWLObjectIntersectionOf(classExclusionOfProhibition, classRule);
		
		OWLReasoner reasoner = compilation.getReasoner();
		
		// tally the direct counts for each modality
		Set<OWLClass> set;
		OWLClass nothing = factory.getOWLNothing();
		set = reasoner.getSubClasses(classRight, true).getFlattened();
		set.remove(nothing);
		compilation.getProperties().setProperty(RULE_RIGHTS, set.size() + "");
		set = reasoner.getSubClasses(classObligation, true).getFlattened();
		set.remove(nothing);
		compilation.getProperties().setProperty(RULE_OBLIGATIONS, set.size() + "");
		set = reasoner.getSubClasses(classProhibition, true).getFlattened();
		set.remove(nothing);
		compilation.getProperties().setProperty(RULE_PROHIBITIONS, set.size() + "");
		set = reasoner.getSubClasses(classExclusionOfRight, true).getFlattened();
		set.remove(nothing);
		compilation.getProperties().setProperty(RULE_EX_RIGHT, set.size() + "");
		set = reasoner.getSubClasses(classExclusionOfObligation, true).getFlattened();
		set.remove(nothing);
		compilation.getProperties().setProperty(RULE_EX_OBLIGATION, set.size() + "");
		set = reasoner.getSubClasses(classExclusionOfProhibition, true).getFlattened();
		set.remove(nothing);
		compilation.getProperties().setProperty(RULE_EX_PROHIBITION, set.size() + "");
		
		// tally actions
		TreeMap<String,Integer> tally1 = new TreeMap<String,Integer>();
		Rule[] rule = compilation.getPolicy().rules();
		for (Rule r : rule) {
			Integer i = tally1.get("rule-action-" + r.action.name);
			if (i == null) {
				tally1.put("rule-action-" + r.action.name, 1);
			}
			else {
				tally1.put("rule-action-" + r.action.name, i + 1);
			}
			i = tally1.get("rule-mod-" + r.modality.toString());
			if (i == null) {
				tally1.put("rule-mod-" + r.modality.toString(), 1);
			}
			else {
				tally1.put("rule-mod-" + r.modality.toString(), i + 1);
			}
		}
		
		// tally types
				Type[] type = compilation.getPolicy().types();
				for (Type t : type) {
					Integer i = tally1.get("rule-type-" + t.type);
					if (i == null) {
						tally1.put("rule-type-" + t.type, 1);
					}
					else {
						tally1.put("rule-type-" + t.type, i + 1);
					}

					i = tally1.get("rule-op-" + t.op);
					if (i == null) {
						tally1.put("rule-op-" + t.op, 1);
					}
					else {
						tally1.put("rule-op-" + t.op, i + 1);
					}
				}
		for (String s : tally1.keySet()) {
			compilation.getProperties().setProperty(s, "" + tally1.get(s));
		}
		
		
	}
}
