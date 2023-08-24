package de.tuebingen.sfs.causal.heuristics.separation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.util.struct.Pair;
import de.tuebingen.sfs.util.struct.SetOperations;
import de.tuebingen.sfs.util.struct.Triple;

public class PartialCorrelationDiscreteUnitFlow extends PartialCorrelation<List<Set<Set<Triple<String,String,String>>>>> {

	public static boolean VERBOSE = true;
	
	CausalGraph graph;
	
	Map<String,Integer> varNameToID;
	
	//significance thresholds for the partial correlation values for each pair of variables
	double[][] thresholds;
	
	//collect overlapping elements for each variable pair which cannot be explained by any neighbor
	public boolean storeOverlaps = false;
	public Map<String,Map<String,Map<Integer,Set<Pair<String,String>>>>> unexplainedOverlaps;
	
	//precomputed maximal sizes of partitions connecting each pair of variables (to support early stops)
	public Map<String,Map<String,Integer>> maxCognateSetSizeForPair;
	
	List<Map<Integer,Set<Integer>>> cognateGraphs; 
	List<Set<Triple<String,String,String>>> elementUnits;
	
	Map<String,List<Set<Triple<String,String,String>>>> setsForLanguage;
	Map<Integer,Map<Integer,Set<Integer>>> setsForLanguagePair;
	
	public PartialCorrelationDiscreteUnitFlow(List<Set<Set<Triple<String, String, String>>>> samplePartitions, CausalGraph graph, String[] varNames, double[][] thresholds, boolean storeOverlaps) 
	{
		super(samplePartitions, varNames);
		
		varNameToID = new TreeMap<String,Integer>();
		for (int i = 0; i < varNames.length; i++)
		{
			varNameToID.put(varNames[i], i);
		}
		
		this.graph = graph;
		
		this.thresholds = thresholds;
		this.storeOverlaps = storeOverlaps;
		this.unexplainedOverlaps = new TreeMap<String,Map<String,Map<Integer,Set<Pair<String,String>>>>>();
		
		this.maxCognateSetSizeForPair = new TreeMap<String,Map<String,Integer>>();
		for (String lang1 : varNames)
		{
			Map<String, Integer> innerMap = new TreeMap<String, Integer>();
			for (String lang2 : varNames) {
				innerMap.put(lang2, 0);
			}
			this.maxCognateSetSizeForPair.put(lang1, innerMap);
		}
				
		this.setsForLanguage = new TreeMap<String,List<Set<Triple<String,String,String>>>>();
		this.setsForLanguagePair = new TreeMap<Integer,Map<Integer,Set<Integer>>>();
		for (String lang1 : varNames)
		{
			this.setsForLanguage.put(lang1, new LinkedList<Set<Triple<String,String,String>>>());
			Map<Integer,Set<Integer>> setsForLanguagePairLine = new TreeMap<Integer,Set<Integer>>();
			for (String lang2 : varNames)
			{
				Set<Integer> sets = new TreeSet<Integer>();
				setsForLanguagePairLine.put(varNameToID.get(lang2), sets);
			}
			this.setsForLanguagePair.put(varNameToID.get(lang1), setsForLanguagePairLine);
		}
		
		if (storeOverlaps)
		{
			for (String lang1 : varNames)
			{
				Map<String,Map<Integer,Set<Pair<String,String>>>> unexplainedCognatesSubmap = new TreeMap<String,Map<Integer,Set<Pair<String,String>>>>();
				for (String lang2 : varNames)
				{
					unexplainedCognatesSubmap.put(lang2, new TreeMap<Integer,Set<Pair<String,String>>>());
				}
				unexplainedOverlaps.put(lang1, unexplainedCognatesSubmap);
			}
		}
		
		//in each "cognate graph", each node is a neighbor to itself (for storage efficiency), null encodes non-presence of cognate at the key
		cognateGraphs = new LinkedList<Map<Integer,Set<Integer>>>();
		elementUnits = new LinkedList<Set<Triple<String,String,String>>>();
		
		for (int i = 0; i < data.size(); i++)
		{
			Set<Set<Triple<String,String,String>>> samplePartition = data.get(i);
			for (Set<Triple<String,String,String>> cognateSet : samplePartition)
			{		
				Set<Integer> coveredVarSet = new TreeSet<Integer>();
				for (Triple<String,String,String> cognate1 : cognateSet)
				{
					int cognateSetSizeMinusTwo = cognateSet.size() - 2;
					Integer id1 = varNameToID.get(cognate1.first);
					if (id1 != null) coveredVarSet.add(id1);
					if (id1 == null || this.setsForLanguagePair.get(id1) == null) continue;
					this.setsForLanguage.get(cognate1.first).add(cognateSet);
					//System.err.println("Adding to sets for language " + cognate1.first + ": ");
					//System.err.println("  " + cognateSet);
					for (Triple<String,String, String> cognate2 : cognateSet)
					{
						Integer id2 = varNameToID.get(cognate2.first);
						if (id2 == null || this.setsForLanguagePair.get(id2) == null) continue;
						this.setsForLanguagePair.get(id1).get(id2).add(elementUnits.size());
						this.setsForLanguagePair.get(id2).get(id1).add(elementUnits.size());
						int previousMaxSize = this.maxCognateSetSizeForPair.get(cognate1.first).get(cognate2.first);
						if (cognateSetSizeMinusTwo > previousMaxSize) {
							this.maxCognateSetSizeForPair.get(cognate1.first).put(cognate2.first, cognateSetSizeMinusTwo);
							this.maxCognateSetSizeForPair.get(cognate2.first).put(cognate1.first, cognateSetSizeMinusTwo);
						}
						//System.err.println("  overlap for " + cognate1.first + "/" + cognate2.first + " expanded by cognate set ID " + correlateSets.size());
						//System.err.println("  overlap for " + cognate2.first + "/" + cognate1.first + " expanded by cognate set ID " + correlateSets.size());
					}
				}
				Map<Integer,Set<Integer>> cognateGraph = new TreeMap<Integer,Set<Integer>>();
				for (int var : coveredVarSet)
				{
					cognateGraph.put(var, coveredVarSet);
				}
				//System.err.println("COGSET2CONCEPT" + "\t" + correlateSets.size() + "\t" + i);
				cognateGraphs.add(cognateGraph);
				elementUnits.add(cognateSet);
			}	
		}
	}
	
