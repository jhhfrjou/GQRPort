package gqr;

import uk.ac.soton.ecs.RelationalModel.*;
import uk.ac.soton.ecs.RelationalModel.Exceptions.InconsistentAtomException;

import java.util.ArrayList;
import java.util.List;

public class SourceHead extends Atom {

    private String sourceName;

    private List<Term> sourceHeadVars;

    private Query query;

    public SourceHead(String sourceName) throws InconsistentAtomException {
        super(new Predicate(sourceName,new PredicateSignature(new ArrayList<>())),new ArrayList<>());
        sourceHeadVars = super.getTerms();
        this.sourceName = sourceName;
    }

    public void addSourceHeadVar(Term i) {
        sourceHeadVars.add(i);
    }

    public List<Term> getSourceHeadVars() {
        return sourceHeadVars;
    }

    public void setSourceHeadVars(List<Term> sourceHeadVars) {
        this.sourceHeadVars.clear();
        this.sourceHeadVars.addAll(sourceHeadVars);
    }

    public String getSourceName() {
        return sourceName;
    }

    @Override
    public String toString() {
        String vars = "";
        for (int i = 0; i < sourceHeadVars.size(); i++) {
            if (i == sourceHeadVars.size() - 1)
                vars = vars + "?" + sourceHeadVars.get(i);
            else
                vars = vars + "?" + sourceHeadVars.get(i) + ", ";
        }
        return sourceName + "(" + vars + ") ";
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    @Override
    protected SourceHead clone() throws CloneNotSupportedException {
        SourceHead sh = null;
        try {
            sh = new SourceHead(this.sourceName);
        } catch (InconsistentAtomException e) {
            e.printStackTrace();
        }
        assert sh != null;
        sh.setQuery(getQuery().clone());
        List<Term> vars = new ArrayList<Term>();
        for (Term t : this.getSourceHeadVars()) {
            t = increaseClonedId(t);
            vars.add(t);
        }
        sh.setSourceHeadVars(vars);
        return sh;
    }

    private Term increaseClonedId(Term term) {
        String t = term.toString();
        String first_part = t.substring(0, t.indexOf("CiD") + 3);
//		System.out.println(first_part);
        String mid_part = t.substring(t.indexOf("CiD") + 3, t.indexOf("DiC"));
//		System.out.println(mid_part);
        mid_part = Integer.toString(Integer.parseInt(mid_part) + 1);
        String last_part = t.substring(t.indexOf("DiC"));
//		System.out.println(last_part);
        return new Variable(first_part + mid_part + last_part, DataType.STRING);
    }

    public SourceHead cloneDummy() throws InconsistentAtomException {
//		System.out.println("this is being called for "+this);
        SourceHead sh = new SourceHead(this.sourceName);
//		try {
        sh.setQuery(this.getQuery());
//		} catch (CloneNotSupportedException e) {
//			throw new RuntimeException(e);
//		}
        List<Term> vars = new ArrayList<Term>(getSourceHeadVars());
        sh.setSourceHeadVars(vars);
        return sh;
    }

    public void setSourceHeadVar(int i, Term varS1) {
        getSourceHeadVars().set(i, varS1);
    }

    public void setQuery(Query nQuery) {
        query = nQuery;
    }

    public Query getQuery() {
        return query;
    }

    SourceHead cloneAndSetSourceHeadVars(List<Term> newsourceheadvars) throws InconsistentAtomException {
        SourceHead sh = new SourceHead(this.sourceName);
        try {
            sh.setQuery(getQuery().clone());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        sh.setSourceHeadVars(newsourceheadvars);
        return sh;
    }


}
