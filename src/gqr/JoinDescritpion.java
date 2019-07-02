package gqr;

public class JoinDescritpion {

	private Predicate predicate;
	private int edgeNo;

	public JoinDescritpion(Predicate otherpred, int i) {
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
	protected JoinDescritpion clone() throws CloneNotSupportedException {
		return new JoinDescritpion(this.predicate, this.getEdgeNo());//don't think we have to clone the predicate..
		//we'll leave it like this for now
		//TODO
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof JoinDescritpion))
			return false;
		//TODO should probably change. 
		//I use it for contains, when looking if two variable's infoboxes describe this join
		return ((JoinDescritpion)obj).toString().equals(this.toString());
//		JoinDescritpion other = ((JoinDescritpion)obj);
//		return (other.getPredicate() == this.predicate)&&(other.getEdgeNo()==this.getEdgeNo());
	}

	public boolean equalsIgnoreRepeatedID(JoinDescritpion queryJd) {
		if(!(queryJd instanceof JoinDescritpion))
			return false;
		//TODO to change if toString() cahnges and decide to print only predicates nameand not parameters!
		return new String("-|"+this.getPredicate()+" on:("+this.edgeNo+")|-").equals("-|"+queryJd.getPredicate()+" on:("+queryJd.edgeNo+")|-");
	}

	//This is the old equals
	public boolean equalsWithSamePred(JoinDescritpion obj) {
		if(!(obj instanceof JoinDescritpion))
			return false;
		JoinDescritpion other = ((JoinDescritpion)obj);
		return (other.getPredicate() == this.predicate)&&(other.getEdgeNo()==this.getEdgeNo());
	}
}
