package de.tuebingen.sfs.causal.heuristics.arrows;

import java.util.LinkedList;
import java.util.List;

import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.causal.data.CausalGraphOutput;
import de.tuebingen.sfs.util.struct.Pair;

public class OrientationRules {
	private static final boolean VERBOSE = false;

	public static boolean applyZhangOrientationRule1(CausalGraph graph, String[] varNames) {
		// R1: unshielded A *-> B o-* C => A *-> B --> C
		boolean hasChanged = false;
		for (Integer[] triple : graph.listUnshieldedTriples()) {
			if (graph.getEndSymbol(triple[0], triple[2]) == CausalGraph.ARROW_END
					&& graph.getEndSymbol(triple[1], triple[2]) == CausalGraph.CIRCLE_END) {
				if (VERBOSE)
					System.out.println("  R01: " + varNames[triple[0]] + " "
							+ graph.getLinkRepresentation(triple[0], triple[2]) + " " + varNames[triple[2]] + " "
							+ graph.getLinkRepresentation(triple[2], triple[1]) + " " + varNames[triple[1]]
							+ ", therefore " + varNames[triple[2]] + " --> " + varNames[triple[1]]);
				graph.setEndSymbol(triple[1], triple[2], CausalGraph.LINE_END);
				graph.setEndSymbol(triple[2], triple[1], CausalGraph.ARROW_END);
				hasChanged = true;
			} else if (graph.getEndSymbol(triple[1], triple[2]) == CausalGraph.ARROW_END
					&& graph.getEndSymbol(triple[0], triple[2]) == CausalGraph.CIRCLE_END) {
				if (VERBOSE)
					System.out.println("  R01: " + varNames[triple[1]] + " "
							+ graph.getLinkRepresentation(triple[1], triple[2]) + " " + varNames[triple[2]] + " "
							+ graph.getLinkRepresentation(triple[2], triple[0]) + " " + varNames[triple[0]]
							+ ", therefore " + varNames[triple[2]] + " --> " + varNames[triple[0]]);
				graph.setEndSymbol(triple[0], triple[2], CausalGraph.LINE_END);
				graph.setEndSymbol(triple[2], triple[0], CausalGraph.ARROW_END);
				hasChanged = true;
			}
		}
		return hasChanged;
	}

	public static boolean applyZhangOrientationRule2(CausalGraph graph, String[] varNames) {
		boolean hasChanged = false;
		for (Pair<Integer, Integer> link : graph.listAllLinksInBothDirections()) {
			if (graph.getEndSymbol(link.first, link.second) == CausalGraph.CIRCLE_END) {
				for (int b : graph.getNeighborsOnAcyclicPathsBetween(link.first, link.second, 1)) {
					if (graph.getEndSymbol(link.first, b) == CausalGraph.ARROW_END
							&& graph.getEndSymbol(b, link.second) == CausalGraph.ARROW_END) {
						if (graph.getEndSymbol(b, link.first) == CausalGraph.LINE_END
								|| graph.getEndSymbol(link.second, b) == CausalGraph.LINE_END) {
							graph.setEndSymbol(link.first, link.second, CausalGraph.ARROW_END);
							if (VERBOSE)
								System.out.println("  R02: " + varNames[link.first] + " "
										+ graph.getLinkRepresentation(link.first, b) + " " + varNames[b] + " "
										+ graph.getLinkRepresentation(b, link.second) + " " + varNames[link.second]
										+ ", therefore " + varNames[link.first] + " "
										+ graph.getLinkRepresentation(link.first, link.second) + " "
										+ varNames[link.second]);
							hasChanged = true;
						}
					}
				}
			}
		}
		return hasChanged;
	}

	public static boolean applyZhangOrientationRule3(CausalGraph graph, String[] varNames) {
		boolean hasChanged = false;
		for (Integer[] triple : graph.listUnshieldedTriples()) {
			if (graph.getEndSymbol(triple[0], triple[2]) == CausalGraph.ARROW_END
					&& graph.getEndSymbol(triple[1], triple[2]) == CausalGraph.ARROW_END) {
				for (int d : graph.getNeighborsOnAcyclicPathsBetween(triple[0], triple[1], 1)) {
					if (graph.hasLink(d, triple[2]) && graph.getEndSymbol(d, triple[2]) == CausalGraph.CIRCLE_END) {
						if (graph.getEndSymbol(triple[0], d) == CausalGraph.CIRCLE_END
								&& graph.getEndSymbol(triple[1], triple[2]) == CausalGraph.CIRCLE_END) {
							graph.setEndSymbol(d, triple[2], CausalGraph.ARROW_END);
							if (VERBOSE)
								System.out.println("  R03: " + varNames[triple[0]] + " "
										+ graph.getLinkRepresentation(triple[0], triple[2]) + " " + varNames[triple[2]]
										+ " " + graph.getLinkRepresentation(triple[2], triple[1]) + " "
										+ varNames[triple[1]] + ", therefore " + varNames[d] + " "
										+ graph.getLinkRepresentation(d, triple[2]) + " " + varNames[triple[2]]);
							hasChanged = true;
						}
					}
				}
			}
		}
		return hasChanged;
	}

