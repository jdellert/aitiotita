package de.tuebingen.sfs.causal.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.tuebingen.sfs.causal.data.CausalGraph;
import de.tuebingen.sfs.causal.heuristics.arrows.CausalArrowFinder;
import de.tuebingen.sfs.causal.heuristics.separation.PartialCorrelation;
import de.tuebingen.sfs.util.struct.Pair;
import de.tuebingen.sfs.util.struct.RankingEntry;
import de.tuebingen.sfs.util.struct.SetOperations;

public class PcStarAlgorithm extends PcAlgorithm {
	
	boolean randomLinkOrder;

	public PcStarAlgorithm(PartialCorrelation<?> corrMeasure, CausalArrowFinder<?> arrowFinder, String[] varNames,
			CausalGraph initialGraphWithConstraints, int maxCondSetSize, boolean stable, boolean conservative, boolean randomLinkOrder) {
		super(corrMeasure, arrowFinder, varNames, initialGraphWithConstraints, maxCondSetSize, stable, conservative);
		this.randomLinkOrder = randomLinkOrder;
	}

	public void runSkeletonInference() {
		remainingLinkStrength = new TreeMap<Integer, Map<Integer, Double>>();
		for (int var = 0; var < varNames.length; var++) {
			remainingLinkStrength.put(var, new TreeMap<Integer, Double>());
		}

		for (int depth = 0; depth <= maxCondSetSize; depth++) {
			List<Pair<Integer, Integer>> links = graph.listAllDeletableLinks();
			System.out.println(
					"Proceeding to separating set size " + depth + ", " + links.size() + " potentially deletable links left.");
			
			if (links.size() == 0) {
				System.out.println("Skeleton inference is finished.");
				break;
			}

			// sort links by remaining link strength, remove weakest links first
			List<RankingEntry<Pair<Integer, Integer>>> linkRanking = new ArrayList<RankingEntry<Pair<Integer, Integer>>>(
					links.size());
			for (Pair<Integer, Integer> link : links) {
				double minPartialCorrelation = 1.0;
				if (depth > 0)
					minPartialCorrelation = remainingLinkStrength.get(link.first).get(link.second);
				linkRanking.add(new RankingEntry<Pair<Integer, Integer>>(link, minPartialCorrelation));
			}
			
			if (randomLinkOrder) {
				Collections.shuffle(linkRanking);
			} else {
				Collections.sort(linkRanking);
			}		

			for (RankingEntry<Pair<Integer, Integer>> rankedLink : linkRanking) {
				Pair<Integer, Integer> link = rankedLink.key;
				double minPartialCorrelation = rankedLink.value;

				boolean presetLink = graph.hasPresetLink(link.first, link.second);
				boolean foundSepSet = false;

				// pcStar algorithm: only consider neighbors on undirected acyclic paths between
				// link.first and link.second
				List<Integer> neighbors = new ArrayList<Integer>();
				neighbors.addAll(graph.getNeighborsOnAcyclicPathsBetween(link.first, link.second, 2));

				List<Set<Integer>> separatingSetCandidates = SetOperations.getSubsets(neighbors, depth);

				if (!presetLink) {
					if (!BASIC_INFO || separatingSetCandidates.size() > 0)
						System.out.println("  Attempting to separate pair " + varNames[link.first] + "-"
								+ varNames[link.second] + " using the " + neighbors.size() + " other neighbors of "
								+ varNames[link.first] + " and " + varNames[link.second]
								+ " on connecting paths, forming " + separatingSetCandidates.size()
								+ " separating set candidates");
				} else {
					if (!BASIC_INFO || separatingSetCandidates.size() > 0)
						System.out.println("  Minimizing partial correlation for fixed link " + varNames[link.first]
								+ "-" + varNames[link.second] + " using the " + neighbors.size()
								+ " other neighbors of " + varNames[link.first] + " and " + varNames[link.second]
								+ " on connecting paths, forming " + separatingSetCandidates.size()
								+ " separating set candidates");
				}
				if (VERBOSE)
					System.out.println("  Remaining link strength: " + minPartialCorrelation);
				for (Set<Integer> separatingSetCandidate : separatingSetCandidates) {
					double partialCorrelation = corrMeasure.partialCorrelation(link.first, link.second,
							separatingSetCandidate);
					if (partialCorrelation < minPartialCorrelation) {
						minPartialCorrelation = partialCorrelation;
						if (VERBOSE)
							System.out.println("      Reduced link strength: " + minPartialCorrelation);
					}
					if (!presetLink && corrMeasure.independenceTest(partialCorrelation, link.first, link.second,
							separatingSetCandidate)) {
						if (BASIC_INFO)
							System.out.println("    successful independence test, link (" + varNames[link.first] + ","
									+ varNames[link.second] + ") can be removed by conditioning on "
									+ varSetToString(separatingSetCandidate));
						storeSepSet(link.first, link.second, separatingSetCandidate);
						storeSepSet(link.second, link.first, separatingSetCandidate);
						foundSepSet = true;
					}
				}
				if (foundSepSet) {
					corrMeasure.registerSepSets(link.first, link.second,
							separatingSets.get(link.first).get(link.second));
					if (arrowFinder != null)
						arrowFinder.registerSepSets(link.first, link.second,
								separatingSets.get(link.first).get(link.second));
					graph.removeLink(link.first, link.second);
				} else {
					if (presetLink)
						remainingLinkStrength.get(link.first).put(link.second, 1.0);
					else
						remainingLinkStrength.get(link.first).put(link.second, minPartialCorrelation);
				}
			}

			// optimization: set to undeletable if maximum condition set size was reached
			links = graph.listAllDeletableLinks();
			for (Pair<Integer, Integer> link : links) {
				if (corrMeasure.maxCondSetSizeReached(link.first, link.second, depth)) {
					if (VERBOSE)
						System.err.println("    maximal condition set size reached at " + depth + " for link ("
								+ varNames[link.first] + "," + varNames[link.second] + "), marked as unremovable.");
					graph.setUndeletableLink(link.first, link.second, true);
				}
			}
		}
	}
}
