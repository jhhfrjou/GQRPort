package gqr;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author george konstantinidis
 *
 * This class represents the part of a partial rewriting covering one atomic query subgoal with some CPJ
 */
public class AtomicRewriting {

	private List<SourceHead> sourceHeads;//normally this should be just an element
	//TODO change the above list to a single sourcehead if everything works out
	private List<JoinInView> joivs = new ArrayList<JoinInView>();
	//for all variable nodes in a CPJ, this is a list of the sourceboxes associated with this rewriting 

	boolean clonedForVariableRenamingDueToEquations = false;
	
	/**
	 * TODO returns the sourceboxes associated with this rewriting
	 * @return
	 */
	public List<JoinInView> getJoivs() {
		return joivs;
	}

    /**
     *  
     * @return
     */
	public List<SourceHead> getSourceHeads() {
		return sourceHeads;
	}

	/**
	 * 
	 * @param sourceHeads
	 */
	public void setSourceHeads(List<SourceHead> sourceHeads) {
		this.sourceHeads = sourceHeads;
	}


	public AtomicRewriting() {
		sourceHeads = new ArrayList<SourceHead>();	
	}
	
	
	public void addSourceHead(SourceHead i)
	{
		sourceHeads.add(i);
	}


	@Override
	public String toString() {
		return sourceHeads.toString();
	}
	
	@Override
	protected AtomicRewriting clone() throws CloneNotSupportedException {
		
		AtomicRewriting ret = new AtomicRewriting();
		List<SourceHead> sHeads = new ArrayList<SourceHead>();
		for(SourceHead sh: this.getSourceHeads())
			sHeads.add(sh.clone());
		ret.setSourceHeads(sHeads);
		return ret;
	}


//	public AtomicRewriting append(AtomicRewriting rB) {
//		this.sourceHeads.addAll(rB.getSourceHeads());
//		return this;
//	}


	public boolean contains(List<AtomicRewriting> atomicRewritings) {
		return this.sourceHeads.containsAll(atomicRewritings);
	}


	public void addRefToJoiv(JoinInView joiv) {
		joivs.add(joiv);
	}


	public AtomicRewriting cloneDummy() {
		AtomicRewriting ret = new AtomicRewriting();
		List<SourceHead> sHeads = new ArrayList<SourceHead>();
		for(SourceHead sh: this.getSourceHeads())
			sHeads.add(sh.cloneDummy());
		ret.setSourceHeads(sHeads);
		return ret;
	}

	AtomicRewriting cloneDummyAndSetSourceHeadVars( List<String> newsourceheadvars) 
	{
		AtomicRewriting ret = new AtomicRewriting();
		List<SourceHead> sHeads = new ArrayList<SourceHead>();
		assert(this.getSourceHeads().size() == 1);
		for(SourceHead sh: this.getSourceHeads())
			sHeads.add(sh.cloneAndSetSourceHeadVars(newsourceheadvars));
		ret.setSourceHeads(sHeads);
		return ret;
	}

}
