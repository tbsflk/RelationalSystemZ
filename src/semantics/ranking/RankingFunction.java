package semantics.ranking;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cs.ai.log4KR.logical.semantics.Interpretation;
import edu.cs.ai.log4KR.logical.syntax.ElementaryConjunction;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.Interpretable;
import edu.cs.ai.log4KR.logical.syntax.Literal;
import edu.cs.ai.log4KR.logical.syntax.probabilistic.Conditional;

/**
 * A ranking function provides semantics to qualitative conditionals. Each
 * possible world is mapped to a rank, a positive integer describing its
 * plausibility, with 0 given to a most plausible world.
 * 
 * @author Tobias Falke
 * 
 * @param <I>
 *        Interpretable
 */
public class RankingFunction<I extends Interpretable> {

	/**
	 * Value used to denote infinity
	 */
	public final int INFINITY = Integer.MAX_VALUE;
	/**
	 * Possible worlds
	 */
	protected final Interpretation<I>[] worlds;
	/**
	 * Mapping of worlds to ranks
	 */
	protected final Map<Interpretation<I>, Integer> ranking;

	/**
	 * Creates a new ranking function for the given set of worlds, initially
	 * mapping each world to 0.
	 * 
	 * @param worlds
	 *        Possible worlds
	 */
	public RankingFunction(Interpretation<I>[] worlds) {
		this.worlds = worlds;
		this.ranking = new HashMap<Interpretation<I>, Integer>();
		for (Interpretation<I> world : worlds) {
			this.ranking.put(world, 0);
		}
	}

	/**
	 * Returns the rank assigned to the given world.
	 * 
	 * @param world
	 *        Possible world
	 * @return Rank
	 */
	public int getRank(Interpretation<I> world) {
		return this.ranking.get(world);
	}

	/**
	 * Sets the rank for a specific world.
	 * 
	 * @param world
	 *        Possible world
	 * @param rank
	 *        Rank
	 */
	public void setRank(Interpretation<I> world, int rank) {
		this.ranking.put(world, rank);
	}

