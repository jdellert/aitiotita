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
    		System.err.println("arrow count " + varNames[var1] + " " + arrowSymbolToString(arrowSymbol) + " " + varNames[var2] + " : " + arrowCount);
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
	
	public void outputToDotFormat(PrintStream out, double strengtheningFactor) 
	{
		out.println("digraph LanguageGraph");
		out.println("{");
		out.println("  splines=true;");
		out.println("  node [ fontname=Arial, fontcolor=blue, fontsize=20];");
		
		for (String varName : nameToVar.keySet())
		{
			out.println("  " + varName + ";");
		}
		
		List<String> undirectedSubgraphLink = new LinkedList<String>();
        List<String> directedSubgraphLink = new LinkedList<String>();
        List<String> directedPresetLink = new LinkedList<String>();       
        List<String> bidirectedSubgraphLink = new LinkedList<String>();
        for (Pair<Integer,Integer> link : listAllLinks())
        {
        	String lang1 = varNames[link.first];
        	String lang2 = varNames[link.second];
        	
        	System.err.print("corrMatrix[" + lang1 + "][" + lang2 + "] = ");
        	double correlationStrength = (double) neighborCounts.get(link.first).get(link.second) / numGraphs;
        	
        	System.err.println(correlationStrength);
        	
        	if (correlationStrength == 0.0) continue;
        	int lineWidth = (int) (correlationStrength / 0.05);
        	//if (correlationStrength > 0.0025) lineWidth++;       	
        	//if (correlationStrength > 0.005) lineWidth++;
        	//if (correlationStrength > 0.01) lineWidth++; 
        	//if (correlationStrength > 0.02) lineWidth++;   
        	//if (correlationStrength > 0.04) lineWidth++;   
        	//if (correlationStrength > 0.08) lineWidth++;   
        	//if (correlationStrength > 0.16) lineWidth++; 
        	//if (correlationStrength > 0.32) lineWidth++;
        	//if (correlationStrength > 0.64) lineWidth++;     	
        	correlationStrength *= strengtheningFactor; //25 is good for basic correlation, 100 for remnants after causal inference on cognates, 5 on correlations only
        	if (correlationStrength > 1.0) correlationStrength = 1.0;
        	String alphaValue = Integer.toHexString((int) (correlationStrength * 255));
        	if (alphaValue.length() == 1) alphaValue = "0" + alphaValue; //add 0 in front of single-digit numbers
        	
        	if (lineWidth < 2) continue;
        	
        	int arrowAndArrowCount = getArrowCount(link.first, link.second, ARROW_ARROW);
        	if (arrowAndArrowCount > 0) {
        		lineWidth = (int) ((arrowAndArrowCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			bidirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + getLinkColor(link.first, link.second, ARROW_ARROW) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int lineAndArrowCount = getArrowCount(link.first, link.second, LINE_ARROW);
        	if (lineAndArrowCount > 0) {
        		lineWidth = (int) ((lineAndArrowCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + getLinkColor(link.first, link.second, LINE_ARROW) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int circleAndArrowCount = getArrowCount(link.first, link.second, CIRCLE_ARROW);
        	if (circleAndArrowCount > 0) {
        		lineWidth = (int) ((circleAndArrowCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + getLinkColor(link.first, link.second, CIRCLE_ARROW) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int arrowAndLineCount = getArrowCount(link.first, link.second, ARROW_LINE);
        	if (arrowAndLineCount > 0) {
        		lineWidth = (int) ((arrowAndLineCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + getLinkColor(link.first, link.second, ARROW_LINE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int circleAndLineCount = getArrowCount(link.first, link.second, CIRCLE_LINE);
        	if (circleAndLineCount > 0) {
        		lineWidth = (int) ((circleAndLineCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + getLinkColor(link.first, link.second, CIRCLE_LINE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int arrowAndCircleCount = getArrowCount(link.first, link.second, ARROW_CIRCLE);
        	if (arrowAndCircleCount > 0) {
        		lineWidth = (int) ((arrowAndCircleCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + getLinkColor(link.first, link.second, ARROW_CIRCLE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int lineAndCircleCount = getArrowCount(link.first, link.second, LINE_CIRCLE);
        	if (lineAndCircleCount > 0) {
        		lineWidth = (int) ((lineAndCircleCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + getLinkColor(link.first, link.second, LINE_CIRCLE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int circleAndCircleCount = getArrowCount(link.first, link.second, CIRCLE_CIRCLE);
        	if (circleAndCircleCount > 0) {
        		lineWidth = (int) ((circleAndCircleCount / (correlationStrength * numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			undirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + getLinkColor(link.first, link.second, CIRCLE_CIRCLE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        }
        out.println("subgraph undirected");
        out.println("{");
        out.println("  edge [dir=none];");
        for (String link : undirectedSubgraphLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
        out.println("subgraph bidirected");
        out.println("{");
        out.println("  edge [dir=none];");
        for (String link : bidirectedSubgraphLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
        out.println("subgraph directed");
        out.println("{");
        out.println("  edge [arrowsize=1];");
        for (String link : directedSubgraphLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
        out.println("subgraph directedPreset");
        out.println("{");
        out.println("  edge [arrowsize=1];");
        for (String link : directedPresetLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
		
		out.println("}");
		
		out.close();
	}
}