	private boolean dfsSearchOnCurrentGraph(int xVar, int yVar, Set<Integer> zVars)
	{
		Set<Integer> processed = new HashSet<Integer>();
		Queue<Integer> agenda = new LinkedList<Integer>();
		agenda.add(xVar);
		while (agenda.size() > 0)
		{
			int candidate = agenda.remove();
			if (processed.contains(candidate)) continue;
			if (candidate == yVar) return true;
			Set<Integer> neighbors = graph.getNeighbors(candidate);
			//System.err.println("  neighbors = " + neighbors);
			for (int neighborVar : neighbors)
			{
				if (candidate == xVar && neighborVar == yVar) continue;
				if (neighborVar == yVar || (zVars.contains(neighborVar) && !processed.contains(neighborVar)))
				{
					agenda.add(neighborVar);
				}
			}
			processed.add(candidate);
		}
		return false;
	}
	
	private Set<Integer> parallelDfsSearchOnCurrentGraph(int xVar, int yVar, Set<Integer> zVars)
	{
		boolean verbose = false;
		Set<Integer> flowArrived = new TreeSet<Integer>();
		//store the cognate set IDs that have already been pushed from each node
		Map<Integer,Set<Integer>> processedFlows = new HashMap<Integer,Set<Integer>>();
		//store the cognate set IDs that have not yet been pushed from the current node
		Map<Integer,Set<Integer>> unprocessedFlows = new HashMap<Integer,Set<Integer>>();
		//agenda of nodes to revisit
		Queue<Integer> agenda = new LinkedList<Integer>();
		agenda.add(xVar);
		unprocessedFlows.put(xVar, setsForLanguagePair.get(xVar).get(xVar));
		while (agenda.size() > 0)
		{
			int candidate = agenda.remove();
			//set can be destroyed after this
			Set<Integer> unprocessedFlow = unprocessedFlows.remove(candidate);
			if (verbose) System.err.println("Processing " + unprocessedFlow.size() + " units of unprocessed flow at " + varNames[candidate]);
			unprocessedFlow.removeAll(flowArrived);
			if (unprocessedFlow.size() == 0) {
				unprocessedFlows.put(candidate, new TreeSet<Integer>());
				continue;
			}
			if (candidate == yVar) {
				if (verbose) System.err.println("  " + unprocessedFlow.size() + " units of flow arrived at goal variable " + varNames[candidate]);
				flowArrived.addAll(unprocessedFlow);
			}
			Set<Integer> neighbors = graph.getNeighbors(candidate);
			//System.err.println("  neighbors = " + neighbors);
			for (int neighborVar : neighbors)
			{
				if (candidate == xVar && neighborVar == yVar) continue; //exclude the direct link
				//if (verbose) System.err.println("  neighbor: " + varNames[neighborVar]);
				if (neighborVar == yVar || zVars.contains(neighborVar))
				{
					//add unprocessed flow on neighbor from the intersection of candidate and neighbor
					Set<Integer> neighborIntersect = setsForLanguagePair.get(candidate).get(neighborVar);
					Set<Integer> processedFlowOnNeighbor = processedFlows.get(neighborVar);
					Set<Integer> unprocessedFlowOnNeighbor = unprocessedFlows.get(neighborVar);
					if (unprocessedFlowOnNeighbor == null) {
						unprocessedFlowOnNeighbor = new TreeSet<Integer>();
						unprocessedFlows.put(neighborVar, unprocessedFlowOnNeighbor);
					}
					int propagatedFlowSize = 0;
					for (int setID : unprocessedFlow) {
						if (neighborIntersect.contains(setID) && (processedFlowOnNeighbor == null || !processedFlowOnNeighbor.contains(setID))) {
							unprocessedFlowOnNeighbor.add(setID);
							propagatedFlowSize++;
						}
					}
					if (verbose) System.err.println("  propagated " + propagatedFlowSize + " units of unprocessed flow to " + varNames[neighborVar]);
					agenda.add(neighborVar);
				}
			}
			Set<Integer> processedFlow = processedFlows.get(candidate);
			if (processedFlow == null) {
				processedFlow = new TreeSet<Integer>();
				processedFlows.put(candidate, processedFlow);
			}
			processedFlow.addAll(unprocessedFlow);
			unprocessedFlows.put(candidate, new TreeSet<Integer>());
		}
		return flowArrived;
	}
	
