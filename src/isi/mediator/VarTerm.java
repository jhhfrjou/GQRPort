package isi.mediator;
// __COPYRIGHT_START__
//
// Copyright 2009 University of Southern California. All Rights Reserved.
//
// __COPYRIGHT_END__

public class VarTerm extends Term{
	
	public VarTerm(){}
	public VarTerm(String term){
		var = term;
	}

	@Override
	public VarTerm clone(){
		VarTerm t = new VarTerm();
		t.var = var;
		return t;
	}
	
	@Override
	public boolean equals(Term t){
		if(!(t instanceof VarTerm))
			return false;
        return var.equals(t.var);
	}
	
	@Override
	public String toString(){
		return var;
	}

}

