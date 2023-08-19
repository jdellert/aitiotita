package de.tuebingen.sfs.causal.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.tuebingen.sfs.util.io.ListReader;
import de.tuebingen.sfs.util.struct.Pair;

public class CausalGraphInput {
	public static CausalGraph loadFirstKLinksFromTextFormat(String fileName, int k) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		
		String line = in.readLine(); //first line: #names
		line = in.readLine();
		
		TreeMap<Integer,String> varNames = new TreeMap<Integer,String>();
		while (!line.equals("#links"))
		{
			
			String[] tokens = line.split("\t");
			int id = Integer.parseInt(tokens[0]);
			String varName = tokens[1];
			varNames.put(id, varName);
			
			line = in.readLine();
		}
		CausalGraph graph = new CausalGraph(varNames.values().toArray(new String[varNames.size()]), false);
		
		line = in.readLine(); //line: #links
		int numLinkLines = 0;
		while (line != null && numLinkLines < k)
		{
			String[] tokens = line.split("\t");
			String var1 = tokens[0];
			String var2 = tokens[2];
			int var1ID = graph.nameToVar.get(var1);
			int var2ID = graph.nameToVar.get(var2);
			graph.addLink(var1ID, var2ID);
			numLinkLines++;
			
			String[] linkRepParts = tokens[1].split("");
			
			if ("o".equals(linkRepParts[0])) graph.setEndSymbol(var2ID, var1ID, CausalGraph.CIRCLE_END);
			if ("-".equals(linkRepParts[0])) graph.setEndSymbol(var2ID, var1ID, CausalGraph.LINE_END);
			if ("<".equals(linkRepParts[0])) graph.setEndSymbol(var2ID, var1ID, CausalGraph.ARROW_END);

			if ("o".equals(linkRepParts[2])) graph.setEndSymbol(var1ID, var2ID, CausalGraph.CIRCLE_END);
			if ("-".equals(linkRepParts[2])) graph.setEndSymbol(var1ID, var2ID, CausalGraph.LINE_END);
			if (">".equals(linkRepParts[2])) graph.setEndSymbol(var1ID, var2ID, CausalGraph.ARROW_END);
			
			line = in.readLine();
		}
		in.close();
		return graph;
	}
	
	public static CausalGraph loadFromTextFormat(String fileName) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		
		String line = in.readLine(); //first line: #names
		line = in.readLine();
		
		TreeMap<Integer,String> varNames = new TreeMap<Integer,String>();
		while (!line.equals("#links"))
		{
			
			String[] tokens = line.split("\t");
			int id = Integer.parseInt(tokens[0]);
			String varName = tokens[1];
			varNames.put(id, varName);
			
			line = in.readLine();
		}
		CausalGraph graph = new CausalGraph(varNames.values().toArray(new String[varNames.size()]), false);
		
		line = in.readLine(); //line: #links
		while (line != null)
		{
			String[] tokens = line.split("\t");
			String var1 = tokens[0];
			String var2 = tokens[2];
			int var1ID = graph.nameToVar.get(var1);
			int var2ID = graph.nameToVar.get(var2);
			graph.addLink(var1ID, var2ID);
			
			String[] linkRepParts = tokens[1].split("");
			
			if ("o".equals(linkRepParts[0])) graph.setEndSymbol(var2ID, var1ID, CausalGraph.CIRCLE_END);
			if ("-".equals(linkRepParts[0])) graph.setEndSymbol(var2ID, var1ID, CausalGraph.LINE_END);
			if ("<".equals(linkRepParts[0])) graph.setEndSymbol(var2ID, var1ID, CausalGraph.ARROW_END);

			if ("o".equals(linkRepParts[2])) graph.setEndSymbol(var1ID, var2ID, CausalGraph.CIRCLE_END);
			if ("-".equals(linkRepParts[2])) graph.setEndSymbol(var1ID, var2ID, CausalGraph.LINE_END);
			if (">".equals(linkRepParts[2])) graph.setEndSymbol(var1ID, var2ID, CausalGraph.ARROW_END);
			
			line = in.readLine();
		}
		in.close();
		return graph;
	}
	
	public static Pair<CausalGraph,Map<Integer,Map<Integer,List<Set<Integer>>>>> loadFromSkeletonFile(String skeletonFileName) throws IOException
	{
		List<String[]> lines = ListReader.arrayFromTSV(skeletonFileName);
		
		//extract var names, build symbol table
		List<String> varNameList = new ArrayList<String>();
		Map<String,Integer> varToID = new TreeMap<String,Integer>();
		for (String[] line : lines)
		{
			String varName1 = line[0];
			String varName2 = line[2];
			if (varToID.get(varName1) == null)
			{
				varToID.put(varName1, varNameList.size());
				varNameList.add(varName1);
			}
			if (varToID.get(varName2) == null)
			{
				varToID.put(varName2, varNameList.size());
				varNameList.add(varName2);
			}
		}
		String[] varNames = varNameList.toArray(new String[varNameList.size()]);
		CausalGraph graph = new CausalGraph(varNames, false);
		Map<Integer,Map<Integer,List<Set<Integer>>>> sepSets = new TreeMap<Integer,Map<Integer,List<Set<Integer>>>>();
		
		//load connections (line[1] = o-o) and separating sets (line[3], split by "  ")
		for (String[] line : lines)
		{
			int var1 = varToID.get(line[0]);
			int var2 = varToID.get(line[2]);
			if (line[1].equals("o-o"))
			{
				graph.addLink(var1, var2);
			}
			else if (line[1].equals("ooo"))
			{
				List<Set<Integer>> sepSetsEntry = new LinkedList<Set<Integer>>();
				for (String sepSetString : line[3].split("  "))
				{
					Set<Integer> sepSet = new TreeSet<Integer>();
					for (String varName : sepSetString.substring(1, sepSetString.length() - 1).split(","))
					{
						if (varName.length() > 0)
						{
							sepSet.add(varToID.get(varName));
						}
					}
					sepSetsEntry.add(sepSet);
				}
				if (sepSets.get(var1) == null) sepSets.put(var1, new TreeMap<Integer,List<Set<Integer>>>());
				if (sepSets.get(var2) == null) sepSets.put(var2, new TreeMap<Integer,List<Set<Integer>>>());
				sepSets.get(var1).put(var2, sepSetsEntry);
				sepSets.get(var2).put(var1, sepSetsEntry);
			}
			else
			{
				System.err.println("WARNING: could not interpret and ignored line in skeleton file: " + line[1]);
			}
		}
		
		return new Pair<CausalGraph,Map<Integer,Map<Integer,List<Set<Integer>>>>>(graph, sepSets);
	}
}