	private boolean dfsSearchOnCurrentGraph(int xVar, int yVar, Set<Integer> zVars,  Map<Integer,Set<Integer>> cognateGraph)
	{
		Set<Integer> processed = new HashSet<Integer>();
		Queue<Integer> agenda = new LinkedList<Integer>();
		agenda.add(xVar);
		while (agenda.size() > 0)
		{
			int candidate = agenda.remove();
			if (processed.contains(candidate)) continue;
			if (candidate == yVar) return true;
			Set<Integer> neighbors = graph.getNeighbors(candidate);
			//System.err.println("  neighbors = " + neighbors);
			for (int neighborVar : neighbors)
			{
				if (candidate == xVar && neighborVar == yVar) continue;
				//System.err.println("    neighbor: " + neighborVar);
				if (cognateGraph.get(neighborVar) == null)
				{
					//System.err.println("      cognateGraph.get(" + neighborVar + ") == null");
					continue;
				}
				if (!cognateGraph.get(neighborVar).contains(candidate))
				{
					//System.err.println("      cognateGraph.get(" + neighborVar + ") == " + cognateGraph.get(neighborVar) + ", does not contain " + lastVar + "!");
					continue;
				}
				if (neighborVar == yVar || (zVars.contains(neighborVar) && !processed.contains(neighborVar)))
				{
					agenda.add(neighborVar);
				}
			}
			processed.add(candidate);
		}
		return false;
	}
	
	
	private List<Integer> dfsPathSearchOnCurrentGraph(int xVar, int yVar, Set<Integer> zVars)
	{
		List<Integer> currentDfsPath = new LinkedList<Integer>();
		currentDfsPath.add(xVar);
		return dfsPathSearch(currentDfsPath, yVar, zVars);
	}
	
