package gqr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class QueryPredicateJoin extends PredicateJoin{

	
	public QueryPredicateJoin(Predicate p) {
		super(p);
	}

	public void addNode(GQRNode nv, int j,int p) {
		super.addNode(nv,j);
	}

}
