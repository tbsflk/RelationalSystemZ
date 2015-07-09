package semantics.ranking;

import java.util.Collection;
import java.util.LinkedList;

import semantics.tolerancepair.TolerancePair;
import semantics.worlds.RelationalPossibleWorld;
import semantics.worlds.RelationalPossibleWorldFactory;
import syntax.GroundingOperator;
import syntax.RelationalKnowledgeBase;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Sort;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * Implementation of a system z-like approach to create a ranking function for a
 * first-order conditional knowledge base. Necessary input for such a ranking
 * function is a {@link TolerancePair}. Please note that this class does not
 * check any of the restrictions to the first-order language that apply for this
 * approach, but expects all inputs to be correct. This can be ensured using
 * {@link syntax.RelationalKnowledgeBaseReader}.
 * 
 * @author Tobias Falke
 * 
 */
public class RelationalSystemZ {

	/**
	 * First-order knowledge base KB=<R,F>
	 */
	private final RelationalKnowledgeBase kb;
	/**
	 * Tolerance Pair
	 */
	private TolerancePair pair;
	/**
	 * Explanation of pair
	 */
	private StringBuffer pairExplanation;
	/**
	 * Computed ranking function
	 */
	private final ExplainedRelationalRankingFunction kappa;
	/**
	 * Normalization value
	 */
	private int kZero;
	/**
	 * Domain of individuals
	 */
	private final Collection<Constant> domain;
	/**
	 * Possible worlds
	 */
	private RelationalPossibleWorld[] worlds;
	/**
	 * Grounding operator for conditional instantiation
	 */
	private final GroundingOperator gop;

	/**
	 * Creates an instance of System Z that checks the pair and, if valid,
	 * computes the ranking function.
	 * @param domain
	 *        Domain of individuals
	 * @param kb
	 *        First-order knowledge base
	 * @param pair
	 *        Tolerance pair
	 * @param worlds
	 *        Possible Worlds
	 */
	public RelationalSystemZ(Collection<Constant> domain, RelationalKnowledgeBase kb, TolerancePair pair, RelationalPossibleWorld[] worlds) {

		this.domain = domain;
		this.kb = kb;
		this.gop = new GroundingOperator();
		this.worlds = worlds;
		this.kappa = new ExplainedRelationalRankingFunction(this.worlds, this.domain);

		if (this.isTolerancePair(pair)) {
			this.pair = pair;
		} else {
			this.pair = null;
		}
		this.computeRankingFunction();

	}

	/**
	 * Creates an instance of System Z that checks the pair and, if valid,
	 * computes the ranking function. Possible worlds are generated.
	 * @param domain
	 *        Domain of individuals
	 * @param kb
	 *        First-order knowledge base
	 * @param pair
	 *        Tolerance pair
	 */
	public RelationalSystemZ(Collection<Constant> domain, RelationalKnowledgeBase kb, TolerancePair pair) {

		Collection<RelationalAtom> atoms = this.kb.getAtomsFromKnowledgeBase(domain, this.gop);
		RelationalPossibleWorldFactory worldFactory = new RelationalPossibleWorldFactory();
		this.worlds = worldFactory.createPossibleWorlds(atoms, domain);

		this.domain = domain;
		this.kb = kb;
		this.gop = new GroundingOperator();
		this.kappa = new ExplainedRelationalRankingFunction(this.worlds, this.domain);

		if (this.isTolerancePair(pair)) {
			this.pair = pair;
		} else {
			this.pair = null;
		}
		this.computeRankingFunction();

	}

	/**
	 * Returns the ranking function.
	 * @return Ranking function which computed ranks, or 0 for each world, if
	 *         the partition is not valid
	 */
	public ExplainedRelationalRankingFunction getRankingFunction() {
		return this.kappa;
	}

	/**
	 * Returns the tolerance pair.
	 * @return Provided pair if valid, null otherwise
	 */
	public TolerancePair getTolerancePair() {
		return this.pair;
	}

	/**
	 * Returns explanations for the validity of the tolerance pair.
	 * @return Explanation
	 */
	public StringBuffer getTolerancePairExplanation() {
		return this.pairExplanation;
	}

	/**
	 * Returns the normalization value.
	 * @return kappa_0
	 */
	public int getKappaZero() {
		return this.kZero;
	}

