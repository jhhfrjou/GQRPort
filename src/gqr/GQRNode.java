package gqr;

import uk.ac.soton.ecs.RelationalModel.DataType;
import uk.ac.soton.ecs.RelationalModel.Variable;

import java.lang.reflect.Method;

public class GQRNode {

	private Variable variable;
	private Infobox infobox;
	private boolean isExistential;
	private String queryVar = null;

	public GQRNode(Variable v, Infobox box) {
		variable = v;
		infobox = box;
		setExistential(v.isExistential());
	}
	

	public boolean isExistential() {
		return isExistential;
	}

	public void setExistential(boolean isExistential) {
		this.isExistential = isExistential;
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}

	public Infobox getInfobox() {
		return infobox;
	}

	public void setInfobox(Infobox infobox) {
		this.infobox = infobox;
	}
	
	public static GQRNode dummyExistentialNode()
	{
		Variable v = new Variable(null, DataType.INTEGER);
		v.setIsExistential();
		return new GQRNode(v, null);
	}
	
	public static GQRNode dummyDistinguishedNode()
	{
		Variable v = new Variable(null, DataType.INTEGER);
		return new GQRNode(v, null);
	}
	
	@Override
	protected GQRNode clone() throws CloneNotSupportedException {
		Variable clonedVariable = new Variable(variable.getName(), variable.getType());
		clonedVariable.setPositionInHead(variable.getPositionInHead());
		if (variable.isExistential()) {
			clonedVariable.setIsExistential();
		}
		return new GQRNode(clonedVariable, infobox.clone());

	}
	
	@Override
	public String toString() {
		return (isExistential ? "E " :"D ")+ infobox.toString();
	}


	public void addQueryVar(String name) {
		queryVar = name;
	}


	public String getQueryVar() {
//		if(queryVar == null)
//			throw new RuntimeException("uninitialized query variable reference");
		return queryVar;
	}
}
