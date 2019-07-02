package gqr;

import java.util.ArrayList;
import java.util.List;

public class SourceHead {

	private String sourceName; 

	private List<String> sourceHeadVars;

	private DatalogQuery query;

	public SourceHead(String sourceName) {
		sourceHeadVars = new ArrayList<String>();
		this.sourceName =sourceName;
	}
	
	public void addSourceHeadVar(String i)
	{
		sourceHeadVars.add(i);
	}
	
	public List<String> getSourceHeadVars() {
		return sourceHeadVars;
	}

	public void setSourceHeadVars(List<String> sourceHeadVars) {
		this.sourceHeadVars = sourceHeadVars;
	}

	public String getSourceName() {
		return sourceName;
	}
	
	@Override
	public String toString() {
		String vars = "";
		for(int i= 0; i<sourceHeadVars.size(); i++)
		{
			if(i == sourceHeadVars.size()-1)
				vars = vars+sourceHeadVars.get(i);
			else	
				vars = vars+sourceHeadVars.get(i)+", ";
		}
		return sourceName+"("+vars+") ";
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	
	@Override
	protected SourceHead clone() throws CloneNotSupportedException {
		SourceHead sh = new SourceHead(this.sourceName);
		sh.setQuery(this.getQuery().clone());
		List<String> vars = new ArrayList<String>();
		for(String t: this.getSourceHeadVars())
		{
			t= increaseClonedId(t);
			vars.add(t);
		}
		sh.setSourceHeadVars(vars);
		return sh;
	}

	private String increaseClonedId(String t) {
		String first_part = t.substring(0, t.indexOf("CiD")+3);
//		System.out.println(first_part);
		String mid_part	= t.substring(t.indexOf("CiD")+3, t.indexOf("DiC"));
//		System.out.println(mid_part);
		mid_part = new Integer(Integer.parseInt(mid_part)+1).toString();
		String last_part = t.substring(t.indexOf("DiC"));
//		System.out.println(last_part);
		return first_part+mid_part+last_part;
	}

	public SourceHead cloneDummy() {
//		System.out.println("this is being called for "+this);
		SourceHead sh = new SourceHead(this.sourceName);
//		try {
			sh.setQuery(this.getQuery());
//		} catch (CloneNotSupportedException e) {
//			throw new RuntimeException(e);
//		}
		List<String> vars = new ArrayList<String>();
		for(String t: this.getSourceHeadVars())
		{
			vars.add(t);
		}
		sh.setSourceHeadVars(vars);
		return sh;
	}

	public void setSourceHeadVar(int i, String varS1) {
			getSourceHeadVars().set(i, varS1);
	}

	public void setQuery(DatalogQuery datalogQuery) {
		query = datalogQuery;
	}

	public DatalogQuery getQuery() {
		return query;
	}

	SourceHead cloneAndSetSourceHeadVars(List<String> newsourceheadvars) {
		SourceHead sh = new SourceHead(this.sourceName);
		try {
			sh.setQuery(this.getQuery().clone());
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		sh.setSourceHeadVars(newsourceheadvars);
		return sh;
	}
	
}
