package semantics.ranking;

import java.util.Collection;
import java.util.LinkedList;

import syntax.GroundingOperator;
import syntax.RelationalKnowledgeBase;
import edu.cs.ai.log4KR.logical.semantics.Interpretation;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.probabilistic.Conditional;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalFact;

/**
 * A relational ranking function extends the idea of a ranking function to the
 * first-order case. It can determine ranks and acceptance for first-order
 * formulas, conditionals and knowledge bases. The current implementation only
 * supports formulas and conditionals with only one free variable.
 * 
 * @author Tobias Falke
 * 
 */
public class RelationalRankingFunction extends RankingFunction<RelationalAtom> {

	/**
	 * Domain of individuals
	 */
	protected final Collection<Constant> domain;
	/**
	 * Grounding operator for conditional instantiation
	 */
	protected final GroundingOperator gop;

	/**
	 * Creates a relational ranking function, initially ranking each world with
	 * rank 0.
	 * @param worlds
	 *        Possible worlds
	 * @param domain
	 *        Domain of individuals
	 */
	public RelationalRankingFunction(Interpretation<RelationalAtom>[] worlds, Collection<Constant> domain) {
		super(worlds);
		this.domain = domain;
		this.gop = new GroundingOperator();
	}

	/**
	 * Returns the rank for an open first-order formula, which is the minimal
	 * rank of all of its possible instantiations, or
	 * {@link RankingFunction#INFINITY}, if there is no such instantiation.
	 * @param f
	 *        Formula
	 * @return Rank
	 */
	@Override
	public int getRank(Formula<RelationalAtom> f) {
		if (this.gop.isGrounded(f))
			return super.getRank(f);
		else {
			int minRank = this.INFINITY;
			for (Constant a : this.domain) {
				Formula<RelationalAtom> fg = this.gop.groundFormula(f, a);
				if (this.gop.isGrounded(fg)) {
					int k = super.getRank(fg);
					if (k < minRank) {
						minRank = k;
					}
				}
			}
			return minRank;
		}
	}

	/**
	 * Returns the rank for an open first-order conditional, which is the
	 * minimal rank of all of its instantiations, or
	 * {@link RankingFunction#INFINITY}, if there is no such instantiation.
	 * @param c
	 *        Conditional
	 * @return Rank
	 */
	@Override
	public int getRank(Conditional<RelationalAtom> c) {
		RelationalConditional rc = (RelationalConditional) c;
		if (this.gop.isGrounded(rc))
			return super.getRank(rc);
		else {
			int minRank = this.INFINITY;
			for (Constant a : this.domain) {
				RelationalConditional cg = this.gop.groundConditional(rc, a);
				if (this.gop.isGrounded(cg)) {
					int k = super.getRank(cg);
					if (k < minRank) {
						minRank = k;
					}
				}
			}
			return minRank;
		}
	}

	/**
	 * Checks whether an open first-order formula F is accepted by the ranking
	 * function, which is the case if the open conditional (F|true) is accepted.
	 * @param f
	 *        Formula
	 * @return true, if accepted, false otherwise
	 */
	@Override
	public boolean accepts(Formula<RelationalAtom> f) {
		if (this.gop.isGrounded(f))
			return super.accepts(f);
		else {
			RelationalConditional c = new RelationalFact(f);
			return this.accepts(c);
		}
	}