	/**
	 * Returns the rank for a formula, which is the minimal rank of all worlds
	 * in which the formula is satisfied, or {@link RankingFunction#INFINITY},
	 * if there is no such world.
	 * 
	 * @param f
	 *        Formula
	 * @return Rank
	 */
	public int getRank(Formula<I> f) {
		int minRank = this.INFINITY;
		for (Interpretation<I> world : this.worlds) {
			int rank = this.getRank(world);
			if (world.satisfies(f) && rank < minRank) {
				minRank = rank;
			}
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
	 * @return Rank
	 */
	public int getRank(Conditional<I> c) {
		int verRank = this.getVerificationRank(c);
		if (verRank == this.INFINITY)
			return this.INFINITY;
		else
			return verRank - this.getRank(c.getAntecedence());
	}

	/**
	 * Determines whether a formula is accepted, which is the case if it is
	 * satisfied in all worlds ranked most plausible, i.e. with 0.
	 * 
	 * @param f
	 *        Formula
	 * @return true, if it is accepted, false otherwise
	 */
	public boolean accepts(Formula<I> f) {
		for (Interpretation<I> world : this.worlds) {
			if (this.getRank(world) == 0 && !world.satisfies(f))
				return false;
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
	 * @return true, if it is accepted, false otherwise
	 */
	public boolean accepts(Conditional<I> c) {
		return this.getVerificationRank(c) < this.getFalsificationRank(c);
	}

	/**
	 * Determines whether a set of conditionals is accepted by the ranking
	 * function, which is the case if all conditionals are accepted by the
	 * ranking function.
	 * 
	 * @param conditionals
	 *        Set of conditionals
	 * @return true, if all conditionals are accepted, false otherwise
	 */
	public boolean accepts(Collection<Conditional<I>> conditionals) {
		for (Conditional<I> c : conditionals) {
			if (!this.accepts(c))
				return false;
		}
		return true;
	}

	/**
	 * Returns a textual representation for this ranking function, listing every
	 * possible world with its rank.
	 * 
	 * @return Textual representation
	 */
	@Override
	public String toString() {
		StringBuffer output = new StringBuffer();
		for (Interpretation<I> world : this.worlds) {
			if (this.getRank(world) == this.INFINITY) {
				output.append("k" + world + " = inf" + System.lineSeparator());
			} else {
				output.append("k" + world + " = " + this.getRank(world) + System.lineSeparator());
			}
		}
		return output.toString();
	}

	/**
	 * Returns a textual representation for this ranking function, listing every
	 * possible world with its rank, unless it is
	 * {@link RankingFunction#INFINITY}. The worlds are sorted by ranks.
	 * 
	 * @return Textual representation
	 */
	public String toStringSorted() {
		// sort the list of mappings by value
		List<Map.Entry<Interpretation<I>, Integer>> list = new LinkedList<Map.Entry<Interpretation<I>, Integer>>(this.ranking.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Interpretation<I>, Integer>>() {
			@Override
			public int compare(Map.Entry<Interpretation<I>, Integer> o1, Map.Entry<Interpretation<I>, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		// print in ascending order without infinity
		StringBuffer output = new StringBuffer();
		for (Map.Entry<Interpretation<I>, Integer> entry : list) {
			if (entry.getValue() != this.INFINITY) {
				output.append("k" + entry.getKey() + " = " + entry.getValue() + System.lineSeparator());
			}
		}
		return output.toString();
	}

	/**
	 * Returns the rank of the conditional's verification formula, i.e. the rank
	 * of formula AB for conditional (B|A).
	 * 
	 * @param c
	 *        Conditional
	 * @return Rank
	 */
	public int getVerificationRank(Conditional<I> c) {
		return this.getRank(this.getVerificationFormula(c));
	}

	/**
	 * Returns the rank of the conditional's falsification formula, i.e. the
	 * rank of the formula A!B for conditional (B|A).
	 * 
	 * @param c
	 *        Conditional
	 * @return Rank
	 */
	public int getFalsificationRank(Conditional<I> c) {
		return this.getRank(this.getFalsificationFormula(c));
	}

	/**
	 * Returns the set of worlds this ranking function is defined on.
	 * 
	 * @return Possible Worlds
	 */
	public Interpretation<I>[] getWorlds() {
		return this.worlds;
	}

	/**
	 * Checks whether the given conditional is verified in the given world.
	 * 
	 * @param world
	 *        World
	 * @param c
	 *        Conditional
	 * @return true, if the conditional is verified, false otherwise
	 */
	public boolean verifies(Interpretation<I> world, Conditional<I> c) {
		return world.satisfies(this.getVerificationFormula(c));
	}

	/**
	 * Checks whether the given conditional is falsified in the given world.
	 * 
	 * @param world
	 *        World
	 * @param c
	 *        Conditional
	 * @return true, if the conditional is falsified, false otherwise
	 */
	public boolean falsifies(Interpretation<I> world, Conditional<I> c) {
		return world.satisfies(this.getFalsificationFormula(c));
	}

	/**
	 * Returns the formula to test the verification of a conditional, i.e. AB
	 * for (B|A).
	 * 
	 * @param c
	 *        Conditional
	 * @return Falsification formula
	 */
	protected Formula<I> getVerificationFormula(Conditional<I> c) {
		Formula<I> ant = c.getAntecedence();
		// this is necessary because and() on an elemtary conjunction
		// will not create a new object but change the conjunction
		if (ant instanceof ElementaryConjunction<?>) {
			ElementaryConjunction<I> antE = (ElementaryConjunction<I>) ant;
			ant = new ElementaryConjunction<>(new LinkedList<Literal<I>>(antE.getLiterals()));
		}
		return ant.and(c.getConsequence());
	}

	/**
	 * Returns the formula to test the falsification of a conditional, i.e. A!B
	 * for (B|A).
	 * 
	 * @param c
	 *        Conditional
	 * @return Falsification formula
	 */
	protected Formula<I> getFalsificationFormula(Conditional<I> c) {
		Formula<I> ant = c.getAntecedence();
		// this is necessary because and() on an elemtary conjunction
		// will not create a new object but change the conjunction
		if (ant instanceof ElementaryConjunction<?>) {
			ElementaryConjunction<I> antE = (ElementaryConjunction<I>) ant;
			ant = new ElementaryConjunction<>(new LinkedList<Literal<I>>(antE.getLiterals()));
		}
		return ant.and(c.getConsequence().not());
	}

	/**
	 * Checks whether the ranking functions are equal, i.e. if they are defined
	 * on the same set of possible worlds and assign the same ranks to them.
	 * @param other
	 *        Other ranking function
	 * @return true, if they are equal, false otherwise
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof RankingFunction<?>))
			return false;
		else {
			@SuppressWarnings("unchecked")
			RankingFunction<I> otherKappa = (RankingFunction<I>) other;
			if (!this.worlds.equals(otherKappa.getWorlds()))
				return false;
			else {
				for (Interpretation<I> world : this.worlds) {
					if (this.getRank(world) != otherKappa.getRank(world))
						return false;
				}
				return true;
			}
		}
	}

	/**
	 * Returns a hash code for this ranking function.
	 * @return Hash code
	 */
	@Override
	public int hashCode() {
		int hash = 1, i = 1;
		for (Interpretation<I> world : this.worlds) {
			hash += 37 * this.getRank(world) * i;
			i++;
		}
		return hash;
	}

}
