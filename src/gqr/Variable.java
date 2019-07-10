package gqr;

import uk.ac.soton.ecs.RelationalModel.DataType;

/**
 * This class extends PredicateArgument and represents a variable of a Datalog
 * query.
 */

public class Variable extends uk.ac.soton.ecs.RelationalModel.Variable {
	
	private boolean isExistential = false;
	private int positionInHead;

	/**
	 * Variable constructor. Calls the PredicateArgument constructor.
	 * 
	 * @param name (value) of variable
	 *            
	 */
	public Variable(String name) {
		super(name, DataType.INTEGER);
	}

	public boolean isExistential() {
		return isExistential;
	}

	@Override
	public boolean equals(Object elem) {
		return ((Variable)(elem)).getName().equals(this.getName());
	}

	public void setIsExistential() {
		isExistential = true;
		positionInHead = -1;//don't care
	}
	public int getPositionInHead() {
		return positionInHead;
	}

	public void setPositionInHead(int positionInHead) {
		this.positionInHead = positionInHead;
	}
	
	@Override
	protected Variable clone() throws CloneNotSupportedException {
		Variable var = new Variable(this.getName());
		if(this.isExistential())
			var.setIsExistential();
		var.setPositionInHead(this.getPositionInHead());
		return var;
	}

}