	public static boolean applyZhangOrientationRule5(CausalGraph graph, String[] varNames) {
		// R5: A o-o B and uncovered circle path <A,C,...,D,B>, A -/- D, B -/- C 
		//  => A --- C --- ... --- D --- B --- A
		boolean hasChanged = false;
		for (Pair<Integer, Integer> link : graph.listAllLinksInBothDirections()) {
			if (graph.getEndSymbol(link.first, link.second) == CausalGraph.CIRCLE_END
					&& graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END) {
				List<List<Integer>> circlePaths = graph.getUncoveredCirclePathsBetween(link.first, link.second,
						varNames.length);
				while (circlePaths.size() > 0) {
					boolean circlePathChange = false;
					for (List<Integer> circlePath : circlePaths) {
						// for a path smaller than <A,C,D,B>, we would inevitably have A *-* D and C *-*
						// B (not a path otherwise)
						if (circlePath.size() > 1) {
							int c = circlePath.get(0);
							int d = circlePath.get(circlePath.size() - 1);
							if (graph.hasLink(link.first, d) || graph.hasLink(link.second, c))
								continue;

							graph.setEndSymbol(link.first, link.second, CausalGraph.LINE_END);
							graph.setEndSymbol(link.second, link.first, CausalGraph.LINE_END);
							if (VERBOSE)
								System.out.print("  R05: found uncovered circle path " + varNames[link.first] + " "
										+ graph.getLinkRepresentation(link.first, circlePath.get(0)) + " "
										+ CausalGraphOutput.pathToString(circlePath, graph) + " "
										+ graph.getLinkRepresentation(circlePath.get(circlePath.size() - 1),
												link.second)
										+ " " + varNames[link.second] + ", therefore " + varNames[link.first] + " "
										+ graph.getLinkRepresentation(link.first, link.second) + " "
										+ varNames[link.second]);

							int v1 = -1;
							int v2 = link.first;
							for (int nextVariable : circlePath) {
								v1 = v2;
								v2 = nextVariable;
								graph.setEndSymbol(v1, v2, CausalGraph.LINE_END);
								graph.setEndSymbol(v2, v1, CausalGraph.LINE_END);
							}
							graph.setEndSymbol(v2, link.second, CausalGraph.LINE_END);
							graph.setEndSymbol(link.second, v2, CausalGraph.LINE_END);

							if (VERBOSE)
								System.out
										.println(
												" and " + varNames[link.first] + " "
														+ graph.getLinkRepresentation(link.first, circlePath.get(0))
														+ " " + CausalGraphOutput.pathToString(circlePath, graph) + " "
														+ graph.getLinkRepresentation(
																circlePath.get(circlePath.size() - 1), link.second)
														+ " " + varNames[link.second]);
							hasChanged = true;
							circlePathChange = true;
							break;
						}
					}
					if (!circlePathChange)
						break;
					circlePaths = graph.getUncoveredCirclePathsBetween(link.first, link.second, varNames.length);
				}
			}
		}
		return hasChanged;
	}

	public static boolean applyZhangOrientationRule6(CausalGraph graph, String[] varNames) {
		boolean hasChanged = false;
		// R6: A --- B o-* C => B --* C
		for (Pair<Integer, Integer> abLink : graph.listAllLinksInBothDirections()) {
			if (graph.getEndSymbol(abLink.first, abLink.second) == CausalGraph.LINE_END
					&& graph.getEndSymbol(abLink.second, abLink.first) == CausalGraph.LINE_END) {
				for (int c : graph.getNeighbors(abLink.second)) {
					if (graph.getEndSymbol(c, abLink.second) == CausalGraph.CIRCLE_END) {
						if (VERBOSE)
							System.out.print("  R06: " + varNames[abLink.first] + " "
									+ graph.getLinkRepresentation(abLink.first, abLink.second) + " "
									+ varNames[abLink.second] + " " + graph.getLinkRepresentation(abLink.second, c)
									+ " " + varNames[c]);
						graph.setEndSymbol(c, abLink.second, CausalGraph.LINE_END);
						if (VERBOSE)
							System.out.println(", therefore " + varNames[abLink.second] + " "
									+ graph.getLinkRepresentation(abLink.second, c) + " " + varNames[c]);
						hasChanged = true;
					}
				}
			}
		}

		return hasChanged;
	}

