package eddy.example;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import eddy.lang.Policy;
import eddy.lang.Rule;
import eddy.lang.analysis.CompilationProfile;
import eddy.lang.analysis.ExtensionCalculator;
import eddy.lang.analysis.LimitationPrinciple;
import eddy.lang.analysis.LimitationPrinciple.Violation;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.ParseException;
import eddy.lang.parser.Parser;

/**
 * Demonstrates how to apply the limitation principle to a single policy. The example defines
 * a set of collection rights and uses these permitted purposes to restrict the purposes of
 * the uses and transfers of the governed information. The computation is performed by the
 * {@link LimitationPrinciple} class.
 * 
 * @author Travis Breaux
 *
 */

public class LimitationExample {

	public static void main(String[] args) throws ParseException,OWLException {
		final String filestub = "examples/example.limit.use";
		final String exampleName = LimitationExample.class.getName();
		boolean useLocal = true;
		
		long time = System.currentTimeMillis();
		Parser parser = new Parser();
		Policy policy = parser.parse(new File(filestub + ".policy"));
		Compiler compiler = new Compiler();
		
		// use the local copy of the upper ontology
		if (useLocal) {
			IRI docIRI = IRI.create("http://gaius.isri.cmu.edu/2011/8/policy-base.owl");
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(new File("examples/policy-base.owl")));
			compiler.getManager().addIRIMapper(mapper);
			
			// tell extension calculator to use local ontology
			ExtensionCalculator.setOntologyBasePolicy("examples/policy-base.owl");
			LimitationPrinciple.setOntologyBasePolicy("examples/policy-base.owl");
		}
		
		// compile the policy
		Compilation comp = compiler.compile(policy);
		time = System.currentTimeMillis() - time;
		System.err.println(exampleName + ": Parsing policy... " + (time / 1000) + " secs");
		
		// compute the extension and detect violations
		System.err.print(exampleName + ": Computing limitations..");
		time = System.currentTimeMillis();
		LimitationPrinciple limit = new LimitationPrinciple();
		limit.addSource("COLLECT");
		limit.addTarget("USE");
		limit.addTarget("TRANSFER");
		ArrayList<Violation> violations = limit.analyze(comp);
		time = System.currentTimeMillis() - time;
		System.err.println(". " + (time / 1000) + " secs");
		
		// report the violations
		TreeSet<Rule> rules = new TreeSet<Rule>();
		for (Violation v : violations) {
			System.err.println(exampleName + ": Violation at " + v);
			rules.addAll(v.violators);
		}
		if (violations.size() > 0) {
			System.err.println(exampleName + ": Found " + violations.size() + " violationg interpretations across " + rules.size() + " rules");
		}
		else {
			System.err.println(exampleName + ": No violations found");
		}

		// save the ontology to a file for inspection
		OWLOntology ontology = comp.getOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		manager.saveOntology(ontology, IRI.create(new File(filestub + ".owl")));
		System.err.println(exampleName + ": Saved ontology as '" + filestub + ".owl'");
		
		Compilation extComp = limit.getExtendedCompilation();
		OWLOntology extOntology = extComp.getOntology();
		OWLOntologyManager extManager = extOntology.getOWLOntologyManager();
		extManager.saveOntology(extOntology, IRI.create(new File(filestub + ".extended.owl")));
		System.err.println(exampleName + ": Saved extended ontology as '" + filestub + ".extended.owl'");
		
		System.err.println(exampleName + ": Finished.");
		CompilationProfile.computeProfile(extComp);
		extComp.printProperties(System.out);
	}
}
