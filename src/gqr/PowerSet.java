package gqr;

/*
 * PowerSet.java	Version 1.0 	August 6, 2004
 *
 * Copyright 2004 Positronic Software. All Rights Reserved.
 *
 *
 */

import java.util.*;

/**
 * An iterator over the power set of a collection. The power set of a collection
 * is the set of all subsets of that collection.
 *
 * To use this class, one passes a collection to one of the constructors. The
 * PowerSet object produced is an Iterator, and one may use its hasNext and next
 * methods to produce all nonempty subsets of the given collection. Strictly
 * speaking, the power set of a collection includes the null or empty set, but
 * for practical reasons this class does not produce this set. Each application
 * of the next method produces an instance of the Vector class; this instance
 * contains a subset of the given collection.
 *
 * @author  Kerry M. Soileau
 * ksoileau@wt.net
 * http://web.wt.net/~ksoileau/index.htm
 * @version 1.0, 04/08/06
 
 * 
 * @see ArrayList
 * @see Collection
 * @see Iterator
 * @see Vector
 */
public class PowerSet implements Iterator
{
  private boolean[] membership;
  private Object[] array;

  public PowerSet(Object[] array)
  {
    this.array=array;
    this.membership=new boolean[this.array.length];
  }

  public PowerSet(Collection c)
  {
    this(c.toArray());
  }

  /**
     * Returns the next subset in the PowerSet.
     *
     * @return the next subset in the PowerSet.
     * @exception NoSuchElementException PowerSet has no more subsets.
     */
  public Object next()
  {
    boolean ok=false;
    for(int i=0;i<this.membership.length;i++)
      if(!this.membership[i])
      {
        ok=true;
        break;
      }
    if(!ok)
      throw(new NoSuchElementException("The next method was called when no more objects remained."));
    else
    {
      int n=0;
      this.membership[0]=!this.membership[0];
      boolean carry=!this.membership[0];
      while(n+1<this.membership.length)
      {
        n++;
        if(carry)
        {
          this.membership[n]=!this.membership[n];
          carry=!this.membership[n];
        }
        else break;
      }
      Vector vec=new Vector();
      for(int i=0;i<this.membership.length;i++)
        if(this.membership[i])
          vec.add(this.array[i]);

      return vec;
    }
  }

  /**
     *
     * Not supported by this class.
     *
     * @exception UnsupportedOperationException because the <tt>remove</tt>
     *		  operation is not supported by this Iterator.
     */
  public void remove()
  {
    throw new UnsupportedOperationException("The PowerSet class does not support the remove method.");
  }

  /**
     * Returns <tt>true</tt> if the PowerSet has more subsets. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return a subset
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the PowerSet has more subsets.
     */
  public boolean hasNext()
  {
    for(int i=0;i<this.membership.length;i++)
      if(!this.membership[i])
        return true;
    return false;
  }

  public static void main(String[] args)
  {
    Object[] ary={"a","b","c","d","e","f","a","b"};
    PowerSet powerSet1 = new PowerSet(ary);
    while(powerSet1.hasNext())
      System.out.println(powerSet1.next());
  }
}