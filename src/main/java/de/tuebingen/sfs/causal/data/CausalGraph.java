package de.tuebingen.sfs.causal.data;

import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.util.struct.Pair;

public class CausalGraph 
{
	public String[] varNames;
	public Map<String, Integer> nameToVar;
	
	//skeleton
	Map<Integer,Set<Integer>> neighbors;
	
	//end symbols with value constants
	Map<Integer,Map<Integer,Integer>> endSymbol;
	public static final int CIRCLE_END = 0; //this is the default symbol in a PAG (joker, non-commitment)
	public static final int LINE_END = 1;
	public static final int ARROW_END = 2;
	
	//non-removable links (no conditional independence tests for these remain necessary)
	Map<Integer,Map<Integer,Boolean>> hasUndeletableLink;
	
	//predetermined link and arrows
	Map<Integer,Map<Integer,Boolean>> hasPresetLink;
	Map<Integer,Map<Integer,Boolean>> hasPresetEndSymbol;	
	
	//optional display: coordinates and colors
	Map<Integer, Map<Integer, String>> colors;
	Map<Integer, Point2D.Double> coordinates;

	public CausalGraph(String[] varNames, boolean completeGraph)
	{
		this.varNames = varNames;
		nameToVar = new TreeMap<String, Integer>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			nameToVar.put(varNames[varIndex], varIndex);
		}
		neighbors = new TreeMap<Integer,Set<Integer>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			neighbors.put(varIndex, new TreeSet<Integer>());
		}
		endSymbol = new TreeMap<Integer,Map<Integer, Integer>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			endSymbol.put(varIndex, new TreeMap<Integer, Integer>());
		}
		
		hasUndeletableLink = new TreeMap<Integer,Map<Integer, Boolean>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			hasUndeletableLink.put(varIndex, new TreeMap<Integer, Boolean>());
		}
		hasPresetLink = new TreeMap<Integer,Map<Integer, Boolean>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			hasPresetLink.put(varIndex, new TreeMap<Integer, Boolean>());
		}
		hasPresetEndSymbol = new TreeMap<Integer,Map<Integer, Boolean>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			hasPresetEndSymbol.put(varIndex, new TreeMap<Integer, Boolean>());
		}
		
		colors = new TreeMap<Integer,Map<Integer, String>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			colors.put(varIndex, new TreeMap<Integer, String>());
		}
		
		coordinates = new TreeMap<Integer, Point2D.Double>();
		
		for (int var1 = 0; var1 < varNames.length; var1++)
		{
			for (int var2 = 0; var2 < varNames.length; var2++)
			{
				if (var1 != var2)
				{
					putPresetLink(var1, var2, false);
					markEndAsPreset(var1, var2, false);	
					setUndeletableLink(var1, var2, false);
				}
			}
		}
		
		if (completeGraph)
		{
			for (int var1 = 0; var1 < varNames.length; var1++)
			{
				for (int var2 = 0; var2 < varNames.length; var2++)
				{
					if (var1 != var2)
					{
						addLink(var1, var2);
						putArrow(var1, var2, false);
					}
				}
			}
		}
		
		
	}
	
	public boolean hasLink(int var1, int var2)
	{
		return neighbors.get(var1).contains(var2);
	}
	
	public void addLink(int var1, int var2)
	{
		neighbors.get(var1).add(var2);
		neighbors.get(var2).add(var1);
	}
	
	public void removeLink(int var1, int var2)
	{
		if (hasPresetLink(var1,var2))
		{
			System.err.println("WARNING: removing preset link (this should not happen!)");
		}
		neighbors.get(var1).remove(var2);
		neighbors.get(var2).remove(var1);
	}
	
	public int getEndSymbol(int var1, int var2)
	{
		Integer symbol = endSymbol.get(var1).get(var2);
		if (symbol == null) return CIRCLE_END;
		return symbol;
	}
	
	public void setEndSymbol(int var1, int var2, int symbol)
	{
		if (hasPresetEnd(var1,var2) || hasPresetEnd(var2,var1))
		{
			//System.err.println("WARNING: touching preset end symbol (this should not happen!)");
		}
		endSymbol.get(var1).put(var2, symbol);
	}
	
	public boolean hasArrow(int var1, int var2)
	{
		Integer symbol = endSymbol.get(var1).get(var2);
		if (symbol == null) return false;
		if (symbol == ARROW_END) return true;
		return false;
	}
	
	public void putArrow(int var1, int var2, boolean arrow)
	{
		if (hasPresetArrow(var1,var2) || hasPresetArrow(var2,var1))
		{
			System.err.println("WARNING: touching preset arrow (this should not happen!)");
		}
		if (arrow == true)
		{
			endSymbol.get(var1).put(var2, ARROW_END);
			if (getEndSymbol(var2, var1) == CIRCLE_END)
			{
				endSymbol.get(var2).put(var1, LINE_END);
			}
		}
		else
		{
			endSymbol.get(var1).put(var2, CIRCLE_END);
		}
	}
	
	public void setLinkColor(int var1, int var2, String colorString)
	{
		colors.get(var1).put(var2, colorString);
		colors.get(var2).put(var1, colorString);
	}
	
	/**
	 * Retrieves the color stored for a link, or returns a default color based on the arrow configuration.
	 * @param var1
	 * @param var2
	 * @return color 
	 */
	public String getLinkColor(int var1, int var2)
	{
		String color = colors.get(var1).get(var2);
		if (color == null)
		{
			if (getEndSymbol(var1, var2) == ARROW_END)
        	{
        		if (getEndSymbol(var2, var1) == ARROW_END)
        		{
        			color = "#80d100";
        		}
        		else if (getEndSymbol(var2, var1) == LINE_END)
        		{
        			if (hasPresetArrow(var1, var2) || hasPresetLink(var1, var2))
        			{
        				color = "#000000";
        			}
        			else
        			{
        				color = "#007e56";
        			}
        		}
        		else if (getEndSymbol(var2, var1) == CIRCLE_END)
        		{
        			color = "#80d100";
        		}
        	}
        	else if (getEndSymbol(var1, var2) == LINE_END)
        	{
        		if (getEndSymbol(var2, var1) == ARROW_END)
        		{
        			if (hasPresetArrow(var2, var1) || hasPresetLink(var1, var2))
        			{
        				color = "#000000";
        			}
        			else
        			{
        				color = "#007e56";
        			}
        		}
        		else if (getEndSymbol(var2, var1) == LINE_END)
        		{
        			color = "#cc0000";
        		}
        		else if (getEndSymbol(var2, var1) == CIRCLE_END)
        		{
        			color = "#cccc00";
        		}
        	}
        	else if (getEndSymbol(var1, var2) == CIRCLE_END)
        	{
        		if (getEndSymbol(var2, var1) == ARROW_END)
        		{
        			if (hasPresetArrow(var2, var1) || hasPresetLink(var1, var2))
        			{
        				color = "#000000";
        			}
        			else
        			{
        				color = "#80d100";
        			}
        		}
        		else if (getEndSymbol(var2, var1) == LINE_END)
        		{
        			color = "#cccc00";
        		}
        		else if (getEndSymbol(var2, var1) == CIRCLE_END)
        		{
    				color = "#cccc00";
        		}
        	}
		}
		return color;
	}
	
	public boolean hasUndeletableLink(int var1, int var2)
	{
		if (var1 == var2) return false;
		return (hasUndeletableLink.get(var1).get(var2) || hasUndeletableLink.get(var2).get(var1));	
	}
	
	public void setUndeletableLink(int var1, int var2, boolean link)
	{
		hasUndeletableLink.get(var1).put(var2, link);
		hasUndeletableLink.get(var2).put(var1, link);		
	}
	
	public boolean hasPresetLink(int var1, int var2)
	{
		if (var1 == var2) return false;
		return (hasPresetLink.get(var1).get(var2) || hasPresetLink.get(var2).get(var1));	
	}
	
	public void putPresetLink(int var1, int var2, boolean link)
	{
		hasPresetLink.get(var1).put(var2, link);
		hasPresetLink.get(var2).put(var1, link);		
	}
	
	public boolean hasPresetEnd(int var1, int var2)
	{
		return hasPresetEndSymbol.get(var1).get(var2);	
	}
	
	public void markEndAsPreset(int var1, int var2, boolean presetValue)
	{
		hasPresetEndSymbol.get(var1).put(var2, presetValue);	
	}
	
	public boolean hasPresetArrow(int var1, int var2)
	{
		return hasPresetEnd(var1, var2) && hasArrow(var1, var2);	
	}
	
	public List<Pair<Integer,Integer>> listAllLinks()
	{
		List<Pair<Integer,Integer>> pairs = new LinkedList<Pair<Integer,Integer>>();
		for (int var1 = 0; var1 < varNames.length; var1++)
		{
			for (int var2 = var1 + 1; var2 < varNames.length; var2++)
			{
				if (hasLink(var1, var2))
				{
					pairs.add(new Pair<Integer,Integer>(var1,var2));
				}
			}
		}
		return pairs;
	}
	
	public List<Pair<Integer,Integer>> listAllDeletableLinks()
	{
		List<Pair<Integer,Integer>> pairs = new LinkedList<Pair<Integer,Integer>>();
		for (int var1 = 0; var1 < varNames.length; var1++)
		{
			for (int var2 = var1 + 1; var2 < varNames.length; var2++)
			{
				if (hasLink(var1, var2) && !hasUndeletableLink(var1, var2))
				{
					pairs.add(new Pair<Integer,Integer>(var1,var2));
				}
			}
		}
		return pairs;
	}
	
	public List<Pair<Integer,Integer>> listAllLinksInBothDirections()
	{
		List<Pair<Integer,Integer>> pairs = new LinkedList<Pair<Integer,Integer>>();
		for (int var1 = 0; var1 < varNames.length; var1++)
		{
			for (int var2 = var1 + 1; var2 < varNames.length; var2++)
			{
				if (hasLink(var1, var2))
				{
					pairs.add(new Pair<Integer,Integer>(var1,var2));
					pairs.add(new Pair<Integer,Integer>(var2,var1));
				}
			}
		}
		return pairs;
	}
	
	public List<Integer[]> listUnshieldedTriples()
	{
		List<Integer[]> unshieldedTriples = new LinkedList<Integer[]>();
		for (int cause1Index = 0; cause1Index < varNames.length; cause1Index++)
		{
			for (int cause2Index = cause1Index + 1; cause2Index < varNames.length; cause2Index++)
			{
				for (int dependentIndex = 0; dependentIndex < varNames.length; dependentIndex++)
				{
					if (dependentIndex == cause1Index || dependentIndex == cause2Index) continue;
					if (!hasLink(cause1Index, cause2Index) && hasLink(cause1Index,dependentIndex) && hasLink(cause2Index,dependentIndex))
					{
						if (!hasPresetArrow(dependentIndex, cause1Index) && !hasPresetArrow(dependentIndex, cause2Index))
						{
							unshieldedTriples.add(new Integer[] {cause1Index, cause2Index, dependentIndex});
						}
					}
				}
			}
		}
		return unshieldedTriples;
	}
	
	public Set<Integer> getNeighbors(int var)
	{
		return neighbors.get(var);
	}
	
	public Set<Integer> getParents(int var)
	{
		Set<Integer> parents = new TreeSet<Integer>();
		for (int neighbor : getNeighbors(var))
		{
			if (getEndSymbol(var, neighbor) == LINE_END && getEndSymbol(neighbor, var) == ARROW_END)
			{
				parents.add(neighbor);
			}
		}
		return parents;
	}
	
	public Set<Integer> getAncestors(int var)
	{
		TreeSet<Integer> ancestors = new TreeSet<Integer>();
		List<Integer> agenda = new LinkedList<Integer>();
		agenda.add(var);
		while (agenda.size() > 0)
		{
			int k = agenda.remove(0);
			for (int neighbor : getNeighbors(k))
			{
				if (getEndSymbol(neighbor, k) == ARROW_END || (getEndSymbol(k, neighbor) == LINE_END && getEndSymbol(neighbor, k) == LINE_END))
				{
					if (ancestors.add(neighbor)) agenda.add(neighbor);
				}
			}
		}
		return ancestors;
	}
	
	public List<Integer> getNeighbors(Integer var1, Integer var2) 
	{
		List<Integer> result = new ArrayList<Integer>();
		Set<Integer> neighbors1 = neighbors.get(var1);
		Set<Integer> neighbors2 = neighbors.get(var2);
		for (int neighbor : neighbors1)
		{
			if (neighbor != var2) result.add(neighbor);
		}
		for (int neighbor : neighbors2)
		{
			if (neighbor != var1 && !neighbors1.contains(neighbor)) result.add(neighbor);
		}
		return result;
	}
	
	/**
	 * Uses breadth-first search from both sides to find all neighbors on acyclic paths between var1 and var2
	 * @param var1
	 * @param var2
	 * @return
	 */
	public Set<Integer> getNeighborsOnAcyclicPathsBetween(Integer var1, Integer var2, int maxDepth)
	{
		//System.err.println("getNeighborsOnAcyclicPathsBetween(" + var1 + "," + var2 + ") with maxDepth=" + maxDepth);
		Set<Integer> foundNeighbors = new TreeSet<Integer>();
		
		for (int neighbor : getNeighbors(var1))
		{
			if (neighbor == var2) continue;
			if (bfsSearch(var1, neighbor, var2, maxDepth)) 
			{
				foundNeighbors.add(neighbor);
			}
		}

		for (int neighbor : getNeighbors(var2))
		{
			if (neighbor == var1) continue;
			if (bfsSearch(var2, neighbor, var1, maxDepth)) 
			{
				foundNeighbors.add(neighbor);
			}
		}
		//System.err.println("= " + foundNeighbors);
		return foundNeighbors;
	}
	
	private boolean bfsSearch(int startVar, int startVarNeighbor, int goalVar, int maxDepth)
	{
		//System.err.println("  bfsSearch(" + startVar + "," + startVarNeighbor + "," + goalVar + ") with maxDepth=" + maxDepth);
		//also store depth
		List<Pair<Integer,Integer>> agenda = new LinkedList<Pair<Integer,Integer>>();
		agenda.add(new Pair<Integer,Integer>(startVarNeighbor,1));

		Set<Integer> processed = new TreeSet<Integer>();
		processed.add(startVar);
		
		while (agenda.size() > 0)
		{
			Pair<Integer,Integer> nextIDandDepth = agenda.remove(0);
			int nextID = nextIDandDepth.first;
			int depth = nextIDandDepth.second;
			Set<Integer> neighbors = getNeighbors(nextID);
			for (int neighborVar : neighbors)
			{
				if (!processed.contains(neighborVar))
				{
					if (neighborVar == goalVar) return true;
					if (depth == maxDepth) continue;
					else
					{
						agenda.add(new Pair<Integer,Integer>(neighborVar, depth + 1));
					}
				}
			}
			processed.add(nextID);
			
			//System.err.println("    agenda = " + agenda);
			//System.err.println("    processed = " + processed);
		}
		return false;
	}
	
	/**
	 * Uses breadth-first search to find all shortest discriminating paths <i, ...,l, j, k> for triple <l, j, k>
	 * @param l
	 * @param b
	 * @param p
	 * @return
	 */
	public List<List<Integer>> getShortestDiscriminatingPaths(Integer l, Integer j, Integer k)
	{
		List<List<Integer>> paths = new LinkedList<List<Integer>>();
		
		//a discriminating path must consist completely of colliders which are all parents of k!
		Set<Integer> candidateNodes = getParents(k);
		if (!candidateNodes.remove(l)) return paths;
			
		List<Integer> currentBfsPath = new LinkedList<Integer>();
		currentBfsPath.add(l);
		currentBfsPath.add(j);
		currentBfsPath.add(k);
		
		paths.add(currentBfsPath);
		
		paths = discriminatingPathBfs(paths, candidateNodes, k);
			
		return paths;
	}
	
	private List<List<Integer>> discriminatingPathBfs(List<List<Integer>> currentBfsPaths, Set<Integer> candidateNodes, int endPoint)
	{
		List<List<Integer>> nextIterationPaths = new LinkedList<List<Integer>>();
		List<List<Integer>> completePaths = new LinkedList<List<Integer>>();
		
		for (List<Integer> currentBfsPath : currentBfsPaths)
		{
			int head = currentBfsPath.get(0);
			for (int neighbor : neighbors.get(head))
			{
				if (currentBfsPath.contains(neighbor)) continue;
				if (getEndSymbol(neighbor, head) == ARROW_END)
				{
					if (!candidateNodes.contains(neighbor) && !hasLink(neighbor, endPoint))
					{
						//found starting point i which is not connected to k!
						List<Integer> completePath = new LinkedList<Integer>(currentBfsPath);
						completePath.add(0, neighbor);
						completePaths.add(completePath);
					}
					else if (candidateNodes.contains(neighbor) && getEndSymbol(head, neighbor) == ARROW_END)
					{
						List<Integer> extendedPath = new LinkedList<Integer>(currentBfsPath);
						extendedPath.add(0, neighbor);
						nextIterationPaths.add(extendedPath);
					}
				}
			}
		}
		
		if (completePaths.size() > 0)
		{
			return completePaths;
		}
		else
		{
			if (nextIterationPaths.size() == 0)
			{
				return nextIterationPaths;
			}
			return discriminatingPathBfs(nextIterationPaths, candidateNodes, endPoint);
		}
	}
	
	
	
	/**
	 * Uses depth-first search to find all neighbors on acyclic paths between var1 and var2 shorter than maxDepth
	 * @param var1
	 * @param var2
	 * @param maxDepth
	 * @return
	 */
	public Set<Integer> getNodesOnAcyclicPathsBetween(Integer var1, Integer var2, int maxDepth) 
	{
		Set<Integer> nodesFromWhichGoalCanBeReached = new TreeSet<Integer>();
		List<Integer> currentDfsPath = new LinkedList<Integer>();
		currentDfsPath.add(var1);
		
		dfsStep(currentDfsPath, var2, maxDepth, nodesFromWhichGoalCanBeReached);
		
		nodesFromWhichGoalCanBeReached.remove(var1);
		
		return nodesFromWhichGoalCanBeReached;
	}
	
	//DFS code: horribly inefficient for the connecting neighbors task!
	private void dfsStep(List<Integer> currentDfsPath, int goalVar, int maxDepth, Set<Integer> nodesFromWhichGoalCanBeReached)
	{
		//System.err.println(currentDfsPath + " -> " + goalVar);
		int lastVar = currentDfsPath.get(currentDfsPath.size() - 1);
		Set<Integer> neighbors = getNeighbors(lastVar);
		for (int neighborVar : neighbors)
		{
			if (!currentDfsPath.contains(neighborVar))
			{
				if (neighborVar == goalVar)
				{
					nodesFromWhichGoalCanBeReached.addAll(currentDfsPath);
				}
				else
				{
					if (currentDfsPath.size() < maxDepth)
					{
						currentDfsPath.add(neighborVar);
						dfsStep(currentDfsPath, goalVar, maxDepth, nodesFromWhichGoalCanBeReached);
						currentDfsPath.remove(currentDfsPath.size() - 1);
					}
				}
			}
		}
	}
	
	/**
	 * Uses depth-first search to find all acyclic paths between var1 and var2 shorter than maxDepth
	 * @param var1
	 * @param var2
	 * @param maxDepth
	 * @return
	 */
	public List<List<Integer>> getAcyclicPathsBetween(Integer var1, Integer var2, int maxDepth)
	{
		List<List<Integer>> paths = new LinkedList<List<Integer>>();
		List<Integer> currentDfsPath = new LinkedList<Integer>();
		currentDfsPath.add(var1);
		
		dfsStep(currentDfsPath, var2, maxDepth, paths);
			
		return paths;
	}
	
	private void dfsStep(List<Integer> currentDfsPath, int goalVar, int maxDepth, List<List<Integer>> paths)
	{
		//System.err.println(currentDfsPath + " -> " + goalVar);
		int lastVar = currentDfsPath.get(currentDfsPath.size() - 1);
		Set<Integer> neighbors = getNeighbors(lastVar);
		for (int neighborVar : neighbors)
		{
			if (!currentDfsPath.contains(neighborVar))
			{
				if (neighborVar == goalVar)
				{
					List<Integer> dfsPathCopy = new LinkedList<Integer>();
					dfsPathCopy.addAll(currentDfsPath);
					dfsPathCopy.remove(0);
					paths.add(dfsPathCopy);
				}
				else
				{
					if (currentDfsPath.size() < maxDepth)
					{
						currentDfsPath.add(neighborVar);
						dfsStep(currentDfsPath, goalVar, maxDepth, paths);
						currentDfsPath.remove(currentDfsPath.size() - 1);
					}
				}
			}
		}
	}
	
	/**
	 * Uses depth-first search to find all uncovered potentially directed paths between var1 and var2 shorter than maxDepth
	 * @param var1
	 * @param var2
	 * @param maxDepth
	 * @return
	 */
	public List<List<Integer>> getUncoveredPotentiallyDirectedPathsBetween(Integer var1, Integer var2, int maxDepth)
	{
		List<List<Integer>> paths = new LinkedList<List<Integer>>();
		List<Integer> currentDfsPath = new LinkedList<Integer>();
		currentDfsPath.add(var1);
		
		uncoveredPotentiallyDirectedStep(currentDfsPath, var2, maxDepth, paths);
			
		return paths;
	}
	
	private void uncoveredPotentiallyDirectedStep(List<Integer> currentDfsPath, int goalVar, int maxDepth, List<List<Integer>> paths)
	{
		//System.err.println(currentDfsPath + " -> " + goalVar);
		int lastVar = currentDfsPath.get(currentDfsPath.size() - 1);
		Set<Integer> neighbors = getNeighbors(lastVar);
		for (int neighborVar : neighbors)
		{
			if (!currentDfsPath.contains(neighborVar))
			{
				//check for cover using the last two vars (each triple on path must be unshielded!)
				if (currentDfsPath.size() > 1)
				{
					int previousToLastVar = currentDfsPath.get(currentDfsPath.size() - 2);
					if (hasLink(previousToLastVar, neighborVar)) continue;
				}
				
				//disallow <-* and *-- (otherwise, path would not be potentially directed
				if (getEndSymbol(neighborVar, lastVar) == ARROW_END || getEndSymbol(lastVar, neighborVar) == LINE_END) continue;
				
				if (neighborVar == goalVar)
				{
					List<Integer> dfsPathCopy = new LinkedList<Integer>();
					dfsPathCopy.addAll(currentDfsPath);
					dfsPathCopy.remove(0);
					paths.add(dfsPathCopy);
				}
				else
				{
					if (currentDfsPath.size() < maxDepth)
					{
						currentDfsPath.add(neighborVar);
						uncoveredPotentiallyDirectedStep(currentDfsPath, goalVar, maxDepth, paths);
						currentDfsPath.remove(currentDfsPath.size() - 1);
					}
				}
			}
		}
	}
	
	/**
	 * Uses depth-first search to find all uncovered cirlce paths between var1 and var2 shorter than maxDepth
	 * @param var1
	 * @param var2
	 * @param maxDepth
	 * @return
	 */
	public List<List<Integer>> getUncoveredCirclePathsBetween(Integer var1, Integer var2, int maxDepth)
	{
		List<List<Integer>> paths = new LinkedList<List<Integer>>();
		List<Integer> currentDfsPath = new LinkedList<Integer>();
		currentDfsPath.add(var1);
		
		uncoveredCircleStep(currentDfsPath, var2, maxDepth, paths);
			
		return paths;
	}
	
	private void uncoveredCircleStep(List<Integer> currentDfsPath, int goalVar, int maxDepth, List<List<Integer>> paths)
	{
		//System.err.println(currentDfsPath + " -> " + goalVar);
		int lastVar = currentDfsPath.get(currentDfsPath.size() - 1);
		Set<Integer> neighbors = getNeighbors(lastVar);
		for (int neighborVar : neighbors)
		{
			if (!currentDfsPath.contains(neighborVar))
			{
				//check for cover using the last two vars (each triple on path must be unshielded!)
				if (currentDfsPath.size() > 1)
				{
					int previousToLastVar = currentDfsPath.get(currentDfsPath.size() - 2);
					if (hasLink(previousToLastVar, neighborVar)) continue;
				}
				
				//only allow o-o links
				if (getEndSymbol(neighborVar, lastVar) != CIRCLE_END || getEndSymbol(lastVar, neighborVar) != CIRCLE_END) continue;
				
				if (neighborVar == goalVar)
				{
					List<Integer> dfsPathCopy = new LinkedList<Integer>();
					dfsPathCopy.addAll(currentDfsPath);
					dfsPathCopy.remove(0);
					paths.add(dfsPathCopy);
				}
				else
				{
					if (currentDfsPath.size() < maxDepth)
					{
						currentDfsPath.add(neighborVar);
						uncoveredPotentiallyDirectedStep(currentDfsPath, goalVar, maxDepth, paths);
						currentDfsPath.remove(currentDfsPath.size() - 1);
					}
				}
			}
		}
	}
	
	public Set<Integer> getConnectedVars(int startVar)
	{
		List<Integer> agenda = new LinkedList<Integer>();
		agenda.addAll(neighbors.get(startVar));
		Set<Integer> reachedNodes = new TreeSet<Integer>();
		reachedNodes.addAll(neighbors.get(startVar));
		while (agenda.size() > 0)
		{
			int var = agenda.remove(0);
			for (int neighbor : neighbors.get(var))
			{
				if (!reachedNodes.contains(neighbor))
				{
					reachedNodes.add(neighbor);
					agenda.add(neighbor);
				}
			}
		}
		return reachedNodes;
	}
	
	public Set<Integer> getConnectedVarsNotVia(int startVar, int forbiddenNeighbor)
	{
		List<Integer> agenda = new LinkedList<Integer>();
		agenda.addAll(neighbors.get(startVar));
		agenda.remove((Integer) forbiddenNeighbor);
		Set<Integer> reachedNodes = new TreeSet<Integer>();
		reachedNodes.addAll(neighbors.get(startVar));
		while (agenda.size() > 0)
		{
			int var = agenda.remove(0);
			for (int neighbor : neighbors.get(var))
			{
				if (var == startVar && neighbor == forbiddenNeighbor) continue;
				if (!reachedNodes.contains(neighbor))
				{
					reachedNodes.add(neighbor);
					agenda.add(neighbor);
				}
			}
		}
		return reachedNodes;
	}
	
	public String getLinkRepresentation(int var1, int var2)
	{
		if (!hasLink(var1, var2)) return "ooo";
		String linkRep = "";
		switch (getEndSymbol(var2, var1))
		{
			case CIRCLE_END:
				linkRep += "o";
				break;
			case LINE_END:
				linkRep += "-";
				break;
			case ARROW_END:
				linkRep += "<";
		}
		linkRep += "-";
		switch (getEndSymbol(var1, var2))
		{
			case CIRCLE_END:
				linkRep += "o";
				break;
			case LINE_END:
				linkRep += "-";
				break;
			case ARROW_END:
				linkRep += ">";
		}
		return linkRep;
	}
	
	public List<Integer> getSharedNeighbors(int var1, int var2)
	{
		List<Integer> sharedNeighbors = new ArrayList<Integer>();
		Set<Integer> neighbors1 = neighbors.get(var1);
		Set<Integer> neighbors2 = neighbors.get(var2);
		for (int neighbor : neighbors1)
		{
			if (neighbors2.contains(neighbor)) sharedNeighbors.add(neighbor);
		}
		return sharedNeighbors;
	}
	
	public void printInTextFormat()
	{
		CausalGraphOutput.outputToTextFormat(this, System.out);
	}
	

	
}
