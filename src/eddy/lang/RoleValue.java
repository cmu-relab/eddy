package eddy.lang;

/**
 * Provides a Visitor Pattern interface to the value of a {@link Role}
 * 
 * @author Travis Breaux
 *
 */

public interface RoleValue {

	public void accept(RoleValueVisitor visitor);
}
