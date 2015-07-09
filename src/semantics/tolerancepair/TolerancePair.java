package semantics.tolerancepair;

import java.util.Collection;
import java.util.HashSet;

import semantics.ranking.RelationalSystemZ;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * Data structure for a tolerance-pair for a domain and a first-order knowledge
 * base consisting of conditionals and facts. This implementation itself is just
 * a data structure and does not check any of the conditions for a
 * tolerance-pair, this is however implemented in
 * {@link RelationalSystemZ#isTolerancePair(TolerancePair)}. Hence, this class
 * actually represents a partition-pair.
 * 
 * @author Tobias Falke
 * 
 */
public class TolerancePair implements Comparable<TolerancePair> {

	/**
	 * Number of subsets of partitions (m+1)
	 */
	private final int nOfParts;
	/**
	 * Partition of conditionals
	 */
	private final Collection<RelationalConditional>[] condParts;
	/**
	 * Partition of constants
	 */
	private final Collection<Constant>[] constParts;

	/**
	 * Creates a new tolerance-pair structure.
	 * @param nOfParts
	 *        Number of parts
	 */
	@SuppressWarnings("unchecked")
	public TolerancePair(int nOfParts) {
		this.nOfParts = nOfParts;
		this.condParts = new Collection[this.nOfParts];
		this.constParts = new Collection[this.nOfParts];
		for (int i = 0; i < nOfParts; i++) {
			this.condParts[i] = new HashSet<RelationalConditional>();
			this.constParts[i] = new HashSet<Constant>();
		}
	}

	/**
	 * Creates a new tolerance-pair with an additional tuple, initialized with
	 * the two given sets.
	 * @param conds
	 *        Conditionals in new subset
	 * @param cons
	 *        Constants in new subset
	 * @return New tolerance-pair
	 */
	public TolerancePair extend(Collection<RelationalConditional> conds, Collection<Constant> cons) {
		TolerancePair newPair = new TolerancePair(this.nOfParts + 1);
		for (int i = 0; i < this.nOfParts; i++) {
			newPair.setConditionalPart(i, this.condParts[i]);
			newPair.setConstantPart(i, this.constParts[i]);
		}
		newPair.setConditionalPart(this.nOfParts, conds);
		newPair.setConstantPart(this.nOfParts, cons);
		return newPair;
	}

	/**
	 * Creates a copy of this tolerance-pair.
	 * @return New tolerance-pair
	 */
	public TolerancePair copy() {
		TolerancePair newPair = new TolerancePair(this.nOfParts);
		for (int i = 0; i < this.nOfParts; i++) {
			newPair.setConditionalPart(i, new HashSet<RelationalConditional>(this.condParts[i]));
			newPair.setConstantPart(i, new HashSet<Constant>(this.constParts[i]));
		}
		return newPair;
	}

	/**
	 * Returns the number of subsets (m+1).
	 * @return Number of subsets
	 */
	public int getNOfParts() {
		return this.nOfParts;
	}

	/**
	 * Returns the conditionals in a subset.
	 * @param i
	 *        Subset
	 * @return Conditionals
	 */
	public Collection<RelationalConditional> getConditionalPart(int i) {
		return this.condParts[i];
	}

	/**
	 * Sets the set of conditionals for a subset.
	 * @param i
	 *        Subset
	 * @param condPart
	 *        Conditionals
	 */
	public void setConditionalPart(int i, Collection<RelationalConditional> condPart) {
		this.condParts[i] = condPart;
	}

	/**
	 * Returns the constants in a subset.
	 * @param i
	 *        subset
	 * @return Constants
	 */
	public Collection<Constant> getConstantPart(int i) {
		return this.constParts[i];
	}

	/**
	 * Sets the set of constants in a subset.
	 * @param i
	 *        subset
	 * @param constPart
	 *        Constants
	 */
	public void setConstantPart(int i, Collection<Constant> constPart) {
		this.constParts[i] = constPart;
	}

	/**
	 * Returns a textual representation for a tolerance-pair.
	 * @return Textual representation
	 */
	@Override
	public String toString() {
		StringBuffer output = new StringBuffer();
		for (int i = 0; i < this.nOfParts; i++) {
			output.append(i + " --- ");
			output.append(this.condParts[i] + " --- ");
			output.append(this.constParts[i] + System.lineSeparator());
		}
		return output.toString();
	}

	/**
	 * Determines whether two tolerance-pairs are equal, which is the case if
	 * the have the same number of subsets and the same conditionals and
	 * constants in each subset.
	 * @param o
	 *        Other tolerance-pair
	 * @return true, if they are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TolerancePair))
			return false;
		else if (this == o)
			return true;
		else {
			TolerancePair other = (TolerancePair) o;
			if (this.nOfParts != other.getNOfParts())
				return false;
			else {
				for (int i = 0; i < this.nOfParts; i++) {
					if (!this.condParts[i].containsAll(other.getConditionalPart(i)) || !other.getConditionalPart(i).containsAll(this.condParts[i]))
						return false;
					if (!this.constParts[i].containsAll(other.getConstantPart(i)) || !other.getConstantPart(i).containsAll(this.constParts[i]))
						return false;
				}
				return true;
			}
		}
	}

	/**
	 * Returns a hash code for a tolerance-pair, which is the same for all
	 * tolerance-pairs that are determined to be the same as in
	 * {@link TolerancePair#equals(Object)}.
	 * @return Hash code
	 */
	@Override
	public int hashCode() {
		int ret = 1;
		for (int i = 0; i < this.nOfParts; i++) {
			int sum = 0;
			for (RelationalConditional c : this.condParts[i]) {
				sum += c.hashCode();
			}
			for (Constant c : this.constParts[i]) {
				sum += c.hashCode();
			}
			ret += 37 * (i + 1) * sum;
		}
		return ret;
	}

	/**
	 * Compares two tolerance-pairs. A tolerance-pair t1 is smaller than a
	 * tolerance-pair t2 if it has fewer subsets or if they are the same but t1
	 * has more conditionals or more constants in a lower subset than t2. Note
	 * that two pairs being equal in this sense is different from
	 * {@link TolerancePair#equals(Object)}.
	 * @param other
	 *        Other tolerance-pair
	 * @return <0, if t1<t2, 0, if t1=t2, >0, otherwise
	 */
	@Override
	public int compareTo(TolerancePair other) {
		if (this.getNOfParts() - other.getNOfParts() != 0)
			return this.getNOfParts() - other.getNOfParts();
		else {
			for (int i = 0; i < this.nOfParts; i++) {
				int dif = other.getConditionalPart(i).size() - this.getConditionalPart(i).size();
				if (dif != 0)
					return dif;
				dif = other.getConstantPart(i).size() - this.getConstantPart(i).size();
				if (dif != 0)
					return dif;
			}
			return 0;
		}
	}

	/**
	 * Compares two tolerance-pairs as {@link TolerancePair#compareTo}, but
	 * ignores the last subset.
	 * @param partialPair
	 *        Other tolerance-pair
	 * @return <0, if t1<t2, 0, if t1=t2, >0, otherwise
	 */
	public int compareToPartial(TolerancePair partialPair) {
		if (this.getNOfParts() - partialPair.getNOfParts() != 0)
			return this.getNOfParts() - partialPair.getNOfParts();
		else {
			for (int i = 0; i < this.nOfParts - 1; i++) {
				int dif = partialPair.getConditionalPart(i).size() - this.getConditionalPart(i).size();
				if (dif != 0)
					return dif;
				dif = partialPair.getConstantPart(i).size() - this.getConstantPart(i).size();
				if (dif != 0)
					return dif;
			}
			return 0;
		}
	}

}
