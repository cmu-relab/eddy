package eddy.lang.analysis;

import java.util.TreeMap;

import org.semanticweb.owlapi.model.OWLOntology;

import eddy.lang.Action;
import eddy.lang.Policy;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;

/**
 * Describes the itemized {@link Action} interpretations for all {@link eddy.lang.Rule} in a 
 * {@link Policy}. The extension is computed using the {@link ExtensionCalculator}.
 *  
 * @author Travis Breaux
 *
 */

public class Extension extends Compilation {
	private final TreeMap<String,Action> extMap = new TreeMap<String,Action>();
	
	public Extension(Compiler c, Policy p, OWLOntology o, TreeMap<String,Action> map) {
		super(c, p, o);
		this.extMap.putAll(map);
	}
	public String[] getExtension() {
		return extMap.keySet().toArray(new String[extMap.size()]);
	}
	public Action getAction(String id) {
		return extMap.get(id);
	}
}
