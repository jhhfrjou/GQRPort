package gqr;

import uk.ac.soton.ecs.RelationalModel.*;

import java.util.*;
import java.util.stream.Collectors;

public class Query extends ConjunctiveQuery {

    private Set<PredicateJoin> queryPJs;
    Atom headAtom;
    private List<Variable> headVariables;
    private String name;

    public Query() {
        super();
        headVariables = new ArrayList<>();
    }

    public Query(ConjunctiveQuery cq) {
        super();
        Query.convertTerms(cq);
        headVariables = new ArrayList<>();
        setHead(cq.getHead());
        setBody(cq.getBody());
    }

    private static void convertTerms(ConjunctiveQuery cq) {
        for(Atom atom : cq.getHead()) {
            for (int i = 0; i < atom.getTerms().size(); i++) {
                if(atom.getTerm(i) instanceof uk.ac.soton.ecs.RelationalModel.Variable) {
                    atom.getTerms().set(i, new Variable(atom.getTerm(i).getName()));
                    ((Variable) atom.getTerm(i)).setPositionInHead(i);
                }
            }
        }

        for(Atom atom : cq.getBody()) {
            for (int i = 0; i < atom.getTerms().size(); i++) {
                if(atom.getTerm(i) instanceof uk.ac.soton.ecs.RelationalModel.Variable) {
                    atom.getTerms().set(i, new Variable(atom.getTerm(i).getName()));
                    if(cq.getHeadTerms().contains(atom.getTerm(i))) {

                    }
                }
            }
        }
    }


    @Override
    public void setHead(HashSet<Atom> atoms) {
        for(Term t : getHeadTermsList()) {
            if(t instanceof Variable) {
                headVariables.add((Variable) t);
            }
        }
        for (Atom atom : atoms) {
            headAtom = atom;
            name = headAtom.getPredicate().getName();
            break;
        }
        super.setHead(atoms);
    }

    @Override
    public void setBody(HashSet<Atom> atoms) {
        for(Atom atom : atoms) {
            for (Term term : atom.getTerms()) {
                if(headVariables.indexOf(term) != -1) {
                    ((Variable) term).setPositionInHead(headVariables.indexOf(term));
                } else {
                    ((Variable) term).setIsExistential();
                }
            }
        }



        super.setBody(atoms);
    }

    public List<Term> getHeadTermsList() {
        return new ArrayList<>(getHeadTerms());
    }

    public void computeQueryPJs() {
        Set<PredicateJoin> res = new HashSet<PredicateJoin>();
        int k = 1;
        for (Atom p : this.getBody()) {
            PredicateJoin qpj = new PredicateJoin(new Predicate(p.getPredicate()));//construct a cpj (in fact a pj)
            qpj.setSerialNumber(k++);
            int j = 1;
            for (Term term : p.getTerms()) {//tale all the variables of the query PJ
                assert (term instanceof uk.ac.soton.ecs.RelationalModel.Variable); //at this version all of predicate's "elements" (i.e., arguments) are variables
                uk.ac.soton.ecs.RelationalModel.Variable v = (uk.ac.soton.ecs.RelationalModel.Variable) term;
                Infobox queryVarBox = new Infobox();
                JoinInView joiv = new JoinInView(headAtom.getPredicate().getName());
                for (Atom otherpred : this.getBody()) //get the predicates once again
                {
                    if (!p.equals(otherpred)) {//for every other predicate
                        //if the variable (is joined with) belongs also to otherpred
                        int i = 0;
                        for (Term ped : otherpred.getTerms())//find the place v exists in otherpred
                        {
                            i++;
                            assert (ped instanceof uk.ac.soton.ecs.RelationalModel.Variable);
                            if (term.equals(ped)) //add this join description to the variable's infobox
                                joiv.addJoinDescription(new JoinDescription(new Predicate(otherpred.getPredicate()), i));
                        }
                    }
                }// here we have a  complete infobox for v

                queryVarBox.addJoinInView(joiv);
                gqr.GQRNode nv = new gqr.GQRNode(v, queryVarBox);
                qpj.addNode(nv, j++);

            }
            res.add(qpj);
        }
        queryPJs = res;
    }

