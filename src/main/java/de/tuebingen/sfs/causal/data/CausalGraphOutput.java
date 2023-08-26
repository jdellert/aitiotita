package de.tuebingen.sfs.causal.data;

import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tuebingen.sfs.util.struct.Pair;

public class CausalGraphOutput {
	
	public static String pathToString(List<Integer> path, CausalGraph graph)
	{
		StringBuilder string = new StringBuilder(graph.varNames[path.get(0)]);
		for (int i = 1; i < path.size(); i++)
		{
			string.append(" " + graph.getLinkRepresentation(path.get(i-1), path.get(i)) + " " + graph.varNames[path.get(i)]);
		}
		return string.toString();
	}
	
	public static void outputToTextFormat(CausalGraph graph, PrintStream out)
	{
		out.println("#names");
		for (int i = 0; i < graph.varNames.length; i++)
		{
			out.println(i + "\t" + graph.varNames[i]);
		}
		out.println("#links");
		for (Pair<Integer,Integer> link : graph.listAllLinks())
	    {
			String var1 = graph.varNames[link.first];
			String var2 = graph.varNames[link.second];

			String linkRep = graph.getLinkRepresentation(link.first, link.second);

			out.println(var1 + "\t" + linkRep + "\t" + var2);
	    }
	}
	
	public static void outputToTextFormat(CausalGraphSummary graphSummary, PrintStream out)
	{
		out.println("#names");
		for (int i = 0; i < graphSummary.varNames.length; i++)
		{
			out.println(i + "\t" + graphSummary.varNames[i]);
		}
		out.println("#links");
		for (Pair<Integer,Integer> link : graphSummary.listAllLinks())
	    {
			
			String var1 = graphSummary.varNames[link.first];
			String var2 = graphSummary.varNames[link.second];

			double arrowArrowCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.ARROW_ARROW);
			if (arrowArrowCount > 0.0) out.println(arrowArrowCount / graphSummary.numGraphs + "\t" + var1 + "\t<->\t" + var2);

			double arrowCircleCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.ARROW_CIRCLE);
			if (arrowCircleCount > 0.0) out.println(arrowCircleCount / graphSummary.numGraphs + "\t" + var1 + "\t<-o\t" + var2);
			
