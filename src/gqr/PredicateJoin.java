package gqr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.soton.ecs.RelationalModel.Predicate;

public class PredicateJoin {

	private Predicate pred;
	private Map<Integer,GQRNode> gqrNodes;
	private List<AtomicRewriting> atomicRewritings;
	private int serialNo;


	public PredicateJoin(final Predicate p) {
		this.pred = p;
		gqrNodes = new HashMap<>();
		atomicRewritings = new ArrayList<>();
	}

	public Predicate getPredicate() {
		return pred;
	}

	public void setPredicate(Predicate pred) {
		this.pred = pred;
	}

	public void addNode(GQRNode nv, int edgeNo) {
		gqrNodes.put(edgeNo,nv);
	}

	public Map<Integer, GQRNode> getGqrNodes() {
		return gqrNodes;
	}

	public void setGqrNodes(Map<Integer, GQRNode> gqrNodes) {
		this.gqrNodes = gqrNodes;
	}

	public void addRewriting(AtomicRewriting atomicRewriting) {
		atomicRewritings.add(atomicRewriting); 
	}

	public List<AtomicRewriting> getRewritings() {
		return atomicRewritings;
	}

	public void setRewritings(List<AtomicRewriting> atomicRewritings) {
		this.atomicRewritings = atomicRewritings;
	}

	public void setSerialNumber(int i) {
		serialNo = i;
	}
	
	public String variablePatternStringSequence()
	{
		String hash = "";
		for(int i=1; i<=getGqrNodes().size(); i++)
		{
			hash+=String.valueOf(i);
			hash+= getGqrNodes().get(i).isExistential()?"E":"D";
		}
		return hash;
	}

	public int getSerialNumber() {
		return serialNo;
	}
//	boolean isEmpty()
//	{
//		return false;
//	}

}
