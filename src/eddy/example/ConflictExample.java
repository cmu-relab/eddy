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
import eddy.lang.analysis.CompilationProfile;
import eddy.lang.analysis.Conflict;
import eddy.lang.analysis.ConflictAnalyzer;
import eddy.lang.analysis.Extension;
import eddy.lang.analysis.ExtensionCalculator;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.ParseException;
import eddy.lang.parser.Parser;

/**
 * Demonstrates how to find conflicts within a policy. Each {@link Conflict} is found by
 * first compiling the {@link Policy} into a {@link Compilation}, which is then used to
 * calculate the {@link Extension} using an {@link ExtensionCalculator}. The extension is
 * used by the {@link ConflictAnalyzer} to find conflicts.
 * 
 * @author Travis Breaux
 *
 */

public class ConflictExample {

	public static void main(String[] args) throws ParseException,OWLException {
		final String filestub = "examples/example.conflicts";
		final String exampleName = ConflictExample.class.getName();
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
			ExtensionCalculator.setOntologyBasePath("examples/policy-base.owl");
		}
		
		// compile the policy
		Compilation comp = compiler.compile(policy);
		time = System.currentTimeMillis() - time;
		System.err.println(exampleName + ": Parsing policy... " + (time / 1000) + " secs");
		
		// compute extension and detect conflicts
		System.err.print(exampleName + ": Detecting conflicts..");
		time = System.currentTimeMillis();
		
		ConflictAnalyzer analyzer = new ConflictAnalyzer();
		ExtensionCalculator calc = new ExtensionCalculator();
		Extension ext = calc.extend(comp);
		ArrayList<Conflict> conflicts = analyzer.analyze(ext);
		
		time = System.currentTimeMillis() - time;
		System.err.println(". " + (time / 1000) + " secs");
		
		// report the conflicts
		TreeSet<String> rules = new TreeSet<String>();
		for (Conflict c : conflicts) {
			System.err.println(exampleName + ": Conflict at " + c.toString());
			rules.add(c.rule1.id);
			rules.add(c.rule2.id);
		}
		if (conflicts.size() > 0) {
			System.err.println(exampleName + ": Found " + conflicts.size() + " conflicting interpretations across " + rules.size() + " rules");
		}
		else {
			System.err.println(exampleName + ": No conflicts found");
		}
		
		// save the ontology to a file for inspection
		OWLOntology ontology = comp.getOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		manager.saveOntology(ontology, IRI.create(new File(filestub + ".owl")));
		System.err.println(exampleName + ": Saved ontology as '" + filestub + ".owl'");
		
		System.err.println(exampleName + ": Finished.");
		CompilationProfile.computeProfile(comp);
		comp.printProperties(System.out);
	}
}
