package eddy.lang.analysis;

import java.util.Set;	
import java.util.TreeMap;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import eddy.lang.Datum;
import eddy.lang.Role;
import eddy.lang.RoleValueSet;
import eddy.lang.Rule;
import eddy.lang.parser.Compilation;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.ParseException;

/**
 * Traces a {@link Tracer.Flow} between two {@link eddy.lang.RoleValue} objects. If the trace
 * results in a shared interpretation, then the flow is categorized as either an
 * {@link Flow.Mode#EXACTFLOW}, {@link Flow.Mode#UNDERFLOW}, or 
 * {@link Flow.Mode#OVERFLOW}.
 * 
 * @author Travis Breaux
 *
 */

public abstract class Tracer {
	protected OWLDataFactory factory;
	protected OWLReasoner reasoner;
	protected Compiler compiler;
	
	protected void setCompilation(Compilation comp) {
		this.factory = comp.getOntology().getOWLOntologyManager().getOWLDataFactory();
		this.reasoner = comp.getReasoner();
		this.compiler = comp.getCompiler();
	}
	
	protected Flow.Mode getFlowRestriction(Rule rule, Datum datum) throws ParseException {
		RoleValueSet values = rule.action.getRole(Role.Type.OBJECT).values;
		OWLClassExpression source = compiler.compile(values);
		OWLClassExpression target = compiler.compile(datum);
		return getFlowRestriction(source, target);
	}
	
	protected Flow.Mode getFlowRestriction(OWLClassExpression source, OWLClassExpression target) {
		OWLAxiom axiom1 = factory.getOWLSubClassOfAxiom(source, target);
		OWLAxiom axiom2 = factory.getOWLSubClassOfAxiom(target, source);
		OWLAxiom axiom3 = factory.getOWLEquivalentClassesAxiom(source, target);
		Flow.Mode mode = null;
		
		if (reasoner.isEntailed(axiom3)) {
			mode = Flow.Mode.EXACTFLOW;
		}
		else if (reasoner.isEntailed(axiom2)) {
			mode = Flow.Mode.OVERFLOW;
		}
		else if (reasoner.isEntailed(axiom1)) {
			mode = Flow.Mode.UNDERFLOW;
		}
		else {
			Set<OWLClass> set = reasoner.getSubClasses(source, true).getFlattened();
			final OWLClass nothing = factory.getOWLNothing();
			for (OWLClass src : set) {
				if (src.equals(nothing)) {
					break;
				}
				else if (getFlowRestriction(src, target) != null) {
					mode = Flow.Mode.UNDERFLOW;
					break;
				}
			}
		}
		return mode;
	}
	
	/**
	 * Describes a flow between two {@link Rule} objects. The flow has one of the following
	 * modes to characterize the relationship between two {@link eddy.lang.RoleValue} in the rule: 
	 * {@link Flow.Mode#EXACTFLOW}, if the values are equivalent; {@link Flow.Mode#UNDERFLOW}, 
	 * if the source value is subsumed by the target value; or {@link Flow.Mode#OVERFLOW}, 
	 * if the source value subsumes the target value.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class Flow {
		/**
		 * Describes the kind of flow between two {@link eddy.lang.RoleValue} objects.
		 * 
		 * @author Travis Breaux
		 */
		
		public static enum Mode { EXACTFLOW, OVERFLOW, UNDERFLOW };
		public final Rule source, target;
		public final TreeMap<Role.Type,Mode> modes;
		
		public Flow(Rule source, Rule target, TreeMap<Role.Type,Mode> modes) {
			this.source = source;
			this.target = target;
			this.modes = new TreeMap<Role.Type,Mode>(modes);
		}
		
		public Set<Role.Type> getModeTypes() {
			return modes.keySet();
		}
		
		public Mode getMode(Role.Type type){
			return modes.get(type);
		}
		
		public String toString() {
			return "Flow(" + source.id + ":" + target.id + ", " + modes + ")";
		}
	}
}
