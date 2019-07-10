package gqr;

import isi.mediator.SourceQuery;
import uk.ac.soton.ecs.RelationalModel.Atom;
import uk.ac.soton.ecs.RelationalModel.Term;
import isi.mediator.VarTerm;

public class Util {
	
	public static SourceQuery castQueryAsISISourceQuery(Query query)
	{
		SourceQuery sq = new SourceQuery();

		for(Atom atom:query.getAtoms()) {
			isi.mediator.RelationPredicate isi_p = new isi.mediator.RelationPredicate(atom.getPredicate().getName());
			for(Term p_arg: atom.getTerms())
			{
				isi.mediator.Term t = new VarTerm(p_arg. getName());
				isi_p.addTerm(t);
			}
			sq.addPredicate(isi_p);
		}

		isi.mediator.RelationPredicate isi_head_p = new isi.mediator.RelationPredicate(query.getName());
		
		for(Term p_arg:query.getHeadTerms())
		{
			isi.mediator.Term t = new VarTerm(p_arg.getName());
			isi_head_p.addTerm(t);
		}
		sq.addHead(isi_head_p);
		
		return sq;
	}

}
