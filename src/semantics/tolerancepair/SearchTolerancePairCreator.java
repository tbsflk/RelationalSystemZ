package semantics.tolerancepair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import semantics.ranking.RelationalRankingFunction;
import semantics.ranking.RelationalSystemZ;
import semantics.worlds.RelationalPossibleWorld;
import syntax.GroundingOperator;
import syntax.RelationalKnowledgeBase;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Sort;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * Backtracking-Search approach to create tolerance-pairs. A search tree is
 * explored to find all tolerance-pairs.
 * 
 * This creator does not give progress information.
 * 
 * @author Tobias Falke
 * 
 */
public class SearchTolerancePairCreator extends TolerancePairCreator {

	/**
	 * Possible worlds satisfying the facts
	 */
	protected Collection<RelationalPossibleWorld> validWorlds;
	/**
	 * Ranking function
	 */
	protected RelationalRankingFunction kappa;
	/**
	 * Grounding operator to instantiate conditionals
	 */
	protected GroundingOperator gop;
	/**
	 * Visited nodes in the search tree
	 */
	protected HashSet<TolerancePair> visited;
	/**
	 * Progress listener
	 */
	protected ProgressListener listener;

	protected boolean propositionalCase;

	/**
	 * Creates a new backtracking-search tolerance-pair creator.
	 * @param kb
	 *        First-order knowledge base
	 * @param domain
	 *        Domain
	 * @param worlds
	 *        Possible worlds
	 */
	public SearchTolerancePairCreator(RelationalKnowledgeBase kb, Collection<Constant> domain, RelationalPossibleWorld[] worlds) {
		super(kb, domain, worlds);
		// build set of worlds satisfying the facts
		this.validWorlds = new LinkedList<RelationalPossibleWorld>();
		for (RelationalPossibleWorld world : this.worlds) {
			if (RelationalSystemZ.satisfiesFacts(world, this.kb.getFacts())) {
				this.validWorlds.add(world);
			}
		}
		this.kappa = new RelationalRankingFunction(this.worlds, this.domain);
		this.gop = new GroundingOperator();
	}

	/**
	 * Starts the creation progress. No progress updates a provided for the tree
	 * search.
	 * @param listener
	 *        Progress listener
	 */
	@Override
	public void createPairs(ProgressListener listener) {

		// init sets
		this.pairs = new LinkedList<TolerancePair>();
		this.visited = new HashSet<TolerancePair>();
		this.explanations = new LinkedList<StringBuffer>();
		Set<RelationalConditional> leftConditionals = new HashSet<RelationalConditional>(this.kb.getConditionals());
		Set<Constant> leftConstants = new HashSet<Constant>(this.domain);

		// propositional edge case
		if (leftConstants.isEmpty()) {
			this.propositionalCase = true;
		} else {
			this.propositionalCase = false;
		}

		this.listener = listener;

		// start with root node
		this.search(new TolerancePair(1), leftConditionals, leftConstants);
		Collections.sort(this.pairs);

	}

	/**
	 * Explores a node in the search tree, representing a partially- (or fully-)
	 * built partition-pair.
	 * @param pair
	 *        Current partition-pair
	 * @param leftConditionals
	 *        Left conditionals
	 * @param leftConstants
	 *        Left constants
	 */
	protected void search(TolerancePair pair, Set<RelationalConditional> leftConditionals, Set<Constant> leftConstants) {

		// check search cancellation
		if (this.listener != null && !this.listener.progressChanged(0)) {
			this.pairs.clear();
			this.explanations.clear();
			return;
		}

		// test if the node should be expanded
		if (!this.test(pair, leftConditionals, leftConstants))
			return;

		// expand
		int subset = pair.getNOfParts() - 1;

		// add a pair
		if (!this.propositionalCase && pair.getConditionalPart(subset).isEmpty() && pair.getConstantPart(subset).isEmpty()) {
			for (RelationalConditional c : leftConditionals) {
				for (Constant a : leftConstants) {
					this.addAndSearch(pair, leftConditionals, c, leftConstants, a);
				}
			}
		} else {
			// add a conditional
			if (!pair.getConstantPart(subset).isEmpty() || this.propositionalCase) {
				for (RelationalConditional c : leftConditionals) {
					this.addAndSearch(pair, leftConditionals, c, leftConstants, null);
				}
			}
			// add a constant
			if (!pair.getConditionalPart(subset).isEmpty()) {
				for (Constant a : leftConstants) {
					this.addAndSearch(pair, leftConditionals, null, leftConstants, a);
				}
			}
		}
		// add a level (if current is filled and next can also be filled)
		if (!pair.getConditionalPart(subset).isEmpty() && (!pair.getConstantPart(subset).isEmpty() || this.propositionalCase)) {
			if (!leftConditionals.isEmpty() && (!leftConstants.isEmpty() || this.propositionalCase)) {
				TolerancePair newPair = pair.extend(new HashSet<RelationalConditional>(), new HashSet<Constant>());
				this.search(newPair, leftConditionals, leftConstants);
			}
		}

	}