	/**
	 * Computes the ranking function based on the tolerance pair.
	 */
	private void computeRankingFunction() {
		if (this.pair != null) {

			int m = this.pair.getNOfParts() - 1;

			// set kappa rank for each world and store minimum
			this.kZero = this.kappa.INFINITY;
			for (RelationalPossibleWorld world : this.worlds) {
				if (!RelationalSystemZ.satisfiesFacts(world, this.kb.getFacts())) {
					this.kappa.setRank(world, this.kappa.INFINITY);
				} else {
					int sum = 0;
					for (int i = 0; i <= m; i++) {
						sum += Math.pow(m + 2, i) * this.lambda(i, world);
					}
					this.kappa.setRank(world, sum);
					if (sum < this.kZero) {
						this.kZero = sum;
					}
				}
			}

			// normalize ranks by subtracting the minimal rank
			if (this.kZero > 0) {
				for (RelationalPossibleWorld world : this.worlds) {
					if (this.kappa.getRank(world) != this.kappa.INFINITY) {
						this.kappa.setRank(world, this.kappa.getRank(world) - this.kZero);
					}
				}
			}
		}
	}

	/**
	 * Compute lambda for a world and a subset.
	 * @param i
	 *        Subset of tolerance pair
	 * @param world
	 *        World
	 * @return lambda value
	 */
	public int lambda(int i, RelationalPossibleWorld world) {
		if (this.pair == null)
			return this.kappa.INFINITY;
		else {
			// find max part falsifying a conditional with constants from i
			for (int j = this.pair.getNOfParts() - 1; j >= 0; j--) {
				Collection<Constant> constantsForPart = this.pair.getConstantPart(i);
				if (constantsForPart.isEmpty()) {
					// add dummy constant for propositional case
					constantsForPart = new LinkedList<Constant>();
					constantsForPart.add(new Constant("", new Sort("")));
				}
				for (Constant a : constantsForPart) {
					for (RelationalConditional c : this.pair.getConditionalPart(j)) {
						RelationalConditional cg = this.gop.groundConditional(c, a);
						if (this.kappa.falsifies(world, cg))
							return j + 1;
					}
				}
			}
			// nothing falsified -> 0
			return 0;
		}
	}

	/**
	 * Checks whether a tolerance pair is a valid tolerance pair for this
	 * knowledge base and domain.
	 * 
	 * @param pair
	 *        Tolerance pair
	 * @return true, if valid, false otherwise
	 */
	public boolean isTolerancePair(TolerancePair pair) {

		this.pairExplanation = new StringBuffer();

		// check all conditionals
		for (int i = 0; i < pair.getNOfParts(); i++) {
			for (RelationalConditional c : pair.getConditionalPart(i)) {
				boolean hasWorld = false;

				// for all worlds satisfying the facts
				for (RelationalPossibleWorld world : this.worlds) {
					if (hasWorld) {
						break;
					}
					if (RelationalSystemZ.satisfiesFacts(world, this.kb.getFacts())) {

						// try all their instantiations
						Collection<Constant> constantsForPart = pair.getConstantPart(i);
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
								for (int j = i; j < pair.getNOfParts() && !condFalsified; j++) {
									for (RelationalConditional c2 : pair.getConditionalPart(j)) {
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
								}

								// we found a world for this conditional
								if (!condFalsified) {
									hasWorld = true;
									// add explanation
									this.pairExplanation.append("i=" + i + ", ");
									this.pairExplanation.append("r=" + c + ", a=" + a + System.lineSeparator());
									this.pairExplanation.append(world + System.lineSeparator() + System.lineSeparator());
									break;
								}
							}
						}

					}
				}

				// no world for this conditional, hence no tolerance-pair
				if (!hasWorld)
					return false;
			}
		}

		// we found a world for every conditional
		return true;
	}

	/**
	 * Checks whether a world satisfies a set of facts (set of closed first
	 * order formulas).
	 * 
	 * @param world
	 *        World
	 * @param facts
	 *        Set of facts
	 * @return true, if all facts are satisfied, false otherwise
	 */
	public static boolean satisfiesFacts(RelationalPossibleWorld world, Collection<Formula<RelationalAtom>> facts) {
		for (Formula<RelationalAtom> f : facts) {
			if (!world.satisfies(f))
				return false;
		}
		return true;
	}

}
