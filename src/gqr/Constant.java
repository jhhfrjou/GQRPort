package gqr;


/**
 * This class extends PredicateArgument and represents a constant of a Datalog
 * predicate.
 * 
 * @author Kevin Irmscher
 */

public abstract class Constant extends PredicateArgument {

	/**
	 * Constant constructor. Calls the constructor of class PredicateArgument.
	 * 
	 * @param name
	 *            (value) of constant
	 */
	public Constant(String name) {
		super(name);
	}

}
