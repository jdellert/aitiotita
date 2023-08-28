package de.tuebingen.sfs.causal.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
			CausalGraph initialGraphWithConstraints, int maxCondSetSize, boolean stable, boolean conservative,
			boolean acyclicity, boolean randomLinkOrder) {
		super(corrMeasure, arrowFinder, varNames, initialGraphWithConstraints, maxCondSetSize, stable, conservative,
				acyclicity);
		this.randomLinkOrder = randomLinkOrder;
	}

	public void runSkeletonInference() {
		initializeSepSets();

		for (int depth = 0; depth <= maxCondSetSize; depth++) {
			List<Pair<Integer, Integer>> links = graph.listAllDeletableLinks();
			System.out.println("Proceeding to separating set size " + depth + ", " + links.size()
					+ " potentially deletable links left.");

			if (links.size() == 0) {
				System.out.println("Skeleton inference is finished.");
				break;
			}

			// sort links by remaining link strength, remove weakest links first
			List<RankingEntry<Pair<Integer, Integer>>> linkRanking = new ArrayList<RankingEntry<Pair<Integer, Integer>>>(
					links.size());
			// use length as random seed to make sure the random tie-breaking noise 
			// (and thereby the link ranking) will always be the same on the same data
			Random rand = new Random(links.size());
			for (Pair<Integer, Integer> link : links) {
				double partialCorrelation = corrMeasure.correlation(link.first, link.second);
				if (depth > 0)
					partialCorrelation = graph.getRemainingLinkStrength(link.first, link.second);
				// add uniformly distributed noise of up to 5 percent the original value
				partialCorrelation += rand.nextDouble() * partialCorrelation / 20;
				linkRanking.add(new RankingEntry<Pair<Integer, Integer>>(link, partialCorrelation));
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
						System.out.println(
								"  Attempting to separate pair " + varNames[link.first] + "-" + varNames[link.second]
										+ " using the " + neighbors.size() + " neighbors of " + varNames[link.first]
										+ " and " + varNames[link.second] + " on connecting paths, forming "
										+ separatingSetCandidates.size() + " separating set candidates");
				} else {
					if (!BASIC_INFO || separatingSetCandidates.size() > 0)
						System.out.println("  Minimizing partial correlation for fixed link " + varNames[link.first]
								+ "-" + varNames[link.second] + " using the " + neighbors.size() + " neighbors of "
								+ varNames[link.first] + " and " + varNames[link.second]
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
						graph.setRemainingLinkStrength(link.first, link.second, 1.0);
					else
						graph.setRemainingLinkStrength(link.first, link.second, minPartialCorrelation);
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
