package de.tuebingen.sfs.causal.heuristics.separation;

import java.util.List;
import java.util.Set;

public abstract class PartialCorrelation<T>
{
	protected T data;
	protected String[] varNames;
	
	public PartialCorrelation(T data, String[] varNames)
	{
		this.data = data;
		this.varNames = varNames;
	}
	
	public abstract double partialCorrelation(int xVar, int yVar, Set<Integer> zVars);
	
	public abstract boolean independenceTest(double partialCorrelation, int xVar, int yVar, Set<Integer> zVars);
	
	public boolean vStructureTest(int aVar, int bVar, int cVar)
	{
		return false;
	}
	
	public boolean implementsVStructureTest()
	{
		return false;
	}
	
	public void displayRelevantInformation()
	{
		
	}
	
	public void registerSepSets(int xVar, int yVar, List<Set<Integer>> sepSet)
	{
		
	}
	
	public String varSetToString(Set<Integer> varSet)
	{
		if (varSet.size() == 0) return "[]";
		StringBuilder string = new StringBuilder("[");
		for (int var : varSet)
		{
			string.append(varNames[var] + ",");
		}
		string.setCharAt(string.length() - 1, ']');
		return string.toString();
	}

	public boolean maxCondSetSizeReached(int xVar, int yVar, int condSetSize) {
		return false;
	}
}
