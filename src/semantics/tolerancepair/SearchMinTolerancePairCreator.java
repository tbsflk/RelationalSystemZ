package semantics.tolerancepair;

import java.util.Collection;
import java.util.Set;

import semantics.ranking.RelationalSystemZ;
import semantics.worlds.RelationalPossibleWorld;
import syntax.RelationalKnowledgeBase;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * Backtracking-Search approach to create minimal tolerance-pairs. A search tree
 * is explored to find all minimal tolerance-pairs.
 * 
 * This creator does not give progress information.
 * 
 * @author Tobias Falke
 * 
 */
public class SearchMinTolerancePairCreator extends SearchTolerancePairCreator {

	/**
	 * Creates a new backtracking-search minimal tolerance-pair creator.
	 * @param kb
	 *        First-order knowledge base
	 * @param domain
	 *        Domain
	 * @param worlds
	 *        Possible worlds
	 */
	public SearchMinTolerancePairCreator(RelationalKnowledgeBase kb, Collection<Constant> domain, RelationalPossibleWorld[] worlds) {
		super(kb, domain, worlds);
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
	@Override
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

		// leaf: cannot be minimal -> no expansion
		if (this.pairs.size() > 0 && this.pairs.get(0).compareToPartial(pair) < 0)
			return false;

		// leaf: potential tolerance-pair found -> no expansion
		if (leftConditionals.isEmpty() && (leftConstants.isEmpty() || this.propositionalCase)) {
			// last test for conditions
			RelationalSystemZ rsz = new RelationalSystemZ(this.domain, this.kb, pair, this.worlds);
			if (rsz.getTolerancePair() != null) {
				// minimal?
				int comp = this.pairs.size() == 0 ? 0 : pair.compareTo(this.pairs.get(0));
				if (comp <= 0) { // is (current) minimal
					if (comp < 0) { // others are bigger
						this.pairs.clear();
						this.explanations.clear();
					}
					this.pairs.add(pair);
					this.explanations.add(rsz.getTolerancePairExplanation());
				}
				return false;
			} else
				return false;
		}

		// leaf: conditions failed -> no expansion
		if (!this.isPartialTolerancePair(pair, leftConditionals))
			return false;

		// otherwise -> expand node
		return true;
	}

}