	/**
	 * Adds a new conditional, constant or both to the last subset of the
	 * partition-pair and explores it again.
	 * @param pair
	 *        Current partition-pair
	 * @param leftConditionals
	 *        Left conditionals
	 * @param c
	 *        Conditional to be added
	 * @param leftConstants
	 *        Left constants
	 * @param a
	 *        Constant to be added
	 */
	protected void addAndSearch(TolerancePair pair, Set<RelationalConditional> leftConditionals, RelationalConditional c, Set<Constant> leftConstants, Constant a) {
		// create a copy
		TolerancePair newPair = pair.copy();
		// add the conditional
		Set<RelationalConditional> newLeftConditionals = new HashSet<RelationalConditional>(leftConditionals);
		if (c != null) {
			newLeftConditionals.remove(c);
			newPair.getConditionalPart(pair.getNOfParts() - 1).add(c);
		}
		// add the constant
		Set<Constant> newLeftConstants = new HashSet<Constant>(leftConstants);
		if (a != null) {
			newLeftConstants.remove(a);
			newPair.getConstantPart(pair.getNOfParts() - 1).add(a);
		}
		// explore the tree of this node
		this.search(newPair, newLeftConditionals, newLeftConstants);
	}

	/**
	 * Tests if the node of this partition-pair should be expanded.
	 * @param pair
	 *        Current partition-pair
	 * @param leftConditionals
	 *        Left Conditionals
	 * @param leftConstants
	 *        Left constants
	 * @return true, if it should be further expanded, false otherwise
	 */
	protected boolean test(TolerancePair pair, Set<RelationalConditional> leftConditionals, Set<Constant> leftConstants) {

		// node: empty subset -> needs to be expanded
		int subset = pair.getNOfParts() - 1;
		if (pair.getConditionalPart(subset).isEmpty() && pair.getConstantPart(subset).isEmpty())
			return true;

		// leaf: duplicate -> no expansion
		if (this.visited.contains(pair))
			return false;
		else {
			// first occurrence
			this.visited.add(pair);
		}

		// leaf: potential tolerance-pair found -> no expansion
		if (leftConditionals.isEmpty() && (leftConstants.isEmpty() || this.propositionalCase)) {
			// last test for conditions
			RelationalSystemZ rsz = new RelationalSystemZ(this.domain, this.kb, pair, this.worlds);
			if (rsz.getTolerancePair() != null) {
				this.pairs.add(pair);
				this.explanations.add(rsz.getTolerancePairExplanation());
			}
			return false;
		}

		// leaf: conditions failed -> no expansion
		if (!this.isPartialTolerancePair(pair, leftConditionals))
			return false;

		// otherwise -> expand node
		return true;
	}

	/**
	 * Checks if the conditions for a tolerance-pair are satisfied for the
	 * conditionals in the last subset. For each, it checks if there is a world
	 * in which it is verified for a constant and no other is falsified for any
	 * constant.
	 * @param pair
	 *        Current partition-pair
	 * @param leftConditionals
	 *        Left conditionals
	 * @return true, if such a world is found for each conditional, false
	 *         otherwise
	 */
	protected boolean isPartialTolerancePair(TolerancePair pair, Set<RelationalConditional> leftConditionals) {

		// check all conditionals
		for (RelationalConditional c : pair.getConditionalPart(pair.getNOfParts() - 1)) {
			boolean hasWorld = false;

			// for all worlds satisfying the facts
			for (RelationalPossibleWorld world : this.validWorlds) {
				if (hasWorld) {
					break;
				}

				// try all their instantiations
				Collection<Constant> constantsForPart = pair.getConstantPart(pair.getNOfParts() - 1);
				if (constantsForPart.isEmpty()) {
					// add dummy constant for propositional case
					constantsForPart = new LinkedList<Constant>();
					constantsForPart.add(new Constant("", new Sort("")));
				}
				for (Constant a : constantsForPart) {

					RelationalConditional cg = this.gop.groundConditional(c, a);

					// and if one is verified
					if (this.kappa.verifies(world, cg)) {

						boolean condFalsified = false;
						// and no other conditional is falsified
						Set<RelationalConditional> checkConditionals = new HashSet<RelationalConditional>(leftConditionals);
						checkConditionals.addAll(pair.getConditionalPart(pair.getNOfParts() - 1));
						for (RelationalConditional c2 : checkConditionals) {
							if (condFalsified) {
								break;
							}
							for (Constant a2 : constantsForPart) {
								RelationalConditional c2g = this.gop.groundConditional(c2, a2);
								if (this.kappa.falsifies(world, c2g)) {
									condFalsified = true;
									break;
								}
							}
						}

						// we found a world for this conditional
						if (!condFalsified) {
							hasWorld = true;
							break;
						}
					}
				}

			}

			// no world for this conditional, hence no tolerance-pair
			if (!hasWorld)
				return false;
		}

		// we found a world for every conditional
		return true;
	}

}