	private List<Integer> dfsPathSearch(List<Integer> currentDfsPath, int goalVar, Set<Integer> allowedVars)
	{
		//System.err.println("dfsSearch(" + currentDfsPath + "," + goalVar + "," + allowedVars + ")");
		//System.err.println(currentDfsPath + " -> " + goalVar);
		int lastVar = currentDfsPath.get(currentDfsPath.size() - 1);
		Set<Integer> neighbors = graph.getNeighbors(lastVar);
		//System.err.println("  neighbors = " + neighbors);
		for (int neighborVar : neighbors)
		{
			if (!currentDfsPath.contains(neighborVar))
			{
				if (neighborVar == goalVar)
				{
					//System.err.println("      goal reached!");
					//need to exclude the direct connection which is supposed to be "explained away"
					if (currentDfsPath.size() > 1) return currentDfsPath;
				}
				else
				{
					if (allowedVars.contains(neighborVar))
					{
						//System.err.println("  moving to neighbor " + neighborVar);
						currentDfsPath.add(neighborVar);
						List<Integer> path = dfsPathSearch(currentDfsPath, goalVar, allowedVars);
						currentDfsPath.remove(currentDfsPath.size() - 1);
						if (path != null) return path;
					}
					else
					{
						//System.err.println("  neighbor " + neighborVar + " not allowed!");
					}
				}
			}
			else
			{
				//System.err.println("  path already contains " + neighborVar + ", skipping!");
			}
		}
		return null;
	}
	
	private boolean dfsPathSearchOnCurrentGraph(int xVar, int yVar, Set<Integer> zVars, Map<Integer,Set<Integer>> cognateGraph)
	{
		if (cognateGraph.get(xVar) == null || cognateGraph.get(yVar) == null) return false;
		List<Integer> currentDfsPath = new LinkedList<Integer>();
		currentDfsPath.add(xVar);
		return dfsPathSearch(currentDfsPath, yVar, zVars, cognateGraph);
	}
	
	private boolean dfsPathSearch(List<Integer> currentDfsPath, int goalVar, Set<Integer> allowedVars, Map<Integer,Set<Integer>> cognateGraph)
	{
		//System.err.println("dfsSearch(" + currentDfsPath + "," + goalVar + "," + allowedVars + ")");
		//System.err.println(currentDfsPath + " -> " + goalVar);
		int lastVar = currentDfsPath.get(currentDfsPath.size() - 1);
		Set<Integer> neighbors = graph.getNeighbors(lastVar);
		//System.err.println("  neighbors = " + neighbors);
		for (int neighborVar : neighbors)
		{
			//System.err.println("    neighbor: " + neighborVar);
			if (cognateGraph.get(neighborVar) == null)
			{
				//System.err.println("      cognateGraph.get(" + neighborVar + ") == null");
				continue;
			}
			if (!cognateGraph.get(neighborVar).contains(lastVar))
			{
				//System.err.println("      cognateGraph.get(" + neighborVar + ") == " + cognateGraph.get(neighborVar) + ", does not contain " + lastVar + "!");
				continue;
			}
			if (!currentDfsPath.contains(neighborVar))
			{
				if (neighborVar == goalVar)
				{
					//System.err.println("      goal reached!");
					//need to exclude the direct connection which is supposed to be "explained away"
					if (currentDfsPath.size() > 1) return true;
				}
				else
				{
					if (allowedVars.contains(neighborVar))
					{
						currentDfsPath.add(neighborVar);
						boolean result = dfsPathSearch(currentDfsPath, goalVar, allowedVars, cognateGraph);
						currentDfsPath.remove(currentDfsPath.size() - 1);
						if (result) return true;
					}
				}
			}
		}
		return false;
	}
	
