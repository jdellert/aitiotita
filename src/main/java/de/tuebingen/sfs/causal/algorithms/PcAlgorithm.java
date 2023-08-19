package de.tuebingen.sfs.causal.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.causal.heuristics.arrows.CausalArrowFinder;
import de.tuebingen.sfs.causal.heuristics.arrows.OrientationRules;
import de.tuebingen.sfs.causal.heuristics.separation.PartialCorrelation;
import de.tuebingen.sfs.util.struct.Pair;
import de.tuebingen.sfs.util.struct.SetOperations;

public class PcAlgorithm {
	public static boolean BASIC_INFO = true;
	public static boolean VERBOSE = true;

	PartialCorrelation<?> corrMeasure;
	CausalArrowFinder<?> arrowFinder;
	String[] varNames;
	CausalGraph graph;
	int maxCondSetSize;
	boolean conservative;
	boolean stable;
	
	public Map<Integer,Map<Integer,List<Set<Integer>>>> separatingSets;
	public Map<Integer,Map<Integer,Double>> remainingLinkStrength;
	
	public PcAlgorithm(PartialCorrelation<?> corrMeasure, CausalArrowFinder<?> arrowFinder, String[] varNames, CausalGraph initialGraphWithConstraints, int maxCondSetSize, boolean stable, boolean conservative) {
		this.corrMeasure = corrMeasure;
		this.arrowFinder = arrowFinder;
		this.varNames = varNames;
		this.graph = initialGraphWithConstraints;
		this.maxCondSetSize = maxCondSetSize;
		this.stable = stable;
		this.conservative = conservative;
		
		this.separatingSets = new TreeMap<Integer,Map<Integer,List<Set<Integer>>>>();
		
	}
	
	public void run() {
		runSkeletonInference();
		runDirectionalityInference();
	}
	