	/**
	 * Checks whether an open first-order conditional is accepted by the ranking
	 * function, which is the case if the conditional has at least one
	 * representative and a) the verification of the conditional has a lower
	 * rank than its falsification or b) both have the same rank, but all
	 * instantiations of the falsification formula with the conditionals
	 * representatives have lower ranks than all instantiations of the
	 * verification formula with representatives of the negated conditional.
	 * @param c
	 *        Conditional
	 * @return true, if accepted, false otherwise
	 */
	@Override
	public boolean accepts(Conditional<RelationalAtom> c) {
		RelationalConditional rc = (RelationalConditional) c;
		if (this.gop.isGrounded(rc))
			return super.accepts(rc);
		else {
			// are there representatives?
			Collection<Constant> vRep = this.getRepresentatives(rc);
			if (vRep.size() == 0)
				return false;
			else {
				// Acc-1
				int vRank = super.getVerificationRank(c);
				int fRank = super.getFalsificationRank(c);
				if (vRank < fRank)
					return true;
				else if (vRank == fRank) {
					// Acc-2
					RelationalConditional fc = new RelationalConditional(c.getConsequence().not(), c.getAntecedence());
					Collection<RelationalConditional> fcvRep = this.gop.groundConditional(fc, vRep);
					Collection<Constant> fRep = this.getRepresentatives(fc);
					Collection<RelationalConditional> vcfRep = this.gop.groundConditional(rc, fRep);
					for (RelationalConditional fcv : fcvRep) {
						for (RelationalConditional vcf : vcfRep) {
							if (super.getVerificationRank(fcv) >= super.getVerificationRank(vcf))
								return false;
						}
					}
					return true;
				} else
					return false;
			}
		}
	}

	/**
	 * Checks whether a first-order knowledge base KB=<R,F> is accepted by the
	 * ranking function, which is the case if all worlds not satisfying the set
	 * of facts are ranked with {@link RankingFunction#INFINITY}, and all
	 * conditionals are accepted.
	 * @param kb
	 *        Knowledge base
	 * @return true, if accepted, false otherwise
	 */
	public boolean accepts(RelationalKnowledgeBase kb) {
		// check facts
		for (Interpretation<RelationalAtom> world : this.worlds) {
			boolean satisfiesFacts = true;
			for (Formula<RelationalAtom> f : kb.getFacts()) {
				if (!world.satisfies(f)) {
					satisfiesFacts = false;
					break;
				}
			}
			if (!satisfiesFacts && this.getRank(world) != this.INFINITY)
				return false;
		}
		// checks conditionals
		for (Conditional<RelationalAtom> c : kb.getConditionals()) {
			if (!this.accepts(c))
				return false;
		}
		return true;
	}

	/**
	 * Returns the set of representatives for an open conditional.
	 * @param c
	 *        Conditional
	 * @return Representatives
	 */
	public Collection<Constant> getRepresentatives(RelationalConditional c) {
		Collection<Constant> rep = new LinkedList<Constant>();
		// check all weak representatives
		Collection<Constant> wRep = this.getWeakRepresentatives(c);
		if (wRep.isEmpty() || wRep.size() == 1)
			return wRep;
		int minFRank = this.INFINITY;
		for (Constant a : wRep) {
			RelationalConditional cg = this.gop.groundConditional(c, a);
			int fRank = this.getFalsificationRank(cg);
			// collect those with minimal falsification rank
			if (fRank < minFRank) {
				rep.clear();
				rep.add(a);
				minFRank = fRank;
			} else if (fRank == minFRank) {
				rep.add(a);
			}
		}
		return rep;
	}

	/**
	 * Returns all weak representatives for an open conditional.
	 * @param c
	 *        Conditional
	 * @return Weak representatives
	 */
	public Collection<Constant> getWeakRepresentatives(RelationalConditional c) {

		Collection<Constant> wRep = new LinkedList<Constant>();

		int vRankOpen = this.getRank(this.getVerificationFormula(c));

		for (Constant a : this.domain) {
			RelationalConditional cg = this.gop.groundConditional(c, a);

			// rank for instantiated verification formula
			int vRankGrounded = super.getVerificationRank(cg);
			if (vRankOpen == vRankGrounded) {

				// rank for instantiated falsification formula
				int fRankGrounded = super.getFalsificationRank(cg);
				// check acceptance
				if (vRankGrounded < fRankGrounded) {
					wRep.add(a);
				}
			}
		}
		return wRep;
	}

	/**
	 * Returns a copy of this ranking function.
	 * @return Copy
	 */
	public RelationalRankingFunction copy() {
		RelationalRankingFunction copy = new RelationalRankingFunction(this.worlds, this.domain);
		for (Interpretation<RelationalAtom> world : this.worlds) {
			copy.setRank(world, this.getRank(world));
		}
		return copy;
	}

}
