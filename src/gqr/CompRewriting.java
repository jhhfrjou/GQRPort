package gqr;

import uk.ac.soton.ecs.RelationalModel.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

/**
 * This class Represents a conjunction of atomic rewritings, i.e., a partial conjunctive rewriting
 * 
 * @author george
 *
 */
public class CompRewriting {

	private List<AtomicRewriting> atomicRewritings;
	private List<Set<AtomicRewriting>> merges = new ArrayList<Set<AtomicRewriting>>();
	private Map<String,Set<String>> equates = new HashMap<String, Set<String>>() ;
	private String head;

	public String getHead() {
		return head;
	}

	public List<Set<AtomicRewriting>> getMerges() {
		return merges;
	}

	public Map<String,Set<String>> getEquates() {
		return equates;
	}

	public void setMerges(List<Set<AtomicRewriting>> merges) {
		this.merges = merges;
	}

	public void setEquates(Map<String,Set<String>> equates) {
		this.equates = equates;
	}

	public List<AtomicRewriting> getAtomicRewritings() {
		return atomicRewritings;
	}

	public void setAtomicRewritings(List<AtomicRewriting> atomicRewritings) {
		this.atomicRewritings = atomicRewritings;
	}

	public CompRewriting(String head)
	{
		this.head  = head;
		atomicRewritings = new ArrayList<AtomicRewriting>();
	}
	
	public void add(AtomicRewriting ar) {
		atomicRewritings.add(ar);
	}

	public CompRewriting merge(CompRewriting crB) {
		//TODO since we are returning a new object here, we shouldn't call it merge or take it out of here
		List<AtomicRewriting> newRewr = new ArrayList<AtomicRewriting>();
		newRewr.addAll(atomicRewritings);
		newRewr.addAll(crB.getAtomicRewritings());
		assert(this.getHead().equals(crB.getHead()));
		CompRewriting cmpr = new CompRewriting(head);
		cmpr.setAtomicRewritings(newRewr);
//		System.out.println("combine CPJs: >>>>>"+this+ " \n          and >>>>>"+crB);
		cmpr.setEquates(combineEquates(this.equates,crB.getEquates()));
		cmpr.setMerges(combineMerges(this.merges,crB.getMerges()));
		return cmpr;
	}

	private List<Set<AtomicRewriting>> combineMerges(List<Set<AtomicRewriting>> merges2,
			List<Set<AtomicRewriting>> merges3) {
		List<Set<AtomicRewriting>> l = new ArrayList<Set<AtomicRewriting>>();
//		System.out.println("Ccccccccombining merges: \n\t"+merges2);
//		System.out.println("Cccccccc with: \t"+merges3);
		l.addAll(merges2);
		l.addAll(merges3);
//		System.out.println("Cccccccc merges in the end: "+l);
		return l;
	}

	private Map<String, Set<String>> combineEquates(
			Map<String, Set<String>> eq, Map<String, Set<String>> eq2) {
		
		Map<String, Set<String>> m = new HashMap<String, Set<String>>();
//		System.out.println("eq1: "+eq);
//		System.out.println("eq2: "+eq2);

//		System.out.println("Putting eq in m");
		for(Entry<String, Set<String>> es: eq.entrySet())
		{
			m.put(es.getKey(), es.getValue());
		}

//		System.out.println("Iterating over eq2");
		for(Entry<String, Set<String>> es: eq2.entrySet())
		{
//			System.out.println("Looking for "+es.getKey()+" in the keys of m");
			Set<String> s = m.get(es.getKey());
			if(s == null)
			{
				m.put(es.getKey(), es.getValue());
//				System.out.println("Not found, so adding it with its values in m (value: "+es.getValue()+")"); 
			}
			else
			{
				Set<String> s1 = new HashSet<String>();
				s1.addAll(s);
				s1.addAll(es.getValue());
				m.put(es.getKey(), s1);
//				System.out.println("Found, adding the new value for the smae key in m (value: "+m.get(es.getKey())+")"); 
			}
		}
		return m;
	}

	@Override
	public String toString() {
		String ret = "";
		
		for(AtomicRewriting at: atomicRewritings)
		{
			assert(at.getSourceHeads().size() == 1);
			ret += at.getSourceHeads().iterator().next().toString()+", ";
		}
		
		ret = ret.substring(0, ret.lastIndexOf(", "));
		
		return head+ " :- "+ ret+" ----  equates: "+equates.toString();// + " merges: "+merges.toString()+"
	
//		return atomicRewritings.toString()+"merges: "+merges.toString()+"equates: "+equates.toString();
	}


	public AtomicRewriting removeAtomic(AtomicRewriting at) {
		 if(atomicRewritings.remove(at))
			 return at;
		 else
			 return null;
	}

	public Integer getAtomic(AtomicRewriting next) {
		 return atomicRewritings.indexOf(next);
	}