	public double partialCorrelation(int xVar, int yVar, Set<Integer> zVars)
	{
		//long startTime = System.currentTimeMillis();
		
		String xVarName = varNames[xVar];
		String yVarName = varNames[yVar];
		
		if (VERBOSE) System.err.print("      Correlate flow for X=" + xVarName + " Y=" + yVarName + " Z=" + varSetToString(zVars) + ": ");
		
		Map<Integer,Set<Pair<String,String>>> cognatesPerConcept = null;
		if (storeOverlaps)
		{
			cognatesPerConcept = unexplainedOverlaps.get(varNames[xVar]).get(varNames[yVar]);
			cognatesPerConcept.clear();
		}
		boolean flowPossible = true;
		//shortcut: if no flow is possible given the current graph structure, there is no need to check every single cognate
		if  (!dfsSearchOnCurrentGraph(xVar, yVar, zVars))
		{
			//if (VERBOSE) System.err.println("      No correlate flow possible for X=" + xVarName + " Y=" + yVarName + " Z=" + varSetToString(zVars) + ", returning maximal partial correlation!");
			flowPossible = false;
		}
		
		double numXSets = setsForLanguagePair.get(xVar).get(xVar).size();
		double numYSets = setsForLanguagePair.get(yVar).get(yVar).size();
			
		double explainedCorrelates = 0.0;
		Set<Integer> flowCognateIDs = new TreeSet<Integer>();
		double unexplainedCorrelates = setsForLanguagePair.get(xVar).get(yVar).size();
		
		if (flowPossible) {
			flowCognateIDs = parallelDfsSearchOnCurrentGraph(xVar, yVar, zVars);
			unexplainedCorrelates -= flowCognateIDs.size();
			explainedCorrelates += flowCognateIDs.size();
		}
		if (storeOverlaps) {
			for (int cognateSetID : setsForLanguagePair.get(xVar).get(yVar)) {
				if (!flowCognateIDs.contains(cognateSetID)) {
					String xString = "?";
					String yString = "?";		
					for (Triple<String,String,String> triple : elementUnits.get(cognateSetID))
					{
						if (triple.first.equals(varNames[xVar]))
						{
							xString = triple.second + "#" + triple.third;
						}
						else if (triple.first.equals(varNames[yVar]))
						{
							yString = triple.second + "#" + triple.third;
						}
					}
					Set<Pair<String,String>> cognatePairSet = new HashSet<Pair<String,String>>();
					cognatePairSet.add(new Pair<String,String>(xString, yString));
					cognatesPerConcept.put(cognateSetID, cognatePairSet);
				}
			}
		}
		
		//double result = unexplainedCorrelates / Math.min(numXSets, numYSets);
		double result = unexplainedCorrelates / Math.max(numXSets, numYSets);
		//double result = unexplainedCorrelates / 1016;
		if (Math.min(numXSets, numYSets) == 0.0 || result == Double.NaN) result = 0.0;
		//if (VERBOSE) System.err.println((int) explainedCorrelates + " U " + (int) unexplainedCorrelates + "/min(" + (int) numXSets + "," + (int) numYSets + ") = " + result);
		if (VERBOSE) System.err.println((int) explainedCorrelates + " U " + (int) unexplainedCorrelates + "/max(" + (int) numXSets + "," + (int) numYSets + ") = " + result);
		//if (VERBOSE) System.err.println((int) explainedCorrelates + " U " + (int) unexplainedCorrelates + "/1016 = " + result);
		//long time = System.currentTimeMillis() - startTime;
		//System.err.println("Time for partial correlation test: " + time + " ms.");
		result = unexplainedCorrelates;
		return result;
	}

	@Override
	public boolean independenceTest(double partialCorrelation, int xVar, int yVar, Set<Integer> zVars) 
	{
		if (VERBOSE) System.err.println("      Condition for independence test: " + partialCorrelation + " <= " + thresholds[xVar][yVar] + " = thresholds[" + xVar + "][" + yVar + "]");
		boolean result = (partialCorrelation <= thresholds[xVar][yVar]);
		return result;
	}
	
