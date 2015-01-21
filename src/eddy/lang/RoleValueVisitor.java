package eddy.lang;

/**
 * Provides the Visitor pattern for {@link RoleValue} classes.
 * 
 * @author Travis Breaux
 *
 */

public interface RoleValueVisitor {

	public void visit(Actor actor);
	
	public void visit(Datum datum);
	
	public void visit(Purpose purpose);
}
