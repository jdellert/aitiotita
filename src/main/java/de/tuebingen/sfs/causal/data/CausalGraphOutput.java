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
		
		out.println("digraph LanguageGraph");
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
	
	public static void outputToDotFormat(CausalGraph graph, Map<String, Point2D.Double> coordinates, Double[][] corrMatrix, double strengtheningFactor) 
	{
		outputToDotFormat(graph, System.out, coordinates, corrMatrix, strengtheningFactor);
	}
	
	public static void outputToDotFormat(CausalGraph graph, PrintStream out, Map<String, Point2D.Double> coordinates, Double[][] corrMatrix, double strengtheningFactor) 
	{
		//about 3600 pixels wide and 800 pixels high (northern hemisphere only)
		//left bound at longitude -20 degrees (just west of iceland)
		double xOffset = -20.0;
		double scalingFactor = 50;
		outputToDotFormat(graph, out, coordinates, corrMatrix, strengtheningFactor, xOffset, scalingFactor, 360);
	}
	
	public static void outputToDotFormat(CausalGraph graph, PrintStream out, Map<String, Point2D.Double> coordinates, Double[][] corrMatrix, double strengtheningFactor, double xOffset, double scalingFactor, double xWrapAround) 
	{
		out.println("digraph LanguageGraph");
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
        	String lang1 = outputVarName(graph.varNames[link.first]);
        	String lang2 = outputVarName(graph.varNames[link.second]);
        	
        	//System.err.print("corrMatrix[" + lang1 + "][" + lang2 + "] = ");
        	double correlationStrength = Math.abs(corrMatrix[link.first][link.second]);
        	
        	//System.err.println(correlationStrength);
        	
        	if (correlationStrength == 0.0) continue;
        	int lineWidth = 1;
        	//if (correlationStrength > 0.0025) lineWidth++;       	
        	//if (correlationStrength > 0.005) lineWidth++;
        	//if (correlationStrength > 0.01) lineWidth++; 
        	if (correlationStrength > 0.02) lineWidth++;   
        	if (correlationStrength > 0.04) lineWidth++;   
        	if (correlationStrength > 0.08) lineWidth++;   
        	if (correlationStrength > 0.16) lineWidth++; 
        	if (correlationStrength > 0.32) lineWidth++;
        	if (correlationStrength > 0.64) lineWidth++;     	
        	correlationStrength *= strengtheningFactor; //25 is good for basic correlation, 100 for remnants after causal inference on cognates, 5 on correlations only
        	if (correlationStrength > 1.0) correlationStrength = 1.0;
        	String alphaValue = Integer.toHexString((int) (correlationStrength * 255));
        	if (alphaValue.length() == 1) alphaValue = "0" + alphaValue; //add 0 in front of single-digit numbers
        	
        	//TODO: compare to gold standard, error color #ff4c4c
        	
        	if (graph.getEndSymbol(link.first, link.second) == CausalGraph.ARROW_END)
        	{
        		if (graph.getEndSymbol(link.second, link.first) == CausalGraph.ARROW_END)
        		{
        			bidirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.LINE_END)
        		{
        			if (graph.hasPresetArrow(link.first, link.second) || graph.hasPresetLink(link.first, link.second))
        			{
        				directedPresetLink.add(lang1 + " -> " + lang2 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        			else
        			{
        				directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END)
        		{
        			directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        	}
        	else if (graph.getEndSymbol(link.first, link.second) == CausalGraph.LINE_END)
        	{
        		if (graph.getEndSymbol(link.second, link.first) == CausalGraph.ARROW_END)
        		{
        			if (graph.hasPresetArrow(link.second, link.first) || graph.hasPresetLink(link.first, link.second))
        			{
        				directedPresetLink.add(lang2 + " -> " + lang1 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        			else
        			{
        				directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.LINE_END)
        		{
        			undirectedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END)
        		{
    				directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        	}
        	else if (graph.getEndSymbol(link.first, link.second) == CausalGraph.CIRCLE_END)
        	{
        		if (graph.getEndSymbol(link.second, link.first) == CausalGraph.ARROW_END)
        		{
        			if (graph.hasPresetArrow(link.second, link.first) || graph.hasPresetLink(link.first, link.second))
        			{
        				directedPresetLink.add(lang2 + " -> " + lang1 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        			else
        			{
        				directedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        			}
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.LINE_END)
        		{
        			directedSubgraphLink.add(lang1 + " -> " + lang2 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
        		}
        		else if (graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END)
        		{
    				undirectedSubgraphLink.add(lang2 + " -> " + lang1 + " [color=\"" + graph.getLinkColor(link.first, link.second) + alphaValue + "\",penwidth=\""  + lineWidth + "\"];");
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
		return varName;
		//return LanguageNames.getLanguageName(LanguageNames.normalize(varName)).replaceAll("::", "_").replaceAll("-", "_").replaceAll(" ", "_");
		//return LanguageNames.normalize(varName).replaceAll("::", "_").replaceAll("-", "_").replaceAll(" ", "_");
	}
}
