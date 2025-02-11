package gqr;

import uk.ac.soton.ecs.RelationalModel.Predicate;

public class JoinDescription {

	private Predicate predicate;
	private int edgeNo;

	public JoinDescription(Predicate otherpred, int i) {
		predicate = otherpred;
		edgeNo = i;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public void setPredicate(Predicate predicate) {
		this.predicate = predicate;
	}

	public int getEdgeNo() {
		return edgeNo;
	}

	public void setEdgeNo(int edgeNo) {
		this.edgeNo = edgeNo;
	}

	@Override
	public String toString() {
		return "-|"+predicate+"_"+predicate.getRepeatedId()+" on:("+edgeNo+")|-";
	}
	
	@Override
	protected JoinDescription clone() throws CloneNotSupportedException {
		return new JoinDescription(this.predicate, this.getEdgeNo());//don't think we have to clone the predicate..
		//we'll leave it like this for now
		//TODO
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof JoinDescription))
			return false;
		//TODO should probably change. 
		//I use it for contains, when looking if two variable's infoboxes describe this join
		return obj.toString().equals(this.toString());
//		JoinDescritpion other = ((JoinDescritpion)obj);
//		return (other.getPredicate() == this.predicate)&&(other.getEdgeNo()==this.getEdgeNo());
	}

	public boolean equalsIgnoreRepeatedID(JoinDescription queryJd) {
		if(queryJd == null)
			return false;
		//TODO to change if toString() cahnges and decide to print only predicates nameand not parameters!
		return ("-|" + this.getPredicate() + " on:(" + this.edgeNo + ")|-").equals("-|"+queryJd.getPredicate()+" on:("+queryJd.edgeNo+")|-");
	}

	//This is the old equals
	public boolean equalsWithSamePred(JoinDescription obj) {
		if(obj == null)
			return false;
		JoinDescription other = obj;
		return (other.getPredicate() == this.predicate)&&(other.getEdgeNo()==this.getEdgeNo());
	}
}
