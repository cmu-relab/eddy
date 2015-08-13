package eddy.lang.analysis;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.TreeMap;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import eddy.lang.Role;
import eddy.lang.Rule.Modality;
import eddy.lang.parser.Compiler;
import eddy.lang.parser.ParseException;

public class ConflictPrinter extends PrintWriter {
	private static final TreeMap<Modality,String> presentContinous = new TreeMap<Modality,String>();
	private static final TreeMap<Modality,String> past = new TreeMap<Modality,String>();
	private static final TreeMap<Modality,String> noun = new TreeMap<Modality,String>();
	
	static {
		presentContinous.put(Modality.PERMISSION, "permits");
		presentContinous.put(Modality.OBLIGATION, "requires");
		presentContinous.put(Modality.REFRAINMENT, "prohibits");
		past.put(Modality.PERMISSION, "permitted");
		past.put(Modality.OBLIGATION, "required");
		past.put(Modality.REFRAINMENT, "prohibited");
		noun.put(Modality.PERMISSION, "permission to");
		noun.put(Modality.OBLIGATION, "obligation to");
		noun.put(Modality.REFRAINMENT, "prohibition to not");
	}
	
	public ConflictPrinter(PrintStream stream) {
		super(stream);
	}
	
	public void print(Conflict c) {
		println("Rule " + c.rule1.id + " is a " + noun.get(c.rule1.modality) + " " + c.rule1.action.toString());
		println("Rule " + c.rule2.id + " is a " + noun.get(c.rule2.modality) + " " + c.rule2.action.toString());
		flush();		
		explain(c);
	}
	
	private void explain(Conflict c) {
		print("These rules conflict, because ");
		
		
		// explain the conflict based on modality, alone
		if (c.rule1.modality.isExclusion()) {
			String act = past.get(c.rule1.modality);
			if (act == null) {
				act = "performed";
			}
			println("rule " + c.rule1.id + " excludes the action " + act + " by rule " + c.rule2.id);
		}
		else if (c.rule1.modality.isPermissible()) {
			String act = presentContinous.get(c.rule1.modality);
			if (act == null) {
				act = "performs";
			}
			println("rule " + c.rule1.id + " " + act + " an action prohibited by rule " + c.rule2.id);
		}
		flush();
		
		// explain the relationship between the conflicting actions
		switch (c.type) {
			case SUBSUMES:
				println("Rule " + c.rule1.id + "'s action subsumes rule " + c.rule2.id + "'s action");
				flush();
				break;
			case SUBSUMED_BY:
				println("Rule " + c.rule1.id + "'s action is subsumed by rule " + c.rule2.id + "'s action");
				flush();
				break;
			case SHARED:
				println("These rules' actions share one or more interpretations.");
				flush();
				explainSharedInterpretations(c);
				break;
			case EQUIVALENT:
				println("These rules' actions are otherwise equivalent.");
				flush();
				break;
		}
	}
	
	public void explainSharedInterpretations(Conflict c) {
		if (c.type != Conflict.Type.SHARED) {
			return;
		}
		Compiler compiler = c.ext.getCompiler();
		
		Role[][] role = new Role[][] {
			new Role[] { c.rule1.action.getRole(Role.Type.OBJECT), c.rule2.action.getRole(Role.Type.OBJECT) },
			new Role[] { c.rule1.action.getRole(Role.Type.SOURCE), c.rule2.action.getRole(Role.Type.SOURCE) },
			new Role[] { c.rule1.action.getRole(Role.Type.TARGET), c.rule2.action.getRole(Role.Type.TARGET) },
			new Role[] { c.rule1.action.getRole(Role.Type.PURPOSE), c.rule2.action.getRole(Role.Type.PURPOSE) }
		};
		OWLClassExpression exp1, exp2;
		OWLAxiom axiom1, axiom2, axiom3;
		OWLReasoner reasoner = c.ext.getReasoner();
		OWLDataFactory factory = c.ext.getOntology().getOWLOntologyManager().getOWLDataFactory();
		try {
			for (Role[] r : role) {
				if (r[0] == null && r[1] == null) {
					continue;
				}
				exp1 = compiler.compile(r[0]);
				exp2 = compiler.compile(r[1]);
				axiom1 = factory.getOWLEquivalentClassesAxiom(exp1, exp2);
				axiom2 = factory.getOWLSubClassOfAxiom(exp1, exp2);
				axiom3 = factory.getOWLSubClassOfAxiom(exp2, exp1);
				
				if (reasoner.isEntailed(axiom1)) {
					println("Rule " + c.rule1.id + "'s " + r[0].toString() + " is equivalent to rule " 
							+ c.rule2.id + "'s " + r[1].toString());
				}
				else if (reasoner.isEntailed(axiom2)) {
					println("Rule " + c.rule1.id + "'s " + r[0].toString() + " subsumes rule " 
							+ c.rule2.id + "'s " + r[1].toString());
				}
				else if (reasoner.isEntailed(axiom3)) {
					println("Rule " + c.rule2.id + "'s " + r[1].toString() + " is subsumed by rule " 
							+ c.rule1.id + "'s " + r[0].toString());
				}
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
