package eddy.example;

import java.io.File;
import java.util.ArrayList;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.analysis.CompilationProfile;
import eddy.lang.analysis.FlowTracer;
import eddy.lang.analysis.Tracer.Flow;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.Parser;

/**
 * Demonstrates how to trace data flows within a policy. The example uses the
 * {@link FlowTracer} to produce traces between collection and transfer actions.
 * 
 * @author Travis Breaux
 *
 */

public class FlowTracingExample {

	public static void main(String[] args) throws Exception {
		final String filestub = "examples/example.flow";
		final String exampleName = FlowTracingExample.class.getName();
		boolean useLocal = false;
		
		long time = System.currentTimeMillis();
		Parser parser = new Parser();
		Policy policy = parser.parse(new File(filestub + ".policy"));
		Compiler compiler = new Compiler();
		
		// use the local copy of the upper ontology
		if (useLocal) {
			IRI docIRI = IRI.create("http://gaius.isri.cmu.edu/2011/8/policy-base.owl");
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(new File("examples/policy-base.owl")));
			compiler.getManager().addIRIMapper(mapper);
		}
		
		// compile the policy
		Compilation comp = compiler.compile(policy);
		time = System.currentTimeMillis() - time;
		System.err.println(exampleName + ": Parsing policy... " + (time / 1000) + " secs");
		
		// find and report the traces
		FlowTracer tracer = new FlowTracer();
		tracer.addSource("COLLECT");
		tracer.addTarget("TRANSFER");
		ArrayList<Flow> flows = tracer.trace(comp, new Datum("purchase-order"));
		for (Flow f: flows) {
			System.err.println(exampleName + ": Flow from " + f.source.id + " to " + f.target.id + "; modes " + f.modes);
		}
		if (flows.size() > 0) {
			System.err.println(exampleName + ": Found " + flows.size() + " data flows");
		}
		else {
			System.err.println(exampleName + ": No flows found");
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