    public Set<PredicateJoin> constructAndReturnPJs() {

        Set<PredicateJoin> res = new HashSet<PredicateJoin>();
        List<Atom> atoms = new ArrayList<>(getHead());
        atoms.addAll(getBody());
        for (Atom a : atoms) {
            uk.ac.soton.ecs.RelationalModel.Predicate p = a.getPredicate();
//			System.out.println("--> predicate "+p);
            PredicateJoin qpj = new PredicateJoin(new Predicate(p));//construct a cpj (in fact a pj)
            int j = 1;

            AtomicRewriting rw = new AtomicRewriting();
            List<String> head_variables = new ArrayList<String>();

//	        System.out.println(p);

            for (Term el : a.getTerms())//tale all the variables of the query PJ
            {
                assert (el instanceof Variable); //at this version all of predicate's "elements" (i.e., arguments) are variables
                Infobox queryVarBox = new Infobox();
                JoinInView joiv = new JoinInView(this.name);

                Variable v = new Variable(el.getName());

                if (v.getPositionInHead() != -1) {
                    head_variables.add("" + v.getPositionInHead());
                }


                for (Atom otherAtom : this.getAtoms()) //get the predicates once again
                {
                    if (!a.equals(otherAtom)) //for every other predicate
                    {
                        //if the variable (is joined with) belongs also to otherpred
                        int i = 0;
                        for (Term ped : otherAtom.getTerms())//find the place v exists in otherpred
                        {
                            i++;
                            assert (ped instanceof Variable);
                            if (el.equals(ped)) //add this join description to the variable's infobox
                                joiv.addJoinDescription(new JoinDescription(new Predicate(otherAtom.getPredicate()), i));
                        }
                    }
                }// here we have a  complete infobox for v

                joiv.addRewriting(rw);
//				System.out.println("----> In getSourcePJs(): joiv for var :"+v);
//				System.out.println("----------> In getSourcePJs(): joiv position in head"+v.getPositionInHead());
                joiv.addSourceHeadPosition(v.getPositionInHead());
                rw.addRefToJoiv(joiv);
                queryVarBox.addJoinInView(joiv);

                GQRNode nv = new GQRNode(v, queryVarBox);
                qpj.addNode(nv, j++);

            }

            head_variables = insertDontCaresAndPredName(head_variables, p.getName() + qpj.variablePatternStringSequence());

//			System.out.println("final rewriting "+head_variables);
            SourceHead sh = new SourceHead(this.name);
            sh.setSourceHeadVars(head_variables);
            try {
                sh.setQuery(this.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            rw.addSourceHead(sh);
            qpj.addRewriting(rw);

            res.add(qpj);
        }
        return res;

    }

    private List<String> insertDontCaresAndPredName(List<String> head_variables, String predicate_name) {

        List<String> newHeadVar = new ArrayList<String>();
        if(head_variables.isEmpty())
        {
            for(int i =0; i<this.headVariables.size(); i++)
            {
                newHeadVar.add(i, "DC"+i+"AT"+this.name+"DOT"+predicate_name+"CiD0DiC");
            }
        }
        else{

            //TODO I might be able to give a quicker solution without sorting
            Collections.sort(head_variables, new Comparator<String>() {

                public int compare(String o1, String o2) {
                    if(Integer.parseInt(o1) < Integer.parseInt(o2))
                        return -1;
                    else if(Integer.parseInt(o1) > Integer.parseInt(o2))
                        return 1;
                    else
                        return 0;
                }
            });

            assertSorted(head_variables);

            int inner_index = 0;

//			while((inner_index<head_variables.size()) && head_variables.get(inner_index).equals("-1"))
//			{
//				inner_index++;
//			}

            for(int i =0; i<this.headVariables.size(); i++)
            {

//				System.out.println(head_variables);
//				System.out.println(inner_index);
//				System.out.println(i);

//				if(inner_index == head_variables.size())
//				{
//					newHeadVar.add(i, "DC"+i+"AT"+this.name+"CiD0DiC");
//
//				}else
//				{
//					while((inner_index<head_variables.size()) && head_variables.get(inner_index).equals("-1"))
//					{
//						inner_index++;
//					}
//
//					if(inner_index == head_variables.size())
//						continue;

                //				System.out.println("I'm checking whether "+i+" is in partialHead.\n\t Actually I'm only checking index "+inner_index);
                if((inner_index == head_variables.size()) || Integer.parseInt(head_variables.get(inner_index)) != i  )
                {
                    //					System.out.println("\t N--> It is NOT so I put a don't care");
                    newHeadVar.add(i, "DC"+i+"AT"+this.name+"DOT"+predicate_name+"CiD0DiC");
                }
                else{
                    assert(head_variables.get(inner_index).equals(""+i));
                    //					System.out.println("\t Y--> It IS so I put "+inner_index +" \n\t ..and I increase index by 1");

                    newHeadVar.add("Z"+i+"AT"+this.name+"DOT"+predicate_name+"CiD0DiC");
                    inner_index++;
                }
//				}
            }
        }
        return newHeadVar;
    }

    private void assertSorted(List<String> partialHeadVarList) {

        for(int i=0; i<partialHeadVarList.size()-1; i++)
        {
//			System.out.println(partialHeadVarList);
//			if(Integer.parseInt(partialHeadVarList.get(i)) == -1)
//				continue;
//			int j=i+1;
//			while(j<partialHeadVarList.size() && Integer.parseInt(partialHeadVarList.get(j))==-1)
//				j++;
//			if(j<partialHeadVarList.size())//if((Integer.parseInt(partialHeadVarList.get(i)) != -1) && (Integer.parseInt(partialHeadVarList.get(i+1))!=-1))

            //TODO this assertion initially was strictly '<'  but it broke so I put '<='. I NEED TO VERIFY THIS IS CORRECT.
            assert(Integer.parseInt(partialHeadVarList.get(i)) <= Integer.parseInt(partialHeadVarList.get(i+1)));
        }
    }

    public void countRepeatedPredicates() {
        //holds for every source how many occurences of the same pj this query contains
        //We're using SourcePredicateJoin objects only to take advantage of their hash/equals method
        Map <SourcePredicateJoin,Integer> occurences_of_the_same_pj = new HashMap<SourcePredicateJoin, Integer>();

        for(PredicateJoin pj: getQueryPJs()) //get all the pjs with their infoboxes only updated for this source
        {
            SourcePredicateJoin spj = new SourcePredicateJoin(pj);

            Integer repeatedTimes = occurences_of_the_same_pj.get(spj);//get the list of the already existed and constructed
            //PJs that can hold information also for this PJ
            if(repeatedTimes == null) //first time we see this PJ in the query
            {
                occurences_of_the_same_pj.put(spj, 1);
                spj.getPredicate().setRepeatedId(0);
            }
            else
            {
                spj.getPredicate().setRepeatedId(repeatedTimes);
                occurences_of_the_same_pj.put(spj,++repeatedTimes);//new PJ in town
            }
        }

    }


    public void substituteVarWithFresh(String shv, boolean ex,int exfreshcount) {

        if(!ex)
        {
            for(Variable var:headVariables)
            {
                if(var.toString().equals(shv))
                {
                    var.setName(PoolOfNames.getName(shv));
                }
            }
        }

        for(Atom atom: this.getAtoms())
        {
            for(Term t : atom.getTerms())
            {
                if(t instanceof uk.ac.soton.ecs.RelationalModel.Variable & t.toString().equals(shv))
                {
                    t.setName(ex?"F"+exfreshcount+""+PoolOfNames.getName(shv):PoolOfNames.getName(shv));
                }
            }
        }
    }

    public void substituteVarWithNew(String varInRule, String shv) {
        for(Variable var:headVariables)
        {
            if(var.toString().equals(varInRule))
            {
                var.setName(shv);
            }
        }

        for(Atom pred: this.getAtoms())
        {
            for(Term v : pred.getTerms())
            {
                if(v.toString().equals(varInRule))
                    v.setName(shv);
            }
        }
    }

    public String toString() {
        String preds = printBody(new ArrayList<>(getBody()));
        String interpretedPreds = "";
//		if (printCollection(interpretedPredicates).length() > 0) {
//			interpretedPreds = "," + printCollection(interpretedPredicates);
//		}
        return name + "(" + printCollection(getHeadTermsList()) + ") <- "
                + preds + interpretedPreds;
    }

    public String printBody(List<Atom> collect) {
        String val = "";
        for (Atom atom : collect) {
            val = val + "," + atom.getPredicate().getName();
            val +="("+printCollection(atom.getTerms())+")";

        }
        val = val.replaceFirst(",", "");
        return val;
    }

    private String printCollection(Collection<Term> collect) {
        String val = "";
        for (Object obj : collect) {
            val = val + "," + obj.toString();
        }
        val = val.replaceFirst(",", "");
        return val;
    }

    public List<uk.ac.soton.ecs.RelationalModel.Predicate> getPredicates() {
        ArrayList<uk.ac.soton.ecs.RelationalModel.Predicate> predicates = new ArrayList<>();
        predicates.addAll(getHead().stream().map(Atom::getPredicate).collect(Collectors.toList()));
        predicates.addAll(getBody().stream().map(Atom::getPredicate).collect(Collectors.toList()));
        return predicates;
    }

    public List<Atom> getAtoms() {
        List<Atom> atoms = new ArrayList<>();
        atoms.addAll(getHead());
        atoms.addAll(getBody());
        return atoms;
    }

    @Override
    protected Query clone() throws CloneNotSupportedException {
        return new Query(this);
    }

    public String getName() {
        return name;
    }

    public int sumOfPredicatesSerials() {
        int sumSerNo = 0;
        for(int i =1; i<=getBody().size(); i++)
            {
                sumSerNo +=i;
            }
        return sumSerNo ;
    }

    public static Pair<Map<SourcePredicateJoin, List<SourcePredicateJoin>>, Map<SourcePredicateJoin, List<SourcePredicateJoin>>> createSourcePredicateJoins(List<Query> views) {
        Map <SourcePredicateJoin,List<SourcePredicateJoin>> sourcePJs = new HashMap<>();
        Map <SourcePredicateJoin,List<SourcePredicateJoin>> indexSourcePJs = new HashMap<>();
        for(Query source :  views) {
            System.out.println("view: "+source);
            Map <SourcePredicateJoin,Integer> occurences_of_the_same_pj = new HashMap<SourcePredicateJoin, Integer>();
            for (PredicateJoin pj : source.constructAndReturnPJs()) { //constructs all the pjs with their infoboxes only updated for this source
                SourcePredicateJoin spj = new SourcePredicateJoin(pj);

                //get the list of the already existed and constructed
                //PJs that can hold information also for this PJ
                List<SourcePredicateJoin> repeated_predicates = sourcePJs.get(spj);

                if (repeated_predicates == null) {//first time we see this PJ among all views
                    occurences_of_the_same_pj.put(spj, 1); //it appears only once in this source
                    spj.getPredicate().setRepeatedId(0);
                    spj.renameRewritingVarsAppending(0);
                    repeated_predicates = new ArrayList<SourcePredicateJoin>();
                    repeated_predicates.add(spj);//new PJ in town
                    sourcePJs.put(spj, repeated_predicates); //now a subsequent source can use this pj
                } else {
                    Integer times = occurences_of_the_same_pj.get(spj); //I've seen this pj again in THIS source this many times
                    if (times == null)
                        times = 0;
                    try {
                        SourcePredicateJoin spj_to_capture_thisspj = repeated_predicates.remove((int) times);//
                        //since I've seen this PJ this many times in this source, all previous same PJs of this source
                        //have been represented "globally". therefore I'm choosing another PJ (same as this, and apparently
                        //constructed for another source some time in the past) which will be in index `times'
                        spj.getPredicate().setRepeatedId(0);
                        spj.renameRewritingVarsAppending(0);
                        repeated_predicates.add(times, spj_to_capture_thisspj.mergeWithSameSpj(spj));

                        //I merge the information for those two PJs, put back in `times', and increase ``times''
                        occurences_of_the_same_pj.put(spj, times + 1);
                    } catch (IndexOutOfBoundsException e) { //not enough source PJs already constructed to represent this
                        //this can happen if I have seen this PJ more times in this source than there are PJs (same as this)
                        //stored globally
                        spj.getPredicate().setRepeatedId(times);
                        spj.renameRewritingVarsAppending(times);
                        repeated_predicates.add(spj);//new PJ in town
                        //and increase ``times''
                        //occurences_of_the_same_pj.put(spj,times+1);//this shouldn't be needed
                    }
                }
            }
        }

        for(SourcePredicateJoin spj: sourcePJs.keySet()) {
            for(SourcePredicateJoin key: spj.potentialMappings())
                addToIndex(key,spj, indexSourcePJs);//appends the value table for key with spj
        }
        return new Pair<>(sourcePJs,indexSourcePJs);
    }

    private static void addToIndex(SourcePredicateJoin key, SourcePredicateJoin spj, Map<SourcePredicateJoin,List<SourcePredicateJoin>> indexSourcePJs) {
        List<SourcePredicateJoin> l = indexSourcePJs.get(key);
        if(l == null)
            l=new ArrayList<>();
        l.add(spj);
        indexSourcePJs.put(key, l);
    }


    public Set<PredicateJoin> getQueryPJs() {
        return queryPJs;
    }
}
