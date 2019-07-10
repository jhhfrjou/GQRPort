package gqr;

public class GQRNode {

	private uk.ac.soton.ecs.RelationalModel.Variable variable;
	private Infobox infobox;
	private boolean isExistential;
	private String queryVar = null;

	public GQRNode(uk.ac.soton.ecs.RelationalModel.Variable v, Infobox box) {
		variable = v;
		infobox = box;
		if(v instanceof Variable)
			this.setExistential(((Variable) v).isExistential());
		else this.setExistential(false);
	}
	

	public boolean isExistential() {
		return isExistential;
	}

	public void setExistential(boolean isExistential) {
		this.isExistential = isExistential;
	}

	public uk.ac.soton.ecs.RelationalModel.Variable getVariable() {
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
		Variable v = new Variable(null);
		v.setIsExistential();
		return new GQRNode(v, null);
	}
	
	public static GQRNode dummyDistinguishedNode()
	{
		Variable v = new Variable(null);
		return new GQRNode(v, null);
	}
	
	@Override
	protected GQRNode clone() throws CloneNotSupportedException {
		return new GQRNode(((Variable) getVariable()).clone(),this.getInfobox().clone());
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
