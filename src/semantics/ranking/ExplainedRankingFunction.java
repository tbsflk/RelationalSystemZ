package semantics.ranking;

import edu.cs.ai.log4KR.logical.semantics.Interpretation;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.Interpretable;
import edu.cs.ai.log4KR.logical.syntax.probabilistic.Conditional;
import gui.Explanation;

import java.util.Collection;

/**
 * Extended version of {@link RankingFunction} that returns explanations for
 * each evaluation. An evaluation tree is built with instances of
 * {@link Explanation}.
 * 
 * @author Tobias Falke
 * 
 * @param <I>
 *        Interpretable
 */
public class ExplainedRankingFunction<I extends Interpretable> extends RankingFunction<I> {

	/**
	 * Creates a new ranking function for the given set of worlds, initially
	 * mapping each world to 0.
	 * 
	 * @param worlds
	 *        Possible worlds
	 */
	public ExplainedRankingFunction(Interpretation<I>[] worlds) {
		super(worlds);
	}

	/**
	 * Returns the rank for a formula, which is the minimal rank of all worlds
	 * in which the formula is satisfied, or {@link RankingFunction#INFINITY},
	 * if there is no such world.
	 * 
	 * @param f
	 *        Formula
	 * @param exp
	 *        Explanation
	 * @return Rank
	 */
	public int getRank(Formula<I> f, Explanation exp) {
		int minRank = this.INFINITY;
		Interpretation<I> minWorld = null;
		for (Interpretation<I> world : this.worlds) {
			int rank = this.getRank(world);
			if (world.satisfies(f) && rank < minRank) {
				minRank = rank;
				minWorld = world;
			}
		}
		if (exp != null) {
			if (minWorld != null) {
				new Explanation(exp, minWorld.toString());
			}
			exp.setText("k( " + f + " ) = " + minRank + " (Def. 2)");
		}
		return minRank;
	}

	/**
	 * Returns the rank for a conditional, which is the difference between the
	 * rank of its verification and the rank of its antecedence. If the
	 * verification is ranked with {@link RankingFunction#INFINITY}, it is also
	 * the result.
	 * 
	 * @param c
	 *        Conditional
	 * @param exp
	 *        Explanation
	 * @return Rank
	 */
	public int getRank(Conditional<I> c, Explanation exp) {
		int vRank = this.getVerificationRank(c, new Explanation(exp));
		if (vRank == this.INFINITY) {
			if (exp != null) {
				exp.setText("k" + c + " = " + vRank);
			}
			return this.INFINITY;
		}
		int aRank = this.getRank(c.getAntecedence(), new Explanation(exp));
		if (exp != null) {
			new Explanation(exp, "k" + c + " = " + vRank + " - " + aRank + " = " + (vRank - aRank));
			exp.setText("k" + c + " = " + (vRank - aRank));
		}
		return vRank - aRank;
	}

	/**
	 * Determines whether a formula is accepted, which is the case if it is
	 * satisfied in all worlds ranked most plausible, i.e. with 0.
	 * 
	 * @param f
	 *        Formula
	 * @param exp
	 *        Explanation
	 * @return true, if it is accepted, false otherwise
	 */
	public boolean accepts(Formula<I> f, Explanation exp) {
		int n = 0;
		for (Interpretation<I> world : this.worlds) {
			if (this.getRank(world) == 0) {
				if (!world.satisfies(f)) {
					if (exp != null) {
						new Explanation(exp, f + " invalid in world with k=0 (Def. 3)");
						new Explanation(exp, world.toString());
						exp.setText("k |= " + f + " -> false");
					}
					return false;
				}
				n++;
			}
		}
		if (exp != null) {
			new Explanation(exp, f + " valid in all " + n + " worlds with k=0 (Def. 3)");
			exp.setText("k |= " + f + " -> true");
		}
		return true;
	}

	/**
	 * Determines whether the given conditional is accepted by the ranking
	 * function, which is the case if the conditionals verification has a lower
	 * rank than its falsification.
	 * 
	 * @param c
	 *        Conditional
	 * @param exp
	 *        Explanation
	 * @return true, if it is accepted, false otherwise
	 */
	public boolean accepts(Conditional<I> c, Explanation exp) {
		int vRank = this.getVerificationRank(c, new Explanation(exp));
		int fRank = this.getFalsificationRank(c, new Explanation(exp));
		if (exp != null) {
			new Explanation(exp, vRank + " < " + fRank + " ? (Def. 3)");
		}
		if (vRank < fRank) {
			if (exp != null) {
				exp.setText("k |= " + c + " -> true");
			}
			return true;
		} else {
			if (exp != null) {
				exp.setText("k |= " + c + " -> false");
			}
			return false;
		}
	}

	/**
	 * Determines whether a set of conditionals is accepted by the ranking
	 * function, which is the case if all conditionals are accepted by the
	 * ranking function.
	 * 
	 * @param conditionals
	 *        Set of conditionals
	 * @param exp
	 *        Explanation
	 * @return true, if all conditionals are accepted, false otherwise
	 */
	public boolean accepts(Collection<Conditional<I>> conditionals, Explanation exp) {
		for (Conditional<I> c : conditionals) {
			if (!this.accepts(c, new Explanation(exp))) {
				if (exp != null) {
					new Explanation(exp, c + " not accepted (Def. 6.1)");
					exp.setText("k |= R -> false");
				}
				return false;
			}
		}
		if (exp != null) {
			new Explanation(exp, "all accepted (Def. 6.1)");
			exp.setText("k |= R -> true");
		}
		return true;
	}

	/**
	 * Returns the rank of the conditional's verification formula, i.e. the rank
	 * of formula AB for conditional (B|A).
	 * 
	 * @param c
	 *        Conditional
	 * @param exp
	 *        Explanation
	 * @return Rank
	 */
	public int getVerificationRank(Conditional<I> c, Explanation exp) {
		return this.getRank(this.getVerificationFormula(c), exp);
	}

	/**
	 * Returns the rank of the conditional's falsification formula, i.e. the
	 * rank of the formula A!B for conditional (B|A).
	 * 
	 * @param c
	 *        Conditional
	 * @param exp
	 *        Explanation
	 * @return Rank
	 */
	public int getFalsificationRank(Conditional<I> c, Explanation exp) {
		return this.getRank(this.getFalsificationFormula(c), exp);
	}

}
