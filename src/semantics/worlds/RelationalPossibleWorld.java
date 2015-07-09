package semantics.worlds;

import java.util.Collection;
import java.util.HashMap;

import syntax.ExistentialQuantification;
import syntax.GroundingOperator;
import syntax.UniversalQuantification;
import edu.cs.ai.log4KR.logical.syntax.Atom;
import edu.cs.ai.log4KR.logical.syntax.Conjunction;
import edu.cs.ai.log4KR.logical.syntax.Contradiction;
import edu.cs.ai.log4KR.logical.syntax.Disjunction;
import edu.cs.ai.log4KR.logical.syntax.ElementaryConjunction;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.Implication;
import edu.cs.ai.log4KR.logical.syntax.Literal;
import edu.cs.ai.log4KR.logical.syntax.Negation;
import edu.cs.ai.log4KR.logical.syntax.Tautology;
import edu.cs.ai.log4KR.relational.classicalLogic.semantics.RelationalPossibleWorldMapRepresentation;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;

/**
 * Extension of {@link RelationalPossibleWorldMapRepresentation}, adding support
 * for quantified formulas and a different textual representation.
 * 
 * @author Tobias Falke
 * 
 */
public class RelationalPossibleWorld extends RelationalPossibleWorldMapRepresentation {

	/**
	 * Domain of individuals
	 */
	private final Collection<Constant> domain;
	/**
	 * Grounding operator to instantiate conditionals
	 */
	private final GroundingOperator gop;

	/**
	 * Creates a possible worlds based on a partially created world.
	 * @param partialWorld
	 *        Partial world
	 * @param domain
	 *        Domain
	 */
	public RelationalPossibleWorld(RelationalPossibleWorld partialWorld, Collection<Constant> domain) {
		super(partialWorld);
		this.interpretables = partialWorld.interpretables;
		this.domain = domain;
		this.gop = new GroundingOperator();
	}

	/**
	 * Creates a possible world.
	 * @param interpretation
	 *        Interpretation
	 * @param interpretables
	 *        Interpretables
	 * @param domain
	 *        Domain
	 */
	protected RelationalPossibleWorld(HashMap<RelationalAtom, Integer> interpretation, Collection<RelationalAtom> interpretables, Collection<Constant> domain) {
		super(interpretation, interpretables);
		this.interpretables = interpretables;
		this.domain = domain;
		this.gop = new GroundingOperator();
	}

	/**
	 * Checks whether a formula is satisfied.
	 * @param f
	 *        Formula
	 * @return true, if satisfied, false if not
	 */
	@Override
	public boolean satisfies(Formula<RelationalAtom> f) {

		if (f instanceof Atom)
			return this.satisfies((Atom<RelationalAtom>) f);
		else if (f instanceof Literal)
			return this.satisfies((Literal<RelationalAtom>) f);
		else if (f instanceof ElementaryConjunction)
			return this.satisfies((ElementaryConjunction<RelationalAtom>) f);
		else if (f instanceof Negation)
			return this.satisfies((Negation<RelationalAtom>) f);
		else if (f instanceof Conjunction)
			return this.satisfies((Conjunction<RelationalAtom>) f);
		else if (f instanceof Disjunction)
			return this.satisfies((Disjunction<RelationalAtom>) f);
		else if (f instanceof Implication)
			return this.satisfies((Implication<RelationalAtom>) f);
		else if (f instanceof Tautology)
			return true;
		else if (f instanceof Contradiction)
			return false;
		else if (f instanceof ExistentialQuantification)
			return this.satisfies((ExistentialQuantification) f);
		else if (f instanceof UniversalQuantification)
			return this.satisfies((UniversalQuantification) f);

		throw new UnsupportedOperationException("Formulas of type " + f.getClass() + " are not supported yet.");

	}

	/**
	 * Checks whether an existentially quantified formula is satisfied.
	 * @param f
	 *        Formula
	 * @return true, if satisfied, false otherwise
	 */
	public boolean satisfies(ExistentialQuantification f) {

		for (Constant a : this.domain) {
			Formula<RelationalAtom> fg = this.gop.groundFormula(f.getFormula(), a);
			if (this.satisfies(fg))
				return true;
		}
		return false;

	}

	/**
	 * Checks whether an universally quantified formula is satisfied.
	 * @param f
	 *        Formula
	 * @return true, if satisfied, false otherwise
	 */
	public boolean satisfies(UniversalQuantification f) {

		for (Constant a : this.domain) {
			Formula<RelationalAtom> fg = this.gop.groundFormula(f.getFormula(), a);
			if (!this.satisfies(fg))
				return false;
		}
		return true;

	}

	/**
	 * Returns the set of interpretables, i.e. a set of relational atoms.
	 * @return Interpretables
	 */
	public Collection<RelationalAtom> getInterpretables() {
		return this.interpretables;
	}

	/**
	 * Returns a textual representation for this world.
	 * @return Textual representation
	 */
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder(this.interpretation.keySet().size() * 10 + 10);
		sb.append("(");

		Collection<RelationalAtom> atoms = this.interpretables;

		for (RelationalAtom a : atoms) {
			sb.append(a);
			sb.append("=");
			sb.append(this.interpretation.get(a));
			sb.append(" ");
		}

		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}
}
