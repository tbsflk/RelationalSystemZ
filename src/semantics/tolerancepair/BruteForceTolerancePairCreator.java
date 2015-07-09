package semantics.tolerancepair;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import semantics.ranking.RelationalSystemZ;
import semantics.worlds.RelationalPossibleWorld;
import syntax.RelationalKnowledgeBase;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * Brute-force approach to create tolerance-pairs. All possible partition-pairs
 * for a knowledge base and a domain are generated and tested.
 * 
 * @author Tobias Falke
 * 
 */
public class BruteForceTolerancePairCreator extends TolerancePairCreator {

	/**
	 * Creates a new brute-force tolerance-pair creator.
	 * @param kb
	 *        First-order knowledge base
	 * @param domain
	 *        Domain
	 * @param worlds
	 *        Possible worlds
	 */
	public BruteForceTolerancePairCreator(RelationalKnowledgeBase kb, Collection<Constant> domain, RelationalPossibleWorld[] worlds) {
		super(kb, domain, worlds);
	}

	/**
	 * Starts the creation progress. A reference to an object implementing the
	 * listener interface can be passed, which will then receive updates
	 * regarding the progress.
	 * @param listener
	 *        Progress listener
	 */
	@Override
	public void createPairs(ProgressListener listener) {

		// generate all partition pairs
		List<TolerancePair> allPairs = this.createPartitionPairs(this.kb.getConditionals(), this.domain);
		Collections.sort(allPairs);

		this.pairs = new LinkedList<TolerancePair>();
		this.explanations = new LinkedList<StringBuffer>();

		// test each pair
		double i = 0;
		for (TolerancePair pair : allPairs) {

			RelationalSystemZ rsz = new RelationalSystemZ(this.domain, this.kb, pair, this.worlds);
			if (rsz.getTolerancePair() != null) {
				// is a valid tolerance-pair
				this.pairs.add(pair);
				this.explanations.add(rsz.getTolerancePairExplanation());
			}

			// progress update
			i++;
			if (listener != null) {
				boolean cont = listener.progressChanged(i / allPairs.size());
				if (!cont) {
					this.pairs.clear();
					this.explanations.clear();
					return;
				}
			}
		}

	}

	/**
	 * Creates all possible partition-pairs of a set of conditionals and a
	 * domain, omitting empty subsets.
	 * 
	 * @param conditionals
	 *        Conditionals
	 * @param constants
	 *        Constants
	 * @return Set of all partition-pairs
	 */
	private List<TolerancePair> createPartitionPairs(Collection<RelationalConditional> conditionals, Collection<Constant> constants) {

		List<TolerancePair> allPairs = new LinkedList<TolerancePair>();

		// minimal number of subsets -> 1
		// maximal number of subsets -> min(|R|,|D|)
		int maxPart = Math.min(conditionals.size(), constants.size());
		if (constants.size() == 0) {
			maxPart = conditionals.size();
		}

		for (int n = 1; n <= maxPart; n++) {

			// get conditional partitions
			Collection<RelationalConditional>[][] condPartitions = this.generateKPartitions((List<RelationalConditional>) conditionals, n);
			// get constant partitions
			Collection<Constant>[][] constPartitions = this.generateKPartitions((List<Constant>) constants, n);

			// combine
			for (Collection<RelationalConditional>[] condPartition : condPartitions) {
				if (constPartitions != null) {
					for (Collection<Constant>[] constPartition : constPartitions) {
						TolerancePair pair = this.createPair(condPartition, constPartition);
						if (pair != null) {
							allPairs.add(pair);
						}
					}
				} else {
					// propositional edge case
					TolerancePair pair = this.createPair(condPartition, null);
					if (pair != null) {
						allPairs.add(pair);
					}
				}
			}
		}

		return allPairs;
	}

	/**
	 * Creates a partition-pair from a partition of conditionals and constants.
	 * @param condPartition
	 *        Partition of conditionals
	 * @param constPartition
	 *        Partition of constants
	 * @return Partition-pair
	 */
	private TolerancePair createPair(Collection<RelationalConditional>[] condPartition, Collection<Constant>[] constPartition) {
		TolerancePair pair = new TolerancePair(condPartition.length);
		boolean valid = true;
		// combine both partitions in one data structure
		for (int i = 0; i < condPartition.length; i++) {
			// test for empty subsets
			if (condPartition[i].isEmpty() || constPartition != null && constPartition[i].isEmpty()) {
				valid = false;
				break;
			}
			pair.setConditionalPart(i, condPartition[i]);
			if (constPartition != null) {
				pair.setConstantPart(i, constPartition[i]);
			}
		}
		if (valid)
			return pair; // ok
		else
			return null; // invalid partition
	}

	/**
	 * Returns all partitions of a set into k subsets.
	 * @param set
	 *        Set
	 * @param k
	 *        Number of subsets
	 * @return Partitions
	 */
	@SuppressWarnings("unchecked")
	private <T> Collection<T>[][] generateKPartitions(List<T> set, int k) {

		int n = set.size();
		if (n == 0)
			return null;
		int size = (int) Math.pow(k, n);
		Collection<T>[][] partitions = new LinkedList[size][];

		// generate all n-digit number in base k
		for (int i = 0; i < size; i++) {
			partitions[i] = new LinkedList[k];
			for (int j = 0; j < k; j++) {
				partitions[i][j] = new LinkedList<T>();
			}
			// and interprete the nth digit of that number, which is a digit out
			// of [0..k-1], to be the assignment of the nth element of the set
			// to one of the k subsets
			for (int j = 0; j < n; j++) {
				int p = (int) (i / Math.pow(k, j) % k);
				partitions[i][p].add(set.get(j));
			}
		}

		return partitions;
	}

}
