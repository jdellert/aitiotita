package de.tuebingen.sfs.util.struct;

public class ComparableTriple <T extends Comparable<T>, U extends Comparable<U>, V extends Comparable<V>> extends Triple<T,U,V> implements Comparable<ComparableTriple<T,U,V>>
{

	public ComparableTriple(T first, U second, V third) 
	{
		super(first, second, third);
	}

	public int compareTo(ComparableTriple<T,U,V> otherTriple) 
	{
		int comparison = this.first.compareTo(otherTriple.first);
		if (comparison == 0)
		{
			comparison = this.second.compareTo(otherTriple.second);
		}
		if (comparison == 0)
		{
			comparison = this.third.compareTo(otherTriple.third);
		}
		return comparison;
	}
}