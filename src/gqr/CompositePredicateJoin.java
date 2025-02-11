package gqr;

import java.util.*;

public class CompositePredicateJoin {

    private List<SourcePredicateJoin> pjs;
    private List<CompRewriting> compRewritings;

    public CompositePredicateJoin() {
        pjs = new ArrayList<SourcePredicateJoin>();
        compRewritings = new ArrayList<CompRewriting>();
    }

    public List<CompRewriting> getRewritings() {
        return compRewritings;
    }

    public void addRewritings(CompRewriting compRewritings) {
        this.compRewritings.add(compRewritings);
    }

    public void setRewritings(List<CompRewriting> compRewritings) {
        this.compRewritings = compRewritings;
    }

    public void add(SourcePredicateJoin pj) {
        pjs.add(pj);
    }

    public List<SourcePredicateJoin> getPjs() {
        return pjs;
    }

    public void setPjs(List<SourcePredicateJoin> pjs) {
        this.pjs = pjs;
    }

    public void add(CompositePredicateJoin a) {
        pjs.addAll(a.getPjs());
    }

    public boolean emptyJoinInView(JoinInView jv) {

        Iterator<CompRewriting> it = this.getRewritings().iterator();
        while (it.hasNext()) //remove all the compRewritings that contain
        //the atomicRewritings in joininview
        {
            boolean remove = false;
            for (AtomicRewriting thisRewr : it.next().getAtomicRewritings()) {
                assert (jv.getRewritings().size() == 1);
                if (thisRewr.contains(jv.getRewritings())) {
                    remove = true;
                    break;
                }
            }

            if (remove) {
                it.remove();
            }

        }
        if (this.getRewritings().isEmpty())
            return true;


        //TODO I think I do have to do this
        for (SourcePredicateJoin spj : this.pjs) {
            if (spj.emptyJoinInView(jv))
                return true;
        }
        return false;
    }

    public CompositePredicateJoin cloneShallow() throws CloneNotSupportedException {

        CompositePredicateJoin a = new CompositePredicateJoin();
        List<SourcePredicateJoin> lspj = new ArrayList<SourcePredicateJoin>();

        for (SourcePredicateJoin spj : this.getPjs()) {
            lspj.add(spj.cloneShallow());
        }
        a.setPjs(lspj);

        List<CompRewriting> l = new ArrayList<CompRewriting>();
        for (CompRewriting ar : this.getRewritings()) {
            l.add(ar);
        }
        a.setRewritings(l);
        return a;
    }

}
