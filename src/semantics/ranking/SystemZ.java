package semantics.ranking;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import edu.cs.ai.log4KR.logical.semantics.Interpretation;
import edu.cs.ai.log4KR.logical.syntax.probabilistic.Conditional;
import edu.cs.ai.log4KR.propositional.classicalLogic.semantics.PropositionalPossibleWorldMapRepresentationFactory;
import edu.cs.ai.log4KR.propositional.classicalLogic.syntax.PropositionalVariable;
import edu.cs.ai.log4KR.propositional.util.PropositionalUtils;

/**
 * System Z calculates a unique minimal ranking function for a propositional
 * conditional knowledge base. The set of conditionals is partitioned into an
 * ordered set of sets of conditionals, each of it consisting of the
 * conditionals that are tolerated by all conditionals within the same and the
 * following sets. A conditional is tolerated by a set of conditionals if there
 * is a possible world in which it is verified and none of the other
 * conditionals if falsified. If the knowledge base is consistent, such a
 * partitioning exists.
 * 
 * @author Tobias Falke
 * 
 */
public class SystemZ {

	/**
	 * Ranking function
	 */
	private final RankingFunction<PropositionalVariable> kappa;
	/**
	 * Possible worlds
	 */
	private final Interpretation<PropositionalVariable>[] worlds;
	/**
	 * Knowledge base
	 */
	private final Collection<Conditional<PropositionalVariable>> kb;
	/**
	 * Mapping of conditionals to subsets
	 */
	private HashMap<Conditional<PropositionalVariable>, Integer> partition;
	/**
	 * Number of subsets in partition
	 */
	private int nOfSubsets;
	/**
	 * Indicates whether the knowledge base is consistent
	 */
	private boolean isConsistent = false;

	/**
	 * Creates a new instance of System Z for the given knowledge base. Possible
	 * worlds will be derived, the conditionals partitioned and the minimal
	 * ranking function will be calculated.
	 * 
	 * @param knowledgeBase
	 *        Knowledge base
	 */
	public SystemZ(Collection<Conditional<PropositionalVariable>> knowledgeBase) {
		this.kb = knowledgeBase;
		PropositionalPossibleWorldMapRepresentationFactory worldFactory = new PropositionalPossibleWorldMapRepresentationFactory();
		Collection<PropositionalVariable> atoms = PropositionalUtils.getAtomsFromKnowledgeBase(knowledgeBase);
		this.worlds = worldFactory.createPossibleWorlds(atoms);
		this.kappa = new RankingFunction<PropositionalVariable>(this.worlds);
		this.computeRankingFunction();
	}

	/**
	 * Returns the computed ranking function.
	 * 
	 * @return Ranking function, if the knowledge base is consistent, null
	 *         otherwise
	 */
	public RankingFunction<PropositionalVariable> getRankingFunction() {
		return this.isConsistent ? this.kappa : null;
	}

	/**
	 * Returns the partition of the knowledge base.
	 * 
	 * @return Partition, if the knowledge base is consistent, null otherwise
	 */
	public Collection<Conditional<PropositionalVariable>>[] getPartition() {
		if (this.nOfSubsets == 0)
			return null;
		@SuppressWarnings("unchecked")
		Collection<Conditional<PropositionalVariable>>[] partition = new LinkedList[this.nOfSubsets];
		for (int i = 0; i < this.nOfSubsets; i++) {
			partition[i] = new LinkedList<Conditional<PropositionalVariable>>();
		}
		for (Conditional<PropositionalVariable> c : this.kb) {
			partition[this.partition.get(c)].add(c);
		}
		return partition;
	}

	/**
	 * Returns whether the knowledge base is consistent or not.
	 * 
	 * @return true, if the knowledge base is consistent, false otherwise
	 */
	public boolean isConsistent() {
		return this.isConsistent;
	}

	/**
	 * Computes the partition and the minimal ranking function based on it.
	 */
	private void computeRankingFunction() {

		// determine the partition for the knowledge base
		this.partition = new HashMap<Conditional<PropositionalVariable>, Integer>();
		Collection<Conditional<PropositionalVariable>> left = new LinkedList<Conditional<PropositionalVariable>>(this.kb);
		int i = 0;
		while (left.size() > 0) {
			Collection<Conditional<PropositionalVariable>> part = new LinkedList<Conditional<PropositionalVariable>>();
			for (Conditional<PropositionalVariable> c : left) {
				if (this.isTolerated(c, left)) {
					part.add(c);
					this.partition.put(c, i);
				}
			}
			// we still have conditionals, but none is tolerated -> inconsistent
			// knowledge base
			if (part.isEmpty()) {
				this.isConsistent = false;
				this.nOfSubsets = 0;
				return;
			}
			left.removeAll(part);
			i++;
		}
		this.isConsistent = true;
		this.nOfSubsets = i;

		// set ranking for worlds based on falsification
		for (Interpretation<PropositionalVariable> world : this.worlds) {
			int maxPart = -1;
			for (Conditional<PropositionalVariable> c : this.kb) {
				if (this.kappa.falsifies(world, c) && this.partition.get(c) > maxPart) {
					maxPart = this.partition.get(c);
				}
			}
			maxPart += 1;
			this.kappa.setRank(world, maxPart);
		}
	}

	/**
	 * Check whether a conditional is tolerated by a set of conditionals (a
	 * knowledge base), which is the case if there is a possible world in which
	 * the conditional is verified and in which no conditional of the knowledge
	 * base is falsified.
	 * 
	 * @param c
	 *        Conditional
	 * @param kb
	 *        Knowledge base
	 * @return true, if the conditional is verified, false otherwise
	 */
	private boolean isTolerated(Conditional<PropositionalVariable> c, Collection<Conditional<PropositionalVariable>> kb) {
		boolean tolerated = false;
		for (Interpretation<PropositionalVariable> world : this.worlds) {
			if (this.kappa.verifies(world, c)) {
				tolerated = true;
				for (Conditional<PropositionalVariable> ckb : kb) {
					if (this.kappa.falsifies(world, ckb)) {
						tolerated = false;
						break;
					}
				}
				if (tolerated)
					return true;
			}
		}
		return false;
	}
}