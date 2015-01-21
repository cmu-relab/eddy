package eddy.lang.parser;

import java.io.File;
import java.util.ArrayList;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class ReasonerTest {
	private OWLOntology ontology;
	private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	private final OWLDataFactory factory = manager.getOWLDataFactory();
	private final String ns = "http://test.owl";
	public int T_HEIGHT;
	public int T_SPAN;
	
	public ReasonerTest() {
		return;
	}
	
	public void run() throws OWLException {
		this.ontology = manager.createOntology(IRI.create(ns));
	}
	
	private void generateConceptHierarchy(String prefix) {
		ArrayList<OWLClass> parents = new ArrayList<OWLClass>();
		OWLClass parent, child;
		int counter = 1, branches;
		
		parent = factory.getOWLClass(IRI.create(ns + "#" + prefix + "0"));
		parents.add(parent);
		
		// for each level, build the list of parents for the next iteration
		for (int i = 0; i < T_HEIGHT; i++) {
			int length = parents.size();
			
			for (int j = 0; j < length; j++) {
				parent = parents.get(j);
				
				// calculate the number of children for each parent
				branches = T_SPAN;
				
				for (int k = 0; k < branches; k++) {
					child = factory.getOWLClass(IRI.create(ns + "#" + prefix + counter));
					counter++;
					OWLAxiom axiom = factory.getOWLSubClassOfAxiom(child, parent);
					manager.addAxiom(ontology, axiom);
					parents.add(child);
				}
			}
			parents = new ArrayList<OWLClass>(parents.subList(length, parents.size()));
		}
	}
	
	private boolean save(File file) {
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
	
	public static void main(String[] args) throws OWLException {
		ReasonerTest test = new ReasonerTest();
		test.T_HEIGHT = 3;
		test.T_SPAN = 5;
		test.run();
		test.generateConceptHierarchy("C");
		test.save(new File("testfile"));
	}
}