	public static boolean applyZhangOrientationRule7(CausalGraph graph, String[] varNames) {
		boolean hasChanged = false;
		// R7: unshielded A --o B o-* C => B --*C
		for (Integer[] triple : graph.listUnshieldedTriples()) {
			if (graph.getEndSymbol(triple[0], triple[2]) == CausalGraph.CIRCLE_END
					&& graph.getEndSymbol(triple[2], triple[0]) == CausalGraph.LINE_END
					&& graph.getEndSymbol(triple[1], triple[2]) == CausalGraph.CIRCLE_END) {
				if (VERBOSE)
					System.out.print("  R07: " + varNames[triple[0]] + " "
							+ graph.getLinkRepresentation(triple[0], triple[2]) + " " + varNames[triple[2]] + " "
							+ graph.getLinkRepresentation(triple[2], triple[1]) + " " + varNames[triple[1]]);
				graph.setEndSymbol(triple[1], triple[2], CausalGraph.LINE_END);
				if (VERBOSE)
					System.out.println(", therefore " + varNames[triple[2]] + " "
							+ graph.getLinkRepresentation(triple[2], triple[1]) + " " + varNames[triple[1]]);
				hasChanged = true;
			} else if (graph.getEndSymbol(triple[1], triple[2]) == CausalGraph.CIRCLE_END
					&& graph.getEndSymbol(triple[2], triple[1]) == CausalGraph.LINE_END
					&& graph.getEndSymbol(triple[0], triple[2]) == CausalGraph.CIRCLE_END) {
				if (VERBOSE)
					System.out.print("  R07: " + varNames[triple[1]] + " "
							+ graph.getLinkRepresentation(triple[1], triple[2]) + " " + varNames[triple[2]] + " "
							+ graph.getLinkRepresentation(triple[2], triple[0]) + " " + varNames[triple[0]]);
				graph.setEndSymbol(triple[0], triple[2], CausalGraph.LINE_END);
				if (VERBOSE)
					System.out.println(", therefore " + varNames[triple[2]] + " "
							+ graph.getLinkRepresentation(triple[2], triple[0]) + " " + varNames[triple[0]]);
				hasChanged = true;
			}
		}

		return hasChanged;
	}

	public static boolean applyZhangOrientationRule8(CausalGraph graph, String[] varNames) {
		// R8: A o-> C and (A --> B --> C or A --o B --> C) => A --> C
		boolean hasChanged = false;
		for (Pair<Integer, Integer> link : graph.listAllLinksInBothDirections()) {
			if (graph.getEndSymbol(link.first, link.second) == CausalGraph.ARROW_END
					&& graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END) {
				for (int b : graph.getNeighborsOnAcyclicPathsBetween(link.first, link.second, 1)) {
					if ((graph.getEndSymbol(link.first, b) == CausalGraph.ARROW_END
							|| graph.getEndSymbol(link.first, b) == CausalGraph.CIRCLE_END)
							&& graph.getEndSymbol(b, link.first) == CausalGraph.LINE_END
							&& graph.getEndSymbol(b, link.second) == CausalGraph.ARROW_END
							&& graph.getEndSymbol(link.second, b) == CausalGraph.LINE_END) {
						graph.setEndSymbol(link.second, link.first, CausalGraph.LINE_END);
						if (VERBOSE)
							System.out.println(
									"  R08: " + varNames[link.first] + " " + graph.getLinkRepresentation(link.first, b)
											+ " " + varNames[b] + " " + graph.getLinkRepresentation(b, link.second)
											+ " " + varNames[link.second] + ", therefore " + varNames[link.first] + " "
											+ graph.getLinkRepresentation(link.first, link.second) + " "
											+ varNames[link.second]);
						hasChanged = true;
					}
				}
			}
		}
		return hasChanged;
	}

