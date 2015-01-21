package eddy.lang.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Random;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import eddy.lang.Policy;
import eddy.lang.analysis.ExtendedConflictAnalyzer;
import eddy.lang.parser.PolicyGenerator.Config;

public class ParserPerformanceTest {

	private static void write(String filename, String text) {
		FileWriter out;
		
		try {
			out = new FileWriter(filename);
			out.write(text);
			out.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void runTest(String name, int passes) throws Exception {
		Random random = new Random(System.currentTimeMillis());
		PolicyGenerator g = new PolicyGenerator(random);
		String[] text = new String[passes];
		String path = "study-profile";
		
		// Clear any existing profiling policies
		File dir=  new File(path);
		dir.mkdir();
		File[] f=dir.listFiles();
		
		int deleted = 0;
		for (int i = 0; i < f.length; i++) {
			if (f[i].delete())
				deleted++;
		}
		System.err.println("Cleared " + deleted + " of " + f.length + " stale test files.");
		
		// Profile #1: Performance as a factor of the concept tree heights
		
		// generate the list of configurations to profile
		Config[] config = new Config[text.length];
		for (int i = 0; i < config.length; i++) {
			config[i] = g.new Config();
			config[i].actorHeight = 4;
			config[i].actorSpan = 2;
			config[i].purposeHeight = 1;
			config[i].purposeSpan = 16;
			config[i].dataHeight = 4;
			config[i].dataSpan = 2;
			config[i].flows = (int) Math.pow(config[i].dataHeight, config[i].dataSpan) * 2;
			config[i].collectRights = (i + 1) * 2;
			config[i].useRights = 0;
			config[i].transferRights = 0;
			config[i].collectProhibitions =  (i / 2) + 1;
			config[i].namespace = "http://test" + i;
			//System.err.println("config " + i + ": " + config[i].toString());
			g.setConfig(config[i]);
			text[i] = g.generatePolicy();
			write(path + "/test" + i + ".policy", text[i]);
		}
		
		Parser parser = new Parser();
		parser.getLogger().setLogLevel(Logger.NONE);
		Policy[] policy = new Policy[text.length];
		long[] runtime = new long[text.length];
		
		//System.err.println("Name\tIndex\tTime\tRules\tMem\tConfs\tIndiv\tData");
		for (int i = 0; i < text.length; i++) {
			runtime[i] = System.currentTimeMillis();
			policy[i] = parser.parse(new StringReader(text[i]));
			
			// compile the policy using a local copy of the upper ontology
			Compiler compiler = new Compiler();
			
			// use the local policy base file to control runtimes
			IRI docIRI = IRI.create("http://gaius.isri.cmu.edu/2011/8/policy-base.owl");
			SimpleIRIMapper mapper = new SimpleIRIMapper(docIRI, IRI.create(new File("study/policy-base.owl")));
			compiler.getManager().addIRIMapper(mapper);
			Compilation comp = compiler.compile(policy[i]);
			
			// compute the extension
			OWLOntology ontology = comp.getOntology();
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			manager.saveOntology(ontology, IRI.create(new File("study-profile/test" + i + ".owl")));
			
			// analyze for conflicts
			ExtendedConflictAnalyzer analyzer = new ExtendedConflictAnalyzer();
			int c = analyzer.analyze(comp).size();
			
			runtime[i] = System.currentTimeMillis() - runtime[i];
			long mem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
			comp.save(new File(path + "/test" + i + ".owl"));
			String x = comp.getProperties().get(CompilationProperties.EXT_SIZE).toString();
			String y = "" + (int) (Math.pow(config[i].dataSpan, config[i].dataHeight));
			System.err.println(name + "\t" + i + "\t" + runtime[i] + "\t" + policy[i].rules().length +
					"\t" + config[i].collectProhibitions + "\t" + mem + "\t" + c + "\t" + x + "\t" + y);
			
			// call the garbage collector, since free memory affects performance
			System.gc();
		}
	}
	
	public static void main(String[] args) throws Exception {
		ParserPerformanceTest test = new ParserPerformanceTest();

		for (int i = 0; i < 100; i++) {
			test.runTest(i + "", 32);
		}
	}
}
