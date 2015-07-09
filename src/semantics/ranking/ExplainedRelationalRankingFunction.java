package semantics.ranking;

import edu.cs.ai.log4KR.logical.semantics.Interpretation;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.probabilistic.Conditional;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalFact;
import gui.Explanation;

import java.util.Collection;
import java.util.LinkedList;

import syntax.GroundingOperator;
import syntax.RelationalKnowledgeBase;

/**
 * Extended version of {@link RelationalRankingFunction} that returns
 * explanations for each evaluation. An evaluation tree is built with instances
 * of {@link Explanation}.
 * 
 * @author Tobias Falke
 * 
 */
public class ExplainedRelationalRankingFunction extends ExplainedRankingFunction<RelationalAtom> {

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
	public ExplainedRelationalRankingFunction(Interpretation<RelationalAtom>[] worlds, Collection<Constant> domain) {
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
	 * @param exp
	 *        Explanation
	 * @return Rank
	 */
	@Override
	public int getRank(Formula<RelationalAtom> f, Explanation exp) {
		if (this.gop.isGrounded(f))
			return super.getRank(f, exp);
		else {
			int minRank = this.INFINITY;
			for (Constant a : this.domain) {
				Formula<RelationalAtom> fg = this.gop.groundFormula(f, a);
				if (this.gop.isGrounded(fg)) {
					int k = super.getRank(fg, new Explanation(exp));
					if (k < minRank) {
						minRank = k;
					}
				}
			}
			if (exp != null) {
				exp.setText("k( " + f + " ) = " + minRank + " (Def. 2)");
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
	 * @param exp
	 *        Explanation
	 * @return Rank
	 */
	@Override
	public int getRank(Conditional<RelationalAtom> c, Explanation exp) {
		RelationalConditional cc = (RelationalConditional) c;
		if (this.gop.isGrounded(cc))
			return super.getRank(cc, exp);
		else {
			int minRank = this.INFINITY;
			for (Constant a : this.domain) {
				RelationalConditional cg = this.gop.groundConditional(cc, a);
				if (this.gop.isGrounded(cg)) {
					int k = super.getRank(cg, new Explanation(exp));
					if (k < minRank) {
						minRank = k;
					}
				}
			}
			if (exp != null) {
				exp.setText("k" + c + " = " + minRank);
			}
			return minRank;
		}
	}

	/**
	 * Checks whether an open first-order formula F is accepted by the ranking
	 * function, which is the case if the open conditional (F|true) is accepted.
	 * @param f
	 *        Formula
	 * @param exp
	 *        Explanation
	 * @return true, if accepted, false otherwise
	 */
	@Override
	public boolean accepts(Formula<RelationalAtom> f, Explanation exp) {
		if (this.gop.isGrounded(f))
			return super.accepts(f, exp);
		else {
			RelationalConditional c = new RelationalFact(f);
			boolean accepted = this.accepts(c, new Explanation(exp));
			if (accepted) {
				if (exp != null) {
					exp.setText("k |= " + f + " -> true");
				}
				return true;
			} else {
				if (exp != null) {
					exp.setText("k |= " + f + " -> false");
				}
				return false;
			}
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
	 * @param exp
	 *        Explanation
	 * @return true, if accepted, false otherwise
	 */
	@Override
	public boolean accepts(Conditional<RelationalAtom> c, Explanation exp) {
		RelationalConditional cc = (RelationalConditional) c;
		if (this.gop.isGrounded(cc))
			return super.accepts(cc, exp);
		else {
			// are there representatives?
			Collection<Constant> vRep = this.getRepresentatives(cc, new Explanation(exp));
			if (vRep.size() == 0) {
				if (exp != null) {
					new Explanation(exp, "Rep" + cc + " = [] -> false (Def. 5)");
					exp.setText("k |= " + cc + " -> false");
				}
				return false;
			} else {
				// Acc-1
				Explanation expAcc;
				expAcc = new Explanation(exp, "Acc-1");
				int vRank = super.getVerificationRank(c, new Explanation(expAcc));
				int fRank = super.getFalsificationRank(c, new Explanation(expAcc));
				if (vRank < fRank) {
					if (exp != null) {
						new Explanation(expAcc, vRank + " < " + fRank + " (Def. 5, Acc-1)");
						expAcc.setText("Acc-1 -> true");
						exp.setText("k |= " + cc + " -> true");
					}
					return true;
				} else if (vRank == fRank) {
					if (exp != null) {
						new Explanation(expAcc, "! " + vRank + " < " + fRank + " (Def. 5, Acc-1)");
						expAcc.setText("Acc-1 -> false");
						expAcc = new Explanation(exp, "Acc-2");
						new Explanation(expAcc, vRank + " = " + fRank + " (Def. 5, Acc-1)");
					}
					// Acc-2
					RelationalConditional fc = new RelationalConditional(c.getConsequence().not(), c.getAntecedence());
					Collection<RelationalConditional> fcvRep = this.gop.groundConditional(fc, vRep);
					Collection<Constant> fRep = this.getRepresentatives(fc, new Explanation(expAcc));
					Collection<RelationalConditional> vcfRep = this.gop.groundConditional(cc, fRep);
					for (RelationalConditional fcv : fcvRep) {
						for (RelationalConditional vcf : vcfRep) {
							Explanation exp55 = new Explanation(expAcc);
							int fcvRank = super.getVerificationRank(fcv, new Explanation(exp55));
							int vcfRank = super.getVerificationRank(vcf, new Explanation(exp55));
							if (fcvRank >= vcfRank) {
								if (exp != null) {
									exp55.setText("! k" + fcv + "=" + fcvRank + " < k" + vcf + "=" + vcfRank + " (Def. 5, Acc-2)");
									expAcc.setText("Acc-2 -> false");
									exp.setText("k |= " + cc + " -> false");
								}
								return false;
							} else {
								if (exp != null) {
									exp55.setText("k" + fcv + "=" + fcvRank + " < k" + vcf + "=" + vcfRank + " (Def. 5, Acc-2)");
								}
							}
						}
					}
					if (exp != null) {
						expAcc.setText("Acc-2 -> true");
						exp.setText("k |= " + cc + " -> true");
					}
					return true;
				} else {
					if (exp != null) {
						new Explanation(exp, vRank + " > " + fRank + " -> impossible!!! (Def. 5)");
					}
					return false;
				}
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
			if (!this.accepts(c, null))
				return false;
		}
		return true;
	}

	/**
	 * Returns the set of representatives for an open conditional.
	 * @param c
	 *        Conditional
	 * @param exp
	 *        Explanation
	 * @return Representatives
	 */
	public Collection<Constant> getRepresentatives(RelationalConditional c, Explanation exp) {

		Collection<Constant> rep = new LinkedList<Constant>();

		// check all weak representatives
		Collection<Constant> wRep = this.getWeakRepresentatives(c, new Explanation(exp));
		if (wRep.isEmpty() || wRep.size() == 1) {
			rep = wRep;
			if (exp != null) {
				exp.setText("Rep" + c + " = " + rep);
			}
			return rep;
		}

		Explanation expMin = new Explanation(exp);
		int minFRank = this.INFINITY;
		for (Constant a : wRep) {
			RelationalConditional cg = this.gop.groundConditional(c, a);
			int fRank = this.getFalsificationRank(cg, new Explanation(expMin));
			// collect those with minimal falsification rank
			if (fRank < minFRank) {
				rep.clear();
				rep.add(a);
				minFRank = fRank;
			} else if (fRank == minFRank) {
				rep.add(a);
			}
		}
		if (exp != null) {
			expMin.setText("min(WRep) = " + rep + " (Def. 4 (3))");
			exp.setText("Rep" + c + " = " + rep);
		}
		return rep;
	}

	/**
	 * Returns all weak representatives for an open conditional.
	 * @param c
	 *        Conditional
	 * @param exp
	 *        Explanation
	 * @return Weak representatives
	 */
	public Collection<Constant> getWeakRepresentatives(RelationalConditional c, Explanation exp) {

		Collection<Constant> wRep = new LinkedList<Constant>();
		if (exp != null) {
			exp.setText("WRep" + c + " = " + wRep);
		}

		// rank for open verification formula
		int vRankOpen = this.getRank(this.getVerificationFormula(c), new Explanation(exp));

		for (Constant a : this.domain) {
			Explanation expA = new Explanation(exp);
			RelationalConditional cg = this.gop.groundConditional(c, a);
			// rank for instantiated verification formula
			int vRankGrounded = super.getVerificationRank(cg, new Explanation(expA));
			if (vRankOpen != vRankGrounded) {
				if (exp != null) {
					new Explanation(expA, "! " + vRankOpen + " = " + vRankGrounded + " (Def. 4 (1))");
					expA.setText(a + " -> false");
				}
			} else {
				if (exp != null) {
					new Explanation(expA, vRankOpen + " = " + vRankGrounded + " (Def. 4 (1))");
				}
				// rank for instantiated falsification formula
				int fRankGrounded = super.getFalsificationRank(cg, new Explanation(expA));
				if (vRankGrounded >= fRankGrounded) {
					if (exp != null) {
						new Explanation(expA, "! " + vRankGrounded + " < " + fRankGrounded + " (Def. 4 (2))");
						expA.setText(a + " -> false");
					}
				} else {
					if (exp != null) {
						new Explanation(expA, vRankGrounded + " < " + fRankGrounded + " (Def. 4 (2))");
						expA.setText(a + " -> true");
					}
					wRep.add(a);
				}
			}
		}

		if (exp != null) {
			exp.setText("WRep" + c + " = " + wRep);
		}
		return wRep;
	}
}
