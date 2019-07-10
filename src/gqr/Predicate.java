package gqr;

import uk.ac.soton.ecs.RelationalModel.PredicateSignature;
import uk.ac.soton.ecs.RelationalModel.Term;
import uk.ac.soton.ecs.RelationalModel.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Class predicate represents an body element of a datalog query that is NOT a
 * interpreted, i.e does not have a comparision of the elements.
 * 
 * A Predicate object constists of list of elements that can be both, variables
 * and constants. Moreover, it has two additional lists for variables and
 * constants.
 * 
 * @author Kevin Irmscher
 */
public class Predicate extends uk.ac.soton.ecs.RelationalModel.Predicate{

	private int repeatedId;

	public Predicate(String name, PredicateSignature signature) {
		super(name, signature);
		repeatedId = -1;
	}

	public Predicate(uk.ac.soton.ecs.RelationalModel.Predicate predicate) {
		super(predicate.getName(),predicate.getSignature());
		repeatedId = -1;
	}

	public void setRepeatedId(int i) {
		repeatedId  = i;
	}

	public int getRepeatedId() {
		if(repeatedId == -1)
			throw new RuntimeException("Unitialized Repeated Id");
		return repeatedId;
	}
}