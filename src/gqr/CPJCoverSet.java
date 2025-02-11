package gqr;

import java.util.ArrayList;
import java.util.List;

public class CPJCoverSet {

	
	List<CompositePredicateJoin> cpjs;
//	private boolean completeCover = false;
//	public CPJCoverSet(List<CompositePredicateJoin> pjs) {
//		this.cpjs=pjs;
//	}
	private int serialNo;

	public int getSerialNo() {
		return serialNo;
	}

	public CPJCoverSet() {
		cpjs = new ArrayList<CompositePredicateJoin>() ;
	}

	public boolean isEmpty() {
		return cpjs.isEmpty();
	}

//	public boolean isCompleteCover() {
//		return completeCover;
//	}

//	public void setCompleteCover(boolean completeCover) {
//		this.completeCover = completeCover;
//	}

	public List<CompositePredicateJoin> getCPJs() {
		return cpjs;
	}

	public void addAll(List<CompositePredicateJoin> l) {
		cpjs.addAll(l);
	}
	
	public void add(CompositePredicateJoin c) {
		cpjs.add(c);
	}

	public void setSerialNo(int serialNumber) {
		serialNo = serialNumber;
	}

}
