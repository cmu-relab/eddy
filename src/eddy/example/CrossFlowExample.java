package eddy.example;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import eddy.lang.Policy;
import eddy.lang.analysis.CrossFlowGrapher;
import eddy.lang.analysis.CrossFlowTracer;
import eddy.lang.analysis.CrossFlowTracer.CrossFlow;
import eddy.lang.net.Agent;
import eddy.lang.net.ServiceMap;
import eddy.lang.net.ServiceMapReader;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.CompilerFactory;
import eddy.lang.parser.Parser;

/**
 * Demonstrates how to trace data flows across two policies. The example first loads a
 * {@link ServiceMap} to align terminology between two policies, then loads each
 * {@link Policy} and map it to the {@link Agent}. These objects are used by the
 * {@link CrossFlowTracer} and the output of tracer is sent to the {@link CrossFlowGrapher}
 * to illustrate the traces.
 * 
 * @author Travis Breaux
 *
 */

public class CrossFlowExample {

	public static void main(String[] args) throws Exception {
		final String exampleName = CrossFlowExample.class.getName();
		final String filestub1 = "examples/example.stream1";
		final String filestub2 = "examples/example.stream2";
		boolean useLocal = false;

		// parse the agent policies
		Parser parser = new Parser();
		Policy policy1 = parser.parse(new File(filestub1 + ".policy"));
		Policy policy2 = parser.parse(new File(filestub2 + ".policy"));
		
		// parse the thesaurus
		ServiceMap map = ServiceMapReader.read(new FileReader(new File("examples/example.stream.thesaurus")));
		
		// create the agents and party relationships
		Agent agent1 = new Agent(URI.create("http://localhost:9001/agent1.owl"));
		agent1.setPolicy(policy1);
		Agent agent2 = new Agent(URI.create("http://localhost:9002/agent2.owl"));
		agent2.setPolicy(policy2);
		
		// use the local copy of the upper ontology
		CompilerFactory factory = new CompilerFactory();
		if (useLocal) {
			IRI docIRI = IRI.create("http://gaius.isri.cmu.edu/2011/8/policy-base.owl");
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(new File("examples/policy-base.owl")));
			factory.addIRIMapper(mapper);
		}
		
		// find and report the traces
		CrossFlowTracer tracer = new CrossFlowTracer();
		tracer.addCrossFlowSource("TRANSFER");
		tracer.addCrossFlowTarget("COLLECT");
		tracer.add(agent1);
		tracer.add(agent2);
		tracer.add(map);
		ArrayList<CrossFlow> flows = tracer.trace(factory);
		for (CrossFlow flow: flows) {
			System.err.println(exampleName + ": " + flow);
		}
		if (flows.size() > 0) {
			System.err.println(exampleName + ": Found " + flows.size() + " data streams");
		}
		else {
			System.err.println(exampleName + ": No streams found");
		}

		System.err.println(exampleName + ": Writing cross flow graph 'examples/example.stream.graphml'");
		CrossFlowGrapher grapher = new CrossFlowGrapher();
		grapher.graph(flows, new File("examples/example.stream.graphml"));
		
		// write the ontologies to files
		Compiler compiler1 = new Compiler();
		Compiler compiler2 = new Compiler();
		
		if (useLocal) {
			IRI docIRI = IRI.create("http://gaius.isri.cmu.edu/2011/8/policy-base.owl");
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(new File("examples/policy-base.owl")));
			compiler1.getManager().addIRIMapper(mapper);
			compiler2.getManager().addIRIMapper(mapper);
		}
		
		Compilation comp;
		comp = compiler1.compile(policy1);
		OWLOntology ontology1 = comp.getOntology();
		OWLOntologyManager manager1 = ontology1.getOWLOntologyManager();
		manager1.saveOntology(ontology1, IRI.create(new File(filestub1 + ".owl")));
		System.err.println(exampleName + ": Saved extended ontology as '" + filestub1 + ".owl'");
		
		comp = compiler2.compile(policy2);
		OWLOntology ontology2 = comp.getOntology();
		OWLOntologyManager manager2 = ontology2.getOWLOntologyManager();
		manager2.saveOntology(ontology2, IRI.create(new File(filestub2 + ".owl")));
		System.err.println(exampleName + ": Saved extended ontology as '" + filestub2 + ".owl'");
	}
}
