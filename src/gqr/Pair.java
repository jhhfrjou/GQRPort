package gqr;

public class Pair<T,Y> {
	
	T A;
	Y B;
	
	public Pair(T a, Y b) {
		A = a;
		B = b;
	}

	public T getA() {
		return A;
	}

	public Y getB() {
		return B;
	}
	
	@Override
	public String toString() {
		return "("+A.toString()+"),("+B.toString()+")";
	}

}
