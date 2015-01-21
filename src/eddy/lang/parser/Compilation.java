package eddy.lang.parser;

import java.io.File;
import java.io.PrintStream;
import java.util.Properties;
import java.util.TreeSet;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;
import eddy.lang.Policy;

/**
 * Describes the {@link Compiler} result of a given {@link Policy}. The compilation includes
 * the policy, the compiled {@link OWLOntology} and {@link OWLReasoner}. These two artifacts
 * are used to perform various kinds of policy analysis (see {@link eddy.lang.analysis} for
 * available analytics).
 * 
 * @author Travis Breaux
 */

public class Compilation {
	private final Policy policy;
	private final Compiler compiler;
	private final OWLOntology ontology;
	private OWLReasoner reasoner;
	private final Properties properties = new Properties();
	
	public Compilation(Compiler compiler, Policy policy, OWLOntology ontology) {
		this.policy = policy;
		this.compiler = compiler;
		this.ontology = ontology;
		
		//this.reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
		//this.reasoner = new FaCTPlusPlusReasoner(ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
		this.reasoner = new Reasoner(ontology);
		//this.reasoner = new SnorocketOWLReasoner(ontology, new SimpleConfiguration(), false);
	}
	
	public Policy getPolicy() {
		return policy;
	}
	
	public OWLOntology getOntology() {
		return ontology;
	}
	
	public OWLReasoner getReasoner() {
		return reasoner;
	}
	
	public void refreshReasoner() {
		if (reasoner instanceof FaCTPlusPlusReasoner) {
			reasoner = new FaCTPlusPlusReasoner(ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
		}
		else {
			reasoner.flush();
		}
	}
	
	public Compiler getCompiler() {
		return compiler;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public void printProperties(PrintStream out) {
		TreeSet<String> keys = new TreeSet<String>(properties.stringPropertyNames());
		for (String key: keys) {
			out.println(key + "=" + properties.get(key));
		}
		out.flush();
	}
	
	public boolean save(File file) {
		try {
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			manager.saveOntology(ontology, IRI.create(file));
			
			// remap the ontology to this new file
			/*
			IRI docIRI = manager.getOntologyDocumentIRI(ontology);
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(file));
			manager.addIRIMapper(mapper);
			*/
		}
		catch (OWLOntologyStorageException e) {
			return false;
		}
		return true;
	}
}
