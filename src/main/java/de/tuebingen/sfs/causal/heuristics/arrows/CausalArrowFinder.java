package de.tuebingen.sfs.causal.heuristics.arrows;

import java.util.List;
import java.util.Set;

import de.tuebingen.sfs.causal.data.CausalGraph;

public abstract class CausalArrowFinder<T>
{
	protected T data;
	protected String[] varNames;
	
	public CausalArrowFinder(T data, String[] varNames)
	{
		this.data = data;
		this.varNames = varNames;
	}
	
	public void registerSepSets(int xVar, int yVar, List<Set<Integer>> sepSets)
	{
	}
	
	public abstract List<String[]> findArrows(CausalGraph graph, boolean verbose);
}