	public void runSkeletonInference() {
		separatingSets = new TreeMap<Integer,Map<Integer,List<Set<Integer>>>>();
		
		//phase 1: create skeleton
		remainingLinkStrength = new TreeMap<Integer,Map<Integer,Double>>();	
		for (int var = 0; var < varNames.length; var++)
		{
			remainingLinkStrength.put(var, new TreeMap<Integer, Double>());
		}
		
		for (int depth = 0; depth <= maxCondSetSize; depth++)
		{
			List<Pair<Integer,Integer>> links = graph.listAllDeletableLinks();
			System.err.println("Proceeding to separating set size " + depth + ", " + links.size() + " deletable links left.");

			if (depth > 2)
			{
				//preprocessing for speedup: detect all links which persist even when conditioning on all neighbor nodes
				//and take them out of the iteration if they persist (upper bound criterion, detecting non-deletable nodes)
				int linkID = 0;
				for (Pair<Integer,Integer> link : links)
				{
					if (VERBOSE) System.err.print("checking link (" + varNames[link.first] + "," + varNames[link.second] + ")");
					Set<Integer> neighborVariables = new TreeSet<Integer>();
					neighborVariables.addAll(graph.getNeighbors(link.first));
					neighborVariables.addAll(graph.getNeighbors(link.second));
					neighborVariables.remove(link.first);
					neighborVariables.remove(link.second);
					double partialCorrelation = corrMeasure.partialCorrelation(link.first, link.second, neighborVariables);
					if (graph.hasPresetLink(link.first, link.second))
					{
						graph.setUndeletableLink(link.first, link.second, true);
						remainingLinkStrength.get(link.first).put(link.second, Math.max(partialCorrelation, 0.2));
						remainingLinkStrength.get(link.second).put(link.first, Math.max(partialCorrelation, 0.2));
						continue;
					}
					if (corrMeasure.independenceTest(partialCorrelation, link.first, link.second, neighborVariables))
					{
						if (VERBOSE) System.err.println("    successful independence test, link (" + varNames[link.first] + "," + varNames[link.second] + ") can be removed (overlap: " + partialCorrelation + ") by conditioning on all variables on connecting paths, i.e. " + varSetToString(neighborVariables));
					}
					else
					{
						if (VERBOSE) System.err.println("    found undeletable link (" + varNames[link.first] + "," + varNames[link.second] + "); did not disappear (overlap: " + partialCorrelation + ") even when conditioning on all variables on connecting paths, i.e. " + varSetToString(neighborVariables));
						graph.setUndeletableLink(link.first, link.second, true);
						remainingLinkStrength.get(link.first).put(link.second, partialCorrelation);	
						remainingLinkStrength.get(link.second).put(link.first, partialCorrelation);	
					}
					linkID++;
					if (VERBOSE) System.err.println("check " + linkID + "/" + links.size() + " complete!");
				}
				links = graph.listAllDeletableLinks();
			}
			
			//graph.outputToDotFormat();
			List<Pair<Integer,Integer>> linksToRemove = new LinkedList<Pair<Integer,Integer>>(); //only used in stable version
			for (Pair<Integer,Integer> link : links)
			{
				boolean presetLink = graph.hasPresetLink(link.first, link.second);
				boolean foundSepSet = false;
				double minPartialCorrelation = 1.0;
				if (depth > 0) minPartialCorrelation = remainingLinkStrength.get(link.first).get(link.second);
				List<Integer> neighbors1 = new ArrayList<Integer>();
				for (int neighbor : graph.getNeighbors(link.first))
				{
					if (neighbor != link.second) neighbors1.add(neighbor);
				}
				List<Set<Integer>> separatingSetCandidates1 = SetOperations.getSubsets(neighbors1, depth);
				if (!presetLink) if (BASIC_INFO) System.err.println("  Attempting to separate pair " + varNames[link.first] + "-" + varNames[link.second] + " using the " + neighbors1.size() + " neighbors of " + varNames[link.first] + ", forming " + separatingSetCandidates1.size() + " separating set candidates");
				else if (BASIC_INFO) System.err.println("  Minimizing partial correlation for fixed link " + varNames[link.first] + "-" + varNames[link.second] + " using the " + neighbors1.size() + " neighbors of " + varNames[link.first] + ", forming " + separatingSetCandidates1.size() + " separating set candidates");
				for (Set<Integer> separatingSetCandidate : separatingSetCandidates1)
				{
					double partialCorrelation = corrMeasure.partialCorrelation(link.first, link.second, separatingSetCandidate);
					if (partialCorrelation < minPartialCorrelation) minPartialCorrelation = partialCorrelation;
					if (!presetLink && corrMeasure.independenceTest(partialCorrelation, link.first, link.second, separatingSetCandidate))
					{
						if (BASIC_INFO) System.err.println("    successful independence test, link (" + varNames[link.first] + "," + varNames[link.second] + ") can be removed by conditioning on " + varSetToString(separatingSetCandidate));
						storeSepSet(link.first, link.second, separatingSetCandidate);
						storeSepSet(link.second, link.first, separatingSetCandidate);
						foundSepSet = true;
					}
				}
				List<Integer> neighbors2 = new ArrayList<Integer>();
				for (int neighbor : graph.getNeighbors(link.second))
				{
					if (neighbor != link.first) neighbors2.add(neighbor);
				}		
				List<Set<Integer>> separatingSetCandidates2 = de.tuebingen.sfs.util.struct.SetOperations.getSubsets(neighbors2, depth);
				if (!presetLink) if (BASIC_INFO) System.err.println("  Attempting to separate pair " + varNames[link.first] + "-" + varNames[link.second] + " using the " + neighbors2.size() + " neighbors of " + varNames[link.second] + ", forming " + separatingSetCandidates2.size() + " separating set candidates");
				else if (BASIC_INFO) System.err.println("  Minimizing partial correlation for fixed link " + varNames[link.first] + "-" + varNames[link.second] + " using the " + neighbors2.size() + " neighbors of " + varNames[link.second] + ", forming " + separatingSetCandidates2.size() + " separating set candidates");
				for (Set<Integer> separatingSetCandidate : separatingSetCandidates2)
				{
					double partialCorrelation = corrMeasure.partialCorrelation(link.first, link.second, separatingSetCandidate);
					if (partialCorrelation < minPartialCorrelation) minPartialCorrelation = partialCorrelation;
					if (!presetLink && corrMeasure.independenceTest(partialCorrelation, link.first, link.second, separatingSetCandidate))
					{
						if (BASIC_INFO) System.err.println("    successful independence test, link (" + varNames[link.first] + "," + varNames[link.second] + ") can be removed by conditioning on " + varSetToString(separatingSetCandidate));
						storeSepSet(link.first, link.second, separatingSetCandidate);
						storeSepSet(link.second, link.first, separatingSetCandidate);
						foundSepSet = true;
					}
				}
				if (foundSepSet)
				{
					corrMeasure.registerSepSets(link.first, link.second, separatingSets.get(link.first).get(link.second));
					//if (arrowFinder != null) arrowFinder.registerSepSets(link.first, link.second, sepSets.get(link.first).get(link.second));
					if (stable)
					{
						linksToRemove.add(link);
					}
					else
					{
						graph.removeLink(link.first, link.second);
					}
				}
				else
				{
					if (presetLink) remainingLinkStrength.get(link.first).put(link.second, 1.0);
					else remainingLinkStrength.get(link.first).put(link.second, minPartialCorrelation);
				}
			}
			if (stable)
			{
				for (Pair<Integer,Integer> link : linksToRemove)
				{
					graph.removeLink(link.first, link.second);
				}
			}
		}
		
		corrMeasure.displayRelevantInformation();
	}
	
