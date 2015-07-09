package syntax;

import java.util.Collection;

import edu.cs.ai.log4KR.logical.syntax.Atom;
import edu.cs.ai.log4KR.logical.syntax.Conjunction;
import edu.cs.ai.log4KR.logical.syntax.Disjunction;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.Implication;
import edu.cs.ai.log4KR.logical.syntax.Negation;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Variable;

/**
 * Data structure for a quantified formula.
 * 
 * @author Tobias Falke
 * 
 */
public abstract class Quantification implements Formula<RelationalAtom> {

	protected Formula<RelationalAtom> f;
	protected Variable v;

	public Quantification(Variable v, Formula<RelationalAtom> f) {
		this.f = f;
		this.v = v;
	}

	public Formula<RelationalAtom> getFormula() {
		return this.f;
	}

	public Variable getVariable() {
		return this.v;
	}

	@Override
	public Formula<RelationalAtom> not() {
		return new Negation<RelationalAtom>(this);
	}

	@Override
	public Formula<RelationalAtom> and(Formula<RelationalAtom> f) {
		return new Conjunction<RelationalAtom>(this, f);
	}

	@Override
	public Formula<RelationalAtom> or(Formula<RelationalAtom> f) {
		return new Disjunction<RelationalAtom>(this, f);
	}

	@Override
	public Formula<RelationalAtom> implies(Formula<RelationalAtom> f) {
		return new Implication<RelationalAtom>(this, f);
	}

	@Override
	public Collection<Atom<RelationalAtom>> getAtoms() {
		return this.f.getAtoms();
	}
}
