package syntax;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import edu.cs.ai.log4KR.logical.syntax.Atom;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.grounding.ConstraintBasedGroundingOperator;
import edu.cs.ai.log4KR.relational.classicalLogic.grounding.GroundSubstitution;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Variable;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalFact;

/**
 * Extension of {@link ConstraintBasedGroundingOperator}, providing an
 * unconstrained grounding operator with additional utility methods for simple
 * instantiations.
 * 
 * @author Tobias Falke
 * 
 */
public class GroundingOperator extends ConstraintBasedGroundingOperator {

	/**
	 * Get all ground conditionals for the passed conditional.
	 * @param cond
	 *        Conditional
	 * @param consts
	 *        Constants
	 * @return Groundings
	 */
	@Override
	public Collection<RelationalConditional> groundConditional(RelationalConditional cond, Collection<Constant> consts) {

		HashSet<RelationalConditional> groundedConditionals = new HashSet<RelationalConditional>();

		Collection<Atom<RelationalAtom>> atoms = cond.getAtoms();
		Collection<Variable> variables = new HashSet<Variable>();
		for (Atom<RelationalAtom> atom : atoms) {
			variables.addAll(((RelationalAtom) atom).getVariables());
		}
		// already grounded
		if (variables.isEmpty()) {
			groundedConditionals.add(cond);
			return groundedConditionals;
		}

		Collection<GroundSubstitution> groundSubs = this.createAllAdmissibleGroundSubstitutions(variables, consts);
		for (GroundSubstitution g : groundSubs) {
			groundedConditionals.add(g.map(cond));
		}

		return groundedConditionals;
	}

	/**
	 * Get all possible ground substitutions.
	 * @param vars
	 *        Variables
	 * @param consts
	 *        Constants
	 * @return Substitutions
	 */
	private Collection<GroundSubstitution> createAllAdmissibleGroundSubstitutions(Collection<Variable> vars, Collection<Constant> consts) {
		HashSet<GroundSubstitution> groundSubs = new HashSet<GroundSubstitution>();
		groundSubs.add(new FullGroundSubstitution());
		for (Variable v : vars) {
			HashSet<GroundSubstitution> newGroundSubs = new HashSet<GroundSubstitution>();
			for (Constant c : consts) {
				if (c.getType() == v.getType()) {
					for (GroundSubstitution g : groundSubs) {
						FullGroundSubstitution gNew = new FullGroundSubstitution((FullGroundSubstitution) g);
						gNew.put(v, c);
						newGroundSubs.add(gNew);
					}
				}
			}
			groundSubs = newGroundSubs;
		}
		return groundSubs;
	}

	/**
	 * Ground a formula with a given constant.
	 * @param f
	 *        Formula
	 * @param a
	 *        Constant
	 * @return Grounded constant
	 */
	public Formula<RelationalAtom> groundFormula(Formula<RelationalAtom> f, Constant a) {
		RelationalConditional c = new RelationalFact(f);
		RelationalConditional cgList = this.groundConditional(c, a);
		return cgList.getConsequence();
	}

	/**
	 * Ground a conditional with a given constant.
	 * @param c
	 *        Conditional
	 * @param a
	 *        Constant
	 * @return Grounded conditional
	 */
	public RelationalConditional groundConditional(RelationalConditional c, Constant a) {
		Collection<Constant> aList = new LinkedList<Constant>();
		aList.add(a);
		Collection<RelationalConditional> cgList = this.groundConditional(c, aList);
		return cgList.iterator().next();
	}

	/**
	 * Checks whether a formula is ground, i.e. if it does not have free
	 * variables.
	 * @param f
	 *        Formula
	 * @return true, if grounded, false otherwise
	 */
	public boolean isGrounded(Formula<RelationalAtom> f) {
		Collection<Atom<RelationalAtom>> atoms = f.getAtoms();
		for (Atom<RelationalAtom> atom : atoms) {
			if (!((RelationalAtom) atom).isGround())
				return false;
		}
		return true;
	}

	/**
	 * Checks whether a conditional is ground, i.e. if it does not have free
	 * variables.
	 * @param c
	 *        Conditional
	 * @return true, if grounded, false otherwise
	 */
	public boolean isGrounded(RelationalConditional c) {
		return this.isGrounded(c.getAntecedence()) && this.isGrounded(c.getConsequence());
	}

}