	public boolean vStructureTest(int aIndex, int bIndex, int cIndex)
	{
		List<Set<Triple<String,String,String>>> bCognateSets = setsForLanguage.get(varNames[bIndex]);
		Set<Integer> abCognateSets = setsForLanguagePair.get(aIndex).get(bIndex);
		Set<Integer> acCognateSets = setsForLanguagePair.get(aIndex).get(cIndex);
		Set<Integer> bcCognateSets = setsForLanguagePair.get(bIndex).get(cIndex);
		int numBCognateSets = 0;
		for (Set<Triple<String,String,String>> cogset : bCognateSets) {
			if (cogset.size() >= 2) {
				numBCognateSets += 1;
			}
		}
		int abIntersectionSize = 0;
		for (int setIndex : abCognateSets) {
			if (elementUnits.get(setIndex).size() >= 3)
				abIntersectionSize += 1;
		}
		int bcIntersectionSize = 0;
		for (int setIndex : bcCognateSets) {
			if (elementUnits.get(setIndex).size() >= 3)
				bcIntersectionSize += 1;
		}
		int abcIntersectionSize = SetOperations.getIntersection(abCognateSets, bcCognateSets).size();
		
		System.err.print("v-structure test for " + varNames[aIndex] + " -> " + varNames[bIndex] + " <- " + varNames[cIndex] + ") = ");
		System.err.print("chyper(" + abcIntersectionSize + ", " + abCognateSets.size() + ", " + (numBCognateSets - abCognateSets.size()) + ", " + bcCognateSets.size() + ") = ");
		
		double pValue = 0.2;
		//double pValue = DistLib.hypergeometric.cumulative(abcIntersectionSize, abCognateSets.size(), numBCognateSets - abCognateSets.size(), bcCognateSets.size());
		System.err.print(pValue);

		if (pValue < 0.10)
		{
			System.err.println(" => v-structure!");
			return true;
		}
		else
		{
			System.err.println(" => cannot reject null hypothesis");
			return false;
		}
	}
	
	public boolean implementsVStructureTest()
	{
		return true;
	}
	
	public void displayRelevantInformation()
	{
		if (VERBOSE)
		{
			if (storeOverlaps)
			{
				for (String lang1 : unexplainedOverlaps.keySet())
				{
					for (String lang2 : unexplainedOverlaps.get(lang1).keySet())
					{
						System.err.println(varNameToID.get(lang1));
						System.err.println(varNameToID.get(lang2));
						boolean lateralLink = !graph.hasPresetLink(varNameToID.get(lang1), varNameToID.get(lang2));
						//System.err.println(lateralLink + " preset link " + lang1  + " (" + varNameToID.get(lang1) + ") -- " + lang2  + " (" + varNameToID.get(lang2) + ")");
						boolean lang1ToLang2 = graph.hasArrow(varNameToID.get(lang1), varNameToID.get(lang2));
						boolean lang2ToLang1 = graph.hasArrow(varNameToID.get(lang2), varNameToID.get(lang1));
						System.err.println("Otherwise unexplained cognate pairs between " + lang1.toUpperCase() + " and " + lang2.toUpperCase());
						for (Integer cognateSetID : unexplainedOverlaps.get(lang1).get(lang2).keySet()) {
							for (Pair<String, String> pair : unexplainedOverlaps.get(lang1).get(lang2).get(cognateSetID)) {
								if (lateralLink && lang1ToLang2 && !lang2ToLang1) {	
									System.err.println("LOAN" + "\t" + cognateSetID + "\t" + lang1 + "\t" + lang2);
								} else if (lateralLink && !lang1ToLang2 && lang2ToLang1) {	
									System.err.println("LOAN" + "\t" + cognateSetID + "\t" + lang2 + "\t" + lang1);
								}
								System.err.println(cognateSetID + "\t" + pair.first + "\t" + pair.second);
							}
						}
					}
				}
			}
		}
	}
	
	public void registerSepSets(int xVar, int yVar, List<Set<Integer>> sepSet)
	{
		if (storeOverlaps) unexplainedOverlaps.get(varNames[xVar]).get(varNames[yVar]).clear();
	}
	
	public boolean maxCondSetSizeReached(int xVar, int yVar, int condSetSize) {
		int maxCondSetSize = maxCognateSetSizeForPair.get(varNames[xVar]).get(varNames[yVar]);
		//System.err.println("maxCondSetSizeReached(" + varNames[xVar] + "," + varNames[yVar] + "): " + condSetSize + "/" + maxCondSetSize);
		return (condSetSize >= maxCondSetSize); 
	}
}
