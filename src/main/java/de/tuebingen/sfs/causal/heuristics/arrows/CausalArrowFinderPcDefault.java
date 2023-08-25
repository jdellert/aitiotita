package de.tuebingen.sfs.causal.heuristics.arrows;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.util.struct.Pair;

public class CausalArrowFinderPcDefault<T> extends CausalArrowFinder<T>
{
	public static boolean BASIC_INFO = true;
	public static boolean VERBOSE = false;
	
	public Map<Integer,Map<Integer,List<Set<Integer>>>> sepSets;
	public boolean stable;
	public boolean conservative;
	
	public CausalArrowFinderPcDefault(T data, String[] varNames, boolean stable, boolean conservative) 
	{
		super(data, varNames);
		this.sepSets = new TreeMap<Integer,Map<Integer,List<Set<Integer>>>>();
		this.stable = stable;
		this.conservative = conservative;
	}
	
	public void registerSepSets(int xVar, int yVar, List<Set<Integer>> newSepSets)
	{
		if (VERBOSE) System.out.println("registerSepSets(" + xVar + "," + yVar + "," + newSepSets + ")");
		if (newSepSets == null) return;
		Map<Integer,List<Set<Integer>>> submap = sepSets.get(xVar);
		if (submap == null)
		{
			submap = new TreeMap<Integer, List<Set<Integer>>>();
			sepSets.put(xVar, submap);
		}
		List<Set<Integer>> sepSetsForVarPair = submap.get(yVar);
		if (sepSetsForVarPair == null)
		{
			sepSetsForVarPair = new LinkedList<Set<Integer>>();
			submap.put(yVar, sepSetsForVarPair);
		}
		for (Set<Integer> sepSet : newSepSets)
		{
			sepSetsForVarPair.add(sepSet);
		}
	}

	@Override
	public List<String[]> findArrows(CausalGraph graph, boolean verbose) 
	{
		for (Integer[] triple : graph.listUnshieldedTriples())
		{
			if (VERBOSE) System.out.print("sepSets[" + varNames[triple[0]] + "][" + varNames[triple[1]] + "] = ");
			List<Set<Integer>> relevantSepSets = sepSets.get(triple[0]).get(triple[1]);
			if (VERBOSE) {
				List<Set<String>> sepSetsOutput = new LinkedList<Set<String>>();
				for (Set<Integer> sepSet : relevantSepSets) {
					Set<String> sepSetOutput = new TreeSet<String>();
					for (int var : sepSet) {
						sepSetOutput.add(varNames[var]);				
					}
					sepSetsOutput.add(sepSetOutput);
				}
				System.out.println(sepSetsOutput);
			}
			int sepSetsContainingK = 0;
			boolean firstSepSetContainsK = false;
			if (relevantSepSets.size() > 0) firstSepSetContainsK = relevantSepSets.get(0).contains(triple[2]);
			for (Set<Integer> sepSet : relevantSepSets)
			{
				if (sepSet.contains(triple[2]))
				{
					sepSetsContainingK++;
				}
			}
			if (VERBOSE) System.out.println("Separation sets containing " + varNames[triple[2]] + ": " + sepSetsContainingK + "/" + relevantSepSets.size());
			if (conservative)
			{
				if (sepSetsContainingK == 0)
				{
					if (BASIC_INFO) System.err.println("Found v-structure: " + varNames[triple[0]] + " -> " + varNames[triple[2]] + " <- " + varNames[triple[1]]);
					if (!graph.hasPresetEnd(triple[0], triple[2])) graph.putArrow(triple[0], triple[2], true);
					if (!graph.hasPresetEnd(triple[1], triple[2])) graph.putArrow(triple[1], triple[2], true);	
				}
			}
			else if (stable)
			{
				if (sepSetsContainingK <= relevantSepSets.size() / 2)
				{
					if (BASIC_INFO) System.err.println("Found v-structure: " + varNames[triple[0]] + " -> " + varNames[triple[2]] + " <- " + varNames[triple[1]]);
					if (!graph.hasPresetEnd(triple[0], triple[2])) graph.putArrow(triple[0], triple[2], true);
					if (!graph.hasPresetEnd(triple[1], triple[2])) graph.putArrow(triple[1], triple[2], true);	
				}
			}
			else
			{
				if (!firstSepSetContainsK)
				{
					if (BASIC_INFO) System.err.println("Found v-structure: " + varNames[triple[0]] + " -> " + varNames[triple[2]] + " <- " + varNames[triple[1]]);
					if (!graph.hasPresetEnd(triple[0], triple[2])) graph.putArrow(triple[0], triple[2], true);
					if (!graph.hasPresetEnd(triple[1], triple[2])) graph.putArrow(triple[1], triple[2], true);	
				}
			}
		}
		
		//phase 3: orient remaining edges according to DAG criterion
		boolean hasChanged = true;
		while (hasChanged)
		{
			hasChanged = false;
			
			//rules for propagating arrows (just as in PC algorithm)
			//R1: unshielded A *-> B o-* C => A *-> B --> C
			hasChanged |= OrientationRules.applyZhangOrientationRule1(graph, varNames);
			//R2: A *-> B -> C or A -> B *-> C and A *-o C => A *-> C
			hasChanged |= OrientationRules.applyZhangOrientationRule2(graph, varNames);
			//R3: v-structure A *-> B <-* C and A *-o D o-* C where D *-o B imply D *-> B
			hasChanged |= OrientationRules.applyZhangOrientationRule3(graph, varNames);
		}
		
		//slightly roundabout: extract the arrows from the graph we directly operated on,
		//these might be inserted once again by the calling code (which should not do any harm)
		//worse alternative would have been to silently modify the graph, and not return any arrows
		List<String[]> arrows = new LinkedList<String[]>();
		for (Pair<Integer,Integer> link : graph.listAllLinksInBothDirections())
		{
			if (graph.hasArrow(link.first, link.second))
			{
				arrows.add(new String[] {varNames[link.first], varNames[link.second]});
			}
		}
		
		return new LinkedList<String[]>(arrows);
	}

}