			double arrowLineCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.ARROW_LINE);
			if (arrowLineCount > 0.0) out.println(arrowLineCount / graphSummary.numGraphs + "\t" + var1 + "\t<--\t" + var2);
			
			double circleArrowCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.CIRCLE_ARROW);
			if (circleArrowCount > 0.0) out.println(circleArrowCount / graphSummary.numGraphs + "\t" + var1 + "\to->\t" + var2);
			
			double circleCircleCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.CIRCLE_CIRCLE);
			if (circleCircleCount > 0.0) out.println(circleCircleCount / graphSummary.numGraphs + "\t" + var1 + "\to-o\t" + var2);
			
			double circleLineCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.CIRCLE_LINE);
			if (circleLineCount > 0.0) out.println(circleLineCount / graphSummary.numGraphs + "\t" + var1 + "\to--\t" + var2);
			
			double lineArrowCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.LINE_ARROW);
			if (lineArrowCount > 0.0) out.println(lineArrowCount / graphSummary.numGraphs + "\t" + var1 + "\t-->\t" + var2);
			
			double lineLineCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.LINE_LINE);
			if (lineLineCount > 0.0) out.println(lineLineCount / graphSummary.numGraphs + "\t" + var1 + "\t---\t" + var2);
	    }
	}
	
	public static void outputToGoldStandardFormat(CausalGraph graph, PrintStream out) 
	{
		for (int i = 0; i < graph.varNames.length; i++)
		{
			for (int j = 0; j < graph.varNames.length; j++)
			{
				if (i == j) continue;
				if (!graph.hasLink(i,j))
				{
					out.println(graph.varNames[i] + " ooo " + graph.varNames[j]);
				}
				else
				{
					if (graph.getEndSymbol(j,i) == CausalGraph.LINE_END)
					{
						if (graph.getEndSymbol(i,j) == CausalGraph.LINE_END)
						{
							out.println(graph.varNames[i] + " <-> " + graph.varNames[j]);
						}
						else if (graph.getEndSymbol(i,j) == CausalGraph.CIRCLE_END)
						{
							out.println(graph.varNames[i] + " o-> " + graph.varNames[j]);
						}
						else if (graph.getEndSymbol(i,j) == CausalGraph.ARROW_END)
						{
							out.println(graph.varNames[i] + " --> " + graph.varNames[j]);
						}
					}
					else if (graph.getEndSymbol(j,i) == CausalGraph.CIRCLE_END)
					{
						if (graph.getEndSymbol(i,j) == CausalGraph.LINE_END)
						{
							out.println(graph.varNames[i] + " <-o " + graph.varNames[j]);
						}
					}
					else if (graph.getEndSymbol(j,i) == CausalGraph.ARROW_END)
					{						
						if (graph.getEndSymbol(i,j) == CausalGraph.LINE_END)
						{
							out.println(graph.varNames[i] + " <-- " + graph.varNames[j]);
						}
					}
				}
			}
		}
		out.close();
	}
	
	public static void outputToDotFormat(CausalGraph graph, PrintStream out, Pair<Integer,Integer> markedEdge, Set<Integer> markedNodes) 
	{
		//about 3600 pixels wide and 800 pixels high (northern hemisphere only)
		//left bound at longitude -20 degrees (just west of iceland)
		double xOffset = -20.0;
		
		out.println("digraph CausalGraph");
		out.println("{");
		out.println("  splines=true;");
		out.println("  node [ fontname=Arial, fontcolor=blue, fontsize=30];");
		
		for (String varName : graph.nameToVar.keySet())
		{
				Point2D.Double position = graph.coordinates.get(outputVarName(varName));
				if (position == null)
				{
					out.print("  " + outputVarName(varName) + " ");
					if (markedNodes != null && markedNodes.contains(graph.nameToVar.get(varName)))
					{
						out.print("[fillcolor=\"#ff000080\", style=filled]");
					}
					else if (markedEdge != null && (markedEdge.first == graph.nameToVar.get(varName) || markedEdge.second == graph.nameToVar.get(varName)))
					{
						out.print("[fillcolor=\"#ff000080\", style=filled]");
					}
					out.println(";");
				}
				else
				{
					double xPos = position.getY() - xOffset;
					if (xPos <= 0) xPos += 360;
					double yPos = position.getX();
					out.print("  " + outputVarName(varName) + " [pos=\"" + (xPos * 50) + "," + (yPos * 50) + "\", width=\"0.1\", height=\"0.05\"");
					if (markedNodes != null && markedNodes.contains(graph.nameToVar.get(varName)))
					{
						out.print(", fillcolor=\"#00800080\", style=filled");
					}
					else if (markedEdge != null && (markedEdge.first == graph.nameToVar.get(varName) || markedEdge.second == graph.nameToVar.get(varName)))
					{
						out.print(", fillcolor=\"#ff000080\", style=filled");
					}
					out.println("];");
				}
		}
		
		List<String> undirectedSubgraphLink = new LinkedList<String>();
        List<String> directedSubgraphLink = new LinkedList<String>();
        List<String> bidirectedSubgraphLink = new LinkedList<String>();
        for (Pair<Integer,Integer> link : graph.listAllLinks())
        {
        	String lang1 = outputVarName(graph.varNames[link.first]);
        	String lang2 = outputVarName(graph.varNames[link.second]);
        	if (markedEdge != null && ((link.first == markedEdge.first && link.second == markedEdge.second)
        	  ||(link.first == markedEdge.second && link.second == markedEdge.first)))
        	{
        		undirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"#ff0000ff\",penwidth=\"3\"];");
        	}
        	else if (markedNodes != null && markedEdge != null &&
     			   ((markedNodes.contains(link.first) && markedNodes.contains(link.second)) ||
	        			    ((link.first == markedEdge.first || link.first == markedEdge.second) && markedNodes.contains(link.second)) ||
	        			    ((link.second == markedEdge.first || link.second == markedEdge.second) && markedNodes.contains(link.first))))
		    {
        		undirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"#008000ff\",penwidth=\"3\"];");
		    }
        	else
        	{
	        	if (graph.hasArrow(link.first, link.second))
	        	{
	        		if (graph.hasArrow(link.second, link.first))
	        		{
	        			bidirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"#cccc00ff\",penwidth=\"3\"];");
	        		}
	        		else
	        		{
	        			directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"#cc6600ff\",penwidth=\"3\"];");
	        		}
	        	}
	        	else
	        	{
	        		if (graph.hasArrow(link.second, link.first))
	        		{
	        			directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"#cc6600ff\",penwidth=\"3\"];");
	        		}
	        		else
	        		{
	        			undirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"#000000ff\",penwidth=\"3\"];");
	        		}
	        	}
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
        out.println("  edge [arrowsize=2];");
        for (String link : directedSubgraphLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
		
		out.println("}");
		
		out.close();
	}
	
	public static void outputToDotFormat(CausalGraph graph, Map<String, Point2D.Double> coordinates, double strengtheningFactor) 
	{
		outputToDotFormat(graph, System.out, coordinates, strengtheningFactor);
	}
	
	public static void outputToDotFormat(CausalGraph graph, PrintStream out, Map<String, Point2D.Double> coordinates, double strengtheningFactor) 
	{
		//about 3600 pixels wide and 800 pixels high (northern hemisphere only)
		//left bound at longitude -20 degrees (just west of iceland)
		double xOffset = -20.0;
		double scalingFactor = 50;
		outputToDotFormat(graph, out, coordinates, strengtheningFactor, xOffset, scalingFactor, 360);
	}
	
	public static void outputToDotFormat(CausalGraph graph, PrintStream out, Map<String, Point2D.Double> coordinates, double strengtheningFactor, double xOffset, double scalingFactor, double xWrapAround) 
	{
		out.println("digraph CausalGraph");
		out.println("{");
		out.println("  splines=true;");
		out.println("  node [ fontname=Arial, fontcolor=blue, fontsize=20];");
		
		for (String varName : graph.nameToVar.keySet())
		{
			Point2D.Double position = null;
			if (coordinates != null) position = coordinates.get(outputVarName(varName));
			//System.err.println("coordinates.get(" + outputVarName(varName) + "=outputVarName(" + varName + ")) = " + position);
			if (position == null)
			{
				out.println("  " + outputVarName(varName) + ";");
			}
			else
			{
				double xPos = position.getY() - xOffset;
				if (xPos <= 0) xPos += xWrapAround;
				double yPos = position.getX();
				//TODO: context-dependent scaling factor
				out.println("  " + outputVarName(varName) + " [pos=\"" + (xPos * scalingFactor) + "," + (yPos * scalingFactor) + "\", width=\"0.1\", height=\"0.05\"];");
			}
		}
		
		List<String> undirectedSubgraphLink = new LinkedList<String>();
        List<String> directedSubgraphLink = new LinkedList<String>();
        List<String> directedPresetLink = new LinkedList<String>();       
        List<String> bidirectedSubgraphLink = new LinkedList<String>();
        for (Pair<Integer,Integer> link : graph.listAllLinks())
        {
        	String var1Name = outputVarName(graph.varNames[link.first]);
        	String var2Name = outputVarName(graph.varNames[link.second]);
        	
        	//System.err.print("corrMatrix[" + lang1 + "][" + lang2 + "] = ");
        	double linkStrength = graph.getRemainingLinkStrength(link.first, link.second);
        	
        	//System.err.println(correlationStrength);
        	
        	if (linkStrength == 0.0) continue;
        	int lineWidth = 1;
        	//if (correlationStrength > 0.0025) lineWidth++;       	
        	//if (correlationStrength > 0.005) lineWidth++;
        	//if (correlationStrength > 0.01) lineWidth++; 
        	if (linkStrength > 0.02) lineWidth++;   
        	if (linkStrength > 0.04) lineWidth++;   
        	if (linkStrength > 0.08) lineWidth++;   
        	if (linkStrength > 0.16) lineWidth++; 
        	if (linkStrength > 0.32) lineWidth++;
        	if (linkStrength > 0.64) lineWidth++;     	
        	linkStrength *= strengtheningFactor; //25 is good for basic correlation, 100 for remnants after causal inference on cognates, 5 on correlations only
        	if (linkStrength > 1.0) linkStrength = 1.0;
        	String alphaValue = Integer.toHexString((int) (linkStrength * 255));
        	if (alphaValue.length() == 1) alphaValue = "0" + alphaValue; //add 0 in front of single-digit numbers
        	
        	//TODO: compare to gold standard, error color #ff4c4c
        	
        	if (graph.getEndSymbol(link.first, link.second) == CausalGraph.ARROW_END)
        	{
        		if (graph.getEndSymbol(link.second, link.first) == CausalGraph.ARROW_END)
        		{
        			bidirectedSubgraphLink.add(var1Name + " -> " + var2Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.LINE_END)
        		{
        			if (graph.hasPresetArrow(link.first, link.second) || graph.hasPresetLink(link.first, link.second))
        			{
        				directedPresetLink.add(var1Name + " -> " + var2Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        			else
        			{
        				directedSubgraphLink.add(var1Name + " -> " + var2Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END)
        		{
        			directedSubgraphLink.add(var1Name + " -> " + var2Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        	}
        	else if (graph.getEndSymbol(link.first, link.second) == CausalGraph.LINE_END)
        	{
        		if (graph.getEndSymbol(link.second, link.first) == CausalGraph.ARROW_END)
        		{
        			if (graph.hasPresetArrow(link.second, link.first) || graph.hasPresetLink(link.first, link.second))
        			{
        				directedPresetLink.add(var2Name + " -> " + var1Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        			else
        			{
        				directedSubgraphLink.add(var2Name + " -> " + var1Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.LINE_END)
        		{
        			undirectedSubgraphLink.add(var1Name + " -> " + var2Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END)
        		{
    				directedSubgraphLink.add(var2Name + " -> " + var1Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        	}
        	else if (graph.getEndSymbol(link.first, link.second) == CausalGraph.CIRCLE_END)
        	{
        		if (graph.getEndSymbol(link.second, link.first) == CausalGraph.ARROW_END)
        		{
        			if (graph.hasPresetArrow(link.second, link.first) || graph.hasPresetLink(link.first, link.second))
        			{
        				directedPresetLink.add(var2Name + " -> " + var1Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        			else
        			{
        				directedSubgraphLink.add(var2Name + " -> " + var1Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.LINE_END)
        		{
        			directedSubgraphLink.add(var1Name + " -> " + var2Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END)
        		{
    				undirectedSubgraphLink.add(var2Name + " -> " + var1Name + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
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
        out.println("  edge [arrowsize=2];");
        for (String link : directedSubgraphLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
        out.println("subgraph directedPreset");
        out.println("{");
        out.println("  edge [arrowsize=2];");
        for (String link : directedPresetLink)
        {
        	 out.println("  " + link);
        }
        out.println("}");
		
		out.println("}");
		
		out.close();
	}
	
	private static String outputVarName(String varName)
	{
		return "\"" + varName + "\"";
		//return LanguageNames.getLanguageName(LanguageNames.normalize(varName)).replaceAll("::", "_").replaceAll("-", "_").replaceAll(" ", "_");
		//return LanguageNames.normalize(varName).replaceAll("::", "_").replaceAll("-", "_").replaceAll(" ", "_");
	}
	
	public static void outputToDotFormat(CausalGraphSummary graphSummary, PrintStream out, Map<String, Point2D.Double> coordinates, double strengtheningFactor) 
	{
		out.println("digraph CausalGraphSummary");
		out.println("{");
		out.println("  splines=true;");
		out.println("  node [ fontname=Arial, fontcolor=blue, fontsize=20];");
		
		//TODO: support for coordinates
		for (String varName : graphSummary.nameToVar.keySet())
		{
			out.println("  " + outputVarName(varName) + ";");
		}
		
		List<String> undirectedSubgraphLink = new LinkedList<String>();
        List<String> directedSubgraphLink = new LinkedList<String>();
        List<String> directedPresetLink = new LinkedList<String>();       
        List<String> bidirectedSubgraphLink = new LinkedList<String>();
        for (Pair<Integer,Integer> link : graphSummary.listAllLinks())
        {
        	String var1 = outputVarName(graphSummary.varNames[link.first]);
        	String var2 = outputVarName(graphSummary.varNames[link.second]);
        	
        	System.err.print("corrMatrix[" + var1 + "][" + var2 + "] = ");
        	double correlationStrength = ((double) graphSummary.neighborCounts.get(link.first).get(link.second)) / graphSummary.numGraphs;
        	
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
        	
        	int arrowAndArrowCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.ARROW_ARROW);
        	if (arrowAndArrowCount > 0) {
        		lineWidth = (int) ((arrowAndArrowCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			bidirectedSubgraphLink.add(var1 + " -> " + var2 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.ARROW_ARROW) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int lineAndArrowCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.LINE_ARROW);
        	if (lineAndArrowCount > 0) {
        		lineWidth = (int) ((lineAndArrowCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(var1 + " -> " + var2 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.LINE_ARROW) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int circleAndArrowCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.CIRCLE_ARROW);
        	if (circleAndArrowCount > 0) {
        		lineWidth = (int) ((circleAndArrowCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(var1 + " -> " + var2 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.CIRCLE_ARROW) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int arrowAndLineCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.ARROW_LINE);
        	if (arrowAndLineCount > 0) {
        		lineWidth = (int) ((arrowAndLineCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(var2 + " -> " + var1 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.ARROW_LINE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int circleAndLineCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.CIRCLE_LINE);
        	if (circleAndLineCount > 0) {
        		lineWidth = (int) ((circleAndLineCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(var2 + " -> " + var1 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.CIRCLE_LINE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int arrowAndCircleCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.ARROW_CIRCLE);
        	if (arrowAndCircleCount > 0) {
        		lineWidth = (int) ((arrowAndCircleCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(var2 + " -> " + var1 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.ARROW_CIRCLE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int lineAndCircleCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.LINE_CIRCLE);
        	if (lineAndCircleCount > 0) {
        		lineWidth = (int) ((lineAndCircleCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			directedSubgraphLink.add(var1 + " -> " + var2 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.LINE_CIRCLE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        	}
        	
        	int circleAndCircleCount = graphSummary.getArrowCount(link.first, link.second, CausalGraphSummary.CIRCLE_CIRCLE);
        	if (circleAndCircleCount > 0) {
        		lineWidth = (int) ((circleAndCircleCount / (correlationStrength * graphSummary.numGraphs)) / 0.05);
        		if (lineWidth > 0)
        			undirectedSubgraphLink.add(var1 + " -> " + var2 + " [color=\"" + graphSummary.getLinkColor(link.first, link.second, CausalGraphSummary.CIRCLE_CIRCLE) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
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