	public void runDirectionalityInference( ) {
		
		if (arrowFinder != null)
		{
			for (int var1 = 0; var1 < varNames.length; var1++)
			{
				for (int var2 = 0; var2 < varNames.length; var2++)
				{
					if (var1 == var2) continue;
					arrowFinder.registerSepSets(var1, var2, separatingSets.get(var1).get(var2));
				}
			}
			
			for (String[] arrow : arrowFinder.findArrows(graph, true))
			{
				int arrowStart = Arrays.asList(varNames).indexOf(arrow[0]);
				int arrowEnd = Arrays.asList(varNames).indexOf(arrow[1]);
				if (!graph.hasPresetArrow(arrowStart, arrowEnd) && !graph.hasPresetArrow(arrowEnd, arrowStart))
				{
					if (VERBOSE) System.err.println("Adding arrow: " + arrow[0] + " -> " + arrow[1]);
					graph.putArrow(arrowStart, arrowEnd, true);
				}
			}
		}
		else
		{
			//phase 2: establish arrows by analyzing the unshielded triples
			for (Integer[] triple : graph.listUnshieldedTriples())
			{
				List<Set<Integer>> relevantSepSets = separatingSets.get(triple[0]).get(triple[1]);
				int sepSetsContainingK = 0;		
				for (Set<Integer> sepSet : relevantSepSets)
				{
					if (sepSet.contains(triple[2]))
					{
						sepSetsContainingK++;
					}
				}
				//System.err.println("Separation sets containing K: " + sepSetsContainingK + "/" + relevantSepSets.size());
				if (conservative)
				{
					if (sepSetsContainingK == 0)
					{
						if (BASIC_INFO) System.err.println("Found v-structure: " + varNames[triple[0]] + " -> " + varNames[triple[2]] + " <- " + varNames[triple[1]]);
						graph.putArrow(triple[0], triple[2], true);
						graph.putArrow(triple[1], triple[2], true);	
					}
				}
				else
				{
					if (sepSetsContainingK <= relevantSepSets.size() / 2)
					{
						if (BASIC_INFO) System.err.println("Found v-structure: " + varNames[triple[0]] + " -> " + varNames[triple[2]] + " <- " + varNames[triple[1]]);
						graph.putArrow(triple[0], triple[2], true);
						graph.putArrow(triple[1], triple[2], true);	
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
		}	
	}
	
	public  String varSetToString(Set<Integer> varSet)
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
	
	private void storeSepSet(Integer var1, Integer var2, Set<Integer> sepSet)
	{
		Map<Integer,List<Set<Integer>>> submap = separatingSets.get(var1);
		if (submap == null)
		{
			submap = new TreeMap<Integer, List<Set<Integer>>>();
			separatingSets.put(var1, submap);
		}
		List<Set<Integer>> sepSetsForVarPair = submap.get(var2);
		if (sepSetsForVarPair == null)
		{
			sepSetsForVarPair = new LinkedList<Set<Integer>>();
			submap.put(var2, sepSetsForVarPair);
		}
		sepSetsForVarPair.add(sepSet);
	}
}