	public static boolean applyZhangOrientationRule9(CausalGraph graph, String[] varNames) {
		// R9: A o-> C and uncovered potentially directed path <A,B,D,...,C>, B -/- C =>
		// A --> C
		boolean hasChanged = false;
		for (Pair<Integer, Integer> link : graph.listAllLinksInBothDirections()) {
			if (graph.getEndSymbol(link.first, link.second) == CausalGraph.ARROW_END
					&& graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END) {
				// find non-trivial uncovered potentially directed path <A,B,D,...,C>
				for (List<Integer> path : graph.getUncoveredPotentiallyDirectedPathsBetween(link.first, link.second,
						varNames.length)) {
					if (path.size() > 0) {
						if (graph.hasLink(path.get(0), link.second))
							continue;
						graph.setEndSymbol(link.second, link.first, CausalGraph.LINE_END);
						if (VERBOSE)
							System.out.println("  R09: found uncovered potentially directed path "
									+ varNames[link.first] + " " + graph.getLinkRepresentation(link.first, path.get(0))
									+ " " + CausalGraphOutput.pathToString(path, graph) + " "
									+ graph.getLinkRepresentation(path.get(path.size() - 1), link.second) + " "
									+ varNames[link.second] + ", therefore " + varNames[link.first] + " "
									+ graph.getLinkRepresentation(link.first, link.second) + " "
									+ varNames[link.second]);
						hasChanged = true;
						break;
					}
				}
			}
		}
		return hasChanged;
	}

	public static boolean applyZhangOrientationRule10(CausalGraph graph, String[] varNames) {
		// R10: A o-> C, B --> C <-- D, <A,M,...,B>, <A,N,...,D>, M != N, M -/- N => A
		// --> C
		boolean hasChanged = false;
		for (Pair<Integer, Integer> link : graph.listAllLinksInBothDirections()) {
			if (graph.getEndSymbol(link.first, link.second) == CausalGraph.ARROW_END
					&& graph.getEndSymbol(link.second, link.first) == CausalGraph.CIRCLE_END) {
				List<Integer> relevantCNeighbors = new LinkedList<Integer>();
				for (int neighborCandidate : graph.getNeighbors(link.second)) {
					if (graph.getEndSymbol(neighborCandidate, link.second) == CausalGraph.ARROW_END
							&& graph.getEndSymbol(link.second, neighborCandidate) == CausalGraph.LINE_END) {
						relevantCNeighbors.add(neighborCandidate);
					}
				}
				// find a fitting pair of paths for any relevant neighbor candidate
				for (int bIndex = 0; bIndex < relevantCNeighbors.size(); bIndex++) {
					int b = relevantCNeighbors.get(bIndex);
					for (List<Integer> abPath : graph.getUncoveredPotentiallyDirectedPathsBetween(link.first, b,
							varNames.length)) {
						if (abPath.size() > 0) {
							int m = abPath.get(0);
							for (int dIndex = bIndex + 1; dIndex < relevantCNeighbors.size(); dIndex++) {
								int d = relevantCNeighbors.get(dIndex);
								for (List<Integer> adPath : graph
										.getUncoveredPotentiallyDirectedPathsBetween(link.first, d, varNames.length)) {
									if (adPath.size() > 0) {
										int n = adPath.get(0);
										if (n == m)
											continue;
										if (graph.hasLink(n, m))
											continue;
										// pattern found, orient and
										graph.setEndSymbol(link.first, link.second, CausalGraph.LINE_END);
										if (VERBOSE)
											System.out.println(
													"  R10: found diverging pair of potentially directed paths "
															+ varNames[link.first] + " "
															+ graph.getLinkRepresentation(link.first, abPath.get(0))
															+ " " + CausalGraphOutput.pathToString(abPath, graph) + " "
															+ graph.getLinkRepresentation(abPath.get(abPath.size() - 1),
																	b)
															+ " " + varNames[b] + " and " + varNames[link.first] + " "
															+ graph.getLinkRepresentation(link.first, adPath.get(0))
															+ " " + CausalGraphOutput.pathToString(adPath, graph) + " "
															+ graph.getLinkRepresentation(adPath.get(adPath.size() - 1),
																	d)
															+ " " + varNames[d] + ", therefore " + varNames[link.second]
															+ " " + graph.getLinkRepresentation(link.second, link.first)
															+ " " + varNames[link.first]);
										hasChanged = true;
										return hasChanged;
									}
								}
							}
						}
					}
				}
			}
		}
		return hasChanged;
	}
}