	public void addMerge(AtomicRewriting at1, AtomicRewriting at2) {
		
		Set<AtomicRewriting> first_set = null;
		Set<AtomicRewriting> second_set = null;

		boolean in =false;
		for(Set<AtomicRewriting> setj: merges)
		{	
			if((first_set==null) && setj.contains(at1))
			{
				setj.add(at2);
				in = true;
				first_set = setj;
			}
			else if((second_set==null) && setj.contains(at2))
			{
				setj.add(at1);
				in = true;
				second_set = setj;
			}
			if(first_set!=null && second_set!=null)
				break;
		}	
		
		if(first_set!=null && second_set!=null)
		{
			merges.remove(second_set);
			merges.get(merges.indexOf(first_set)).addAll(second_set);
		}
		if(!in)
		{
			Set<AtomicRewriting> hs = new HashSet<AtomicRewriting>();
			hs.add(at1);
			hs.add(at2);
			merges.add(hs);
		}
	}
	
	public String getExpansion()
	{
//		System.out.println("Rewriting : "+ this.toString());
		
//		System.out.println("Atomic Rewritings (view heads) : ");
		
		String exp = "";
		int freshvarcount = 1;
//		System.out.println("Getting the expansion of:"+this);
//		System.out.println("Going in atomic");
//		
		for(AtomicRewriting at: atomicRewritings)
		{

//			System.out.println("At1: "+at);
			assert(at.getSourceHeads().size() == 1);
			SourceHead sh = at.getSourceHeads().iterator().next();
			
			Query rule = sh.getQuery();
//			System.out.println(" 		Rule: "+rule);
			
			for(Term v:rule.getExistentialVariables())
			{
				rule.substituteVarWithFresh(v.getName(),true,freshvarcount);
			}
			freshvarcount++;
			
			for(int i=0; i<sh.getSourceHeadVars().size(); i++)
			{
				String shv = sh.getSourceHeadVars().get(i);
				
				rule.substituteVarWithFresh(shv,false,0);
//				System.out.println("		Rule after substituting shv:"+shv+" with fresh : "+rule);
				String varInRule = rule.getHeadTermsList().get(i).toString();
				
				rule.substituteVarWithNew(varInRule,shv);

//				System.out.println("		Rule after substituting varInruleAtPosi:"+varInRule+" with var in sh:"+shv +" "+rule);
				
			}
			
//			System.out.println(" 		Rule after unification: "+rule);
			
			exp+="," + rule.printBody(rule.getAtoms());
			
		}
		exp = exp.replaceFirst(",","");
		return head + " :- "+exp; 
	}
	
	public void addEquate(String string, String string2, String queryVar) {
//		String name1 = s1.getSourceName();
//		String name2 = s2.getSourceName();
//		if(string.equals(string2))
//		{
//			System.out.println("---> "+string);
//			System.out.println("---> "+string2);
//		}
		//TODO examine why the assert fails
//		assert(!string.equals(string2));
//		System.out.println("Rerwriting: "+this);
//		System.out.println("Equates so far: "+this.getEquates());
//		System.out.println("Adding equate "+string+" with "+string2);
		
		if(queryVar == null)
		{
			Set<String> strs = equates.get(string);
			Set<String> strs2 = equates.get(string2);
			if(strs != null)
				strs.add(string2);
			else if(strs2 != null)
				strs2.add(string);
			else
			{
				int count = 0;			
				for(Set<String> sr: equates.values())
				{
					if(sr.contains(string) || sr.contains(string2))
					{
						count++;
						//TODO uncomment and check
						assert(count == 1);
						//TODO here I can break;
						sr.add(string2);
						sr.add(string);
					}
				}
				if(count==0)
				{
						Set<String> hs = new HashSet<String>();
						hs.add(string2);
						equates.put(string,hs);	
				}
			}
			
		}
		else{
			Set<String> strs = equates.get(queryVar);

			if(strs==null)
			{
				Set<String> hs = new HashSet<String>();
				hs.add(string);
				hs.add(string2);
				equates.put(queryVar,hs);
			}
			else
			{
				strs.add(string2);
				strs.add(string);
			}
		}
	}
	
	@Override
	protected CompRewriting clone() throws CloneNotSupportedException {
		List<AtomicRewriting> l = new ArrayList<AtomicRewriting>();
		for(AtomicRewriting at: this.getAtomicRewritings())
			l.add(at.clone());
		CompRewriting cr = new CompRewriting(head);
		cr.setAtomicRewritings(l);
		cr.setEquates(this.getEquates());
		cr.setMerges(this.getMerges());
		return cr;
	}
	

	protected CompRewriting cloneDummy() throws CloneNotSupportedException {
		List<AtomicRewriting> l = new ArrayList<AtomicRewriting>();
		for(AtomicRewriting at: this.getAtomicRewritings())
			l.add(at.cloneDummy());
		CompRewriting cr = new CompRewriting(head);
		cr.setAtomicRewritings(l);
		cr.setEquates(this.getEquates());
		cr.setMerges(this.getMerges());
		return cr;
	}
	
//	protected CompRewriting cloneShallow() throws CloneNotSupportedException {
//		List<AtomicRewriting> l = new ArrayList<AtomicRewriting>();
//		for(AtomicRewriting at: this.getAtomicRewritings())
//			l.add(at);
//		CompRewriting cr = new CompRewriting(head);
//		cr.setAtomicRewritings(l);
//		cr.setEquates(this.getEquates());
//		cr.setMerges(this.getMerges());
//		return cr;
//	}
}
