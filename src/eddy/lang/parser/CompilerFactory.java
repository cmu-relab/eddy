package eddy.lang.parser;

import java.util.ArrayList;

import org.semanticweb.owlapi.util.SimpleIRIMapper;

/**
 * Provides a factory method to create multiple {@link Compiler} objects. For example, this
 * is used in the {@link eddy.lang.analysis.CrossFlowTracer} to create unique compilers for 
 * each {@link eddy.lang.net.Agent} in a multi-party policy context. This requirement arises 
 * because a compiler is bound to the compilation once a policy is compiled.
 *  
 * @author Travis Breaux
 *
 */

public class CompilerFactory {
	private final ArrayList<SimpleIRIMapper> mappers = new ArrayList<SimpleIRIMapper>();
	
	public void addIRIMapper(SimpleIRIMapper mapper) {
		mappers.add(mapper);
	}
	
	public Compiler createCompiler() {
		Compiler compiler = new Compiler();
		
		for (SimpleIRIMapper mapper : mappers) {
			compiler.getManager().addIRIMapper(mapper);
		}
		return compiler;
	}
}
