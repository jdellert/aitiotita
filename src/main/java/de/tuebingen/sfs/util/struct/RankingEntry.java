package de.tuebingen.sfs.util.struct;

public class RankingEntry<T> implements Comparable<RankingEntry<T>>
{
	public T key;
	public double value;
	
	public RankingEntry(T key, double value)
	{
		this.key = key;
		this.value = value;
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof RankingEntry)
		{
			RankingEntry<T> otherEntry = (RankingEntry<T>) o;
			if (this.value != otherEntry.value) return false;
			return this.key.equals(otherEntry.key);
		}
		return false;
	}

	public int compareTo(RankingEntry<T> otherEntry) 
	{
		//System.err.println("Comparing ranking entries: " + this + " <-> " + otherEntry);
		if (this.value < otherEntry.value) return -1;
		if (this.value > otherEntry.value) return 1;
		if (key instanceof Comparable<?>)
		{
			return -(((Comparable<T>) this.key).compareTo((T) otherEntry.key));
		}
		return 0;
	}
	
	public String toString()
	{
		return key + "(" + value + ")";
	}
	
}
