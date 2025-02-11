package gqr;

import java.util.ArrayList;
import java.util.List;

public class Infobox{

	private List<JoinInView> joinInViews = new ArrayList<JoinInView>();

	public List<JoinInView> getJoinInViews() {
		return joinInViews;
	}

	public void setJoinInViews(List<JoinInView> joinInViews) {
		this.joinInViews = joinInViews;
	}

	public void addJoinInView(JoinInView joiv) {
		joinInViews.add(joiv);
	}
	
	@Override
	public String toString() {
		String ret = "";
		for(JoinInView jv:joinInViews)
		{
			ret  += jv.toString()+"\n";
		}
		return ret;
	}
	
	@Override
	protected Infobox clone() throws CloneNotSupportedException {
		Infobox i = new Infobox();
		
		List<JoinInView> joins = new ArrayList<JoinInView>();
		for(JoinInView jv: this.getJoinInViews())
		{
			joins.add(jv.clone());
		}
		i.setJoinInViews(joins);
		return i;
	}
}
