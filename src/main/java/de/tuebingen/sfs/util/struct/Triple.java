package de.tuebingen.sfs.util.struct;

public class Triple<T,U,V>
{
	public T first;
	public U second;
	public V third;

	public Triple(T first, U second, V third)
	{
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	public String toString()
	{
		return "(" + first + "," + second + "," + third + ")";
	}
	
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof Triple)
		{
			Triple otherTriple = (Triple) o;
			return (otherTriple.first.equals(first) && otherTriple.second.equals(second) && otherTriple.third.equals(third));
		}
		return false;
	}
}
