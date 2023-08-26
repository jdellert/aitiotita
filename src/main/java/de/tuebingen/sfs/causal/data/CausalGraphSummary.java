package de.tuebingen.sfs.causal.data;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.util.struct.Pair;

public class CausalGraphSummary {
	int numGraphs;
	
	public String[] varNames;
	public Map<String, Integer> nameToVar;
	
	//skeleton
	Map<Integer,Set<Integer>> neighbors;
	Map<Integer,Map<Integer,Integer>> neighborCounts;
	
	//end symbols with value constants (startVar -> (endVar -> (symbol as in CausalGraph -> count)))
	Map<Integer,Map<Integer,Map<Integer,Integer>>> arrowSymbolCounts;
	public static final int CIRCLE_CIRCLE = 0; //this is the default symbol in a PAG (joker, non-commitment)
	public static final int CIRCLE_LINE = 1;
	public static final int CIRCLE_ARROW = 2;
	public static final int LINE_CIRCLE = 3;
	public static final int ARROW_CIRCLE = 4;
	public static final int LINE_ARROW = 5;
	public static final int ARROW_LINE = 6;
	public static final int ARROW_ARROW = 7;
	public static final int LINE_LINE = 8;
	
	//optional display: coordinates and colors
	Map<Integer, Map<Integer, Map<Integer, String>>> colors;
	
	public CausalGraphSummary(String[] varNames) {
		this.numGraphs = 0;
		this.varNames = varNames;
		nameToVar = new TreeMap<String, Integer>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			nameToVar.put(varNames[varIndex], varIndex);
		}
		neighbors = new TreeMap<Integer,Set<Integer>>();
		neighborCounts = new TreeMap<Integer,Map<Integer,Integer>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			neighbors.put(varIndex, new TreeSet<Integer>());
			neighborCounts.put(varIndex, new TreeMap<Integer,Integer>());
		}
		arrowSymbolCounts = new TreeMap<Integer,Map<Integer, Map<Integer, Integer>>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			arrowSymbolCounts.put(varIndex, new TreeMap<Integer, Map<Integer, Integer>>());
		}
		
		colors = new TreeMap<Integer,Map<Integer, Map<Integer,String>>>();
		for (int varIndex = 0; varIndex < varNames.length; varIndex++)
		{
			colors.put(varIndex, new TreeMap<Integer, Map<Integer, String>>());
		}
	}
	
	private static int endSymbolsToArrowSymbol(int endSymbol1, int endSymbol2) {
		if (endSymbol1 == 0) {
			if (endSymbol2 == 0) return CIRCLE_CIRCLE;
			if (endSymbol2 == 1) return LINE_CIRCLE;
			if (endSymbol2 == 2) return ARROW_CIRCLE;
		}
		if (endSymbol1 == 1) {
			if (endSymbol2 == 0) return CIRCLE_LINE;
			if (endSymbol2 == 1) return LINE_LINE;
			if (endSymbol2 == 2) return ARROW_LINE;
		}
		if (endSymbol1 == 2) {
			if (endSymbol2 == 0) return CIRCLE_ARROW;
			if (endSymbol2 == 1) return LINE_ARROW;
			if (endSymbol2 == 2) return ARROW_ARROW;
		}
		System.err.println("ERROR: cannot convert end symbol combination (" + endSymbol1 + "," + endSymbol2 + ")");
		return -1;
	}
	
	private static String arrowSymbolToString(int arrowSymbol) {
		switch (arrowSymbol) {
			case CIRCLE_CIRCLE: return "o-o";
			case CIRCLE_LINE: return "o--";
			case CIRCLE_ARROW: return "o->";
			case LINE_CIRCLE: return "--o";
			case ARROW_CIRCLE: return "<-o";
			case LINE_ARROW: return "-->";
			case ARROW_LINE: return "<--";
			case ARROW_ARROW: return "<->";
			case LINE_LINE: return "---";	
			default: return "?-?";
		}	
	}
	
	private void increaseNeighborCount(int var1, int var2) {
		Map<Integer,Integer> countsForVar1 = neighborCounts.get(var1);
		Integer previousCount = countsForVar1.get(var2);
		if (previousCount == null) {
			previousCount = 0;
		}
		countsForVar1.put(var2, previousCount + 1);
	}
	
	private void increaseArrowSymbolCount(int var1, int var2, int symbol) {
		Map<Integer,Integer> arrowSymbolCountsForPair = arrowSymbolCounts.get(var1).get(var2);
		if (arrowSymbolCountsForPair == null ) {
			arrowSymbolCountsForPair = new TreeMap<Integer,Integer>();
			arrowSymbolCounts.get(var1).put(var2, arrowSymbolCountsForPair);
		}
		Integer previousCount = arrowSymbolCountsForPair.get(symbol);
		if (previousCount == null) {
			previousCount = 0;
		}
		arrowSymbolCountsForPair.put(symbol, previousCount + 1);
	}
	
	public void addGraph(CausalGraph graph) {
		
		for (int var1 : graph.neighbors.keySet()) {
			for (int var2 : graph.neighbors.get(var1)) {
				this.neighbors.get(var1).add(var2);
				increaseNeighborCount(var1, var2);
				if (var1 > var2) continue;
				int endSymbol1 = graph.endSymbol.get(var1).get(var2);
				int endSymbol2 = graph.endSymbol.get(var2).get(var1);
				increaseArrowSymbolCount(var1, var2, endSymbolsToArrowSymbol(endSymbol1, endSymbol2));
			}
		}
		
		this.numGraphs++;
	}
	
	public boolean hasLink(int var1, int var2)
	{
		return neighbors.get(var1).contains(var2);
	}
	
	public int getArrowCount(int var1, int var2, int arrowSymbol) {
    	Map<Integer,Integer> arrowSymbolCountsForPair = arrowSymbolCounts.get(var1).get(var2);
    	Integer arrowCount = arrowSymbolCountsForPair.get(arrowSymbol);
    	if (arrowCount != null) {
    		return arrowCount;
    	}
    	return 0;
	}
	
	/**
	 * Retrieves the color stored for a link, or returns a default color based on the arrow configuration.
	 * @param var1
	 * @param var2
	 * @param arrowSymbol
	 * @return color 
	 */
	public String getLinkColor(int var1, int var2, int arrowSymbol)
	{
		Map<Integer, String> colorPerSymbol = colors.get(var1).get(var2);
		if (colorPerSymbol == null || colorPerSymbol.get(arrowSymbol) == null)
		{
			switch (arrowSymbol) {
				case ARROW_ARROW: return "#80d100";
				case LINE_ARROW: return "#007e56"; //dark green
				case ARROW_LINE: return "#007e56"; //dark green
				case CIRCLE_ARROW: return "#80d100"; //light green
				case ARROW_CIRCLE: return "#80d100"; //light green
				case CIRCLE_LINE: return "#cccc00"; //yellow
				case LINE_CIRCLE: return "#cccc00"; //yellow
				case CIRCLE_CIRCLE: return "#cccc00"; //yellow	
				case LINE_LINE: return "#cc0000"; //red
			}
		} 
		return colorPerSymbol.get(arrowSymbol);
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
	
	public void printInTextFormat()
	{
		CausalGraphOutput.outputToTextFormat(this, System.out);
	}
	
}
