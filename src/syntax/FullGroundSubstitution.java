package syntax;

import edu.cs.ai.log4KR.logical.syntax.Conjunction;
import edu.cs.ai.log4KR.logical.syntax.Disjunction;
import edu.cs.ai.log4KR.logical.syntax.ElementaryConjunction;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.logical.syntax.Implication;
import edu.cs.ai.log4KR.logical.syntax.Literal;
import edu.cs.ai.log4KR.logical.syntax.Negation;
import edu.cs.ai.log4KR.logical.syntax.Tautology;
import edu.cs.ai.log4KR.relational.classicalLogic.grounding.GroundSubstitution;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Variable;

/**
 * Extension of {@link GroundSubstitution}, adding the missing support for
 * formulas of type Tautology.
 * 
 * @author Tobias Falke
 * 
 */
public class FullGroundSubstitution extends GroundSubstitution {

	public FullGroundSubstitution() {
	}

	public FullGroundSubstitution(FullGroundSubstitution g) {
		super(g);
	}

	public Tautology<RelationalAtom> map(Tautology<RelationalAtom> t) {
		return t;
	}

	public void put(Variable v, Constant c) {
		this.mapVarToConst.put(v, c);
	}

	@Override
	public Formula<RelationalAtom> map(Formula<RelationalAtom> f) {

		if (f instanceof RelationalAtom)
			return this.map((RelationalAtom) f);
		else if (f instanceof Literal)
			return this.map((Literal<RelationalAtom>) f);
		else if (f instanceof ElementaryConjunction)
			return this.map((ElementaryConjunction<RelationalAtom>) f);
		else if (f instanceof Negation)
			return this.map((Negation<RelationalAtom>) f);
		else if (f instanceof Conjunction)
			return this.map((Conjunction<RelationalAtom>) f);
		else if (f instanceof Disjunction)
			return this.map((Disjunction<RelationalAtom>) f);
		else if (f instanceof Implication)
			return this.map((Implication<RelationalAtom>) f);
		else if (f instanceof Tautology)
			return this.map((Tautology<RelationalAtom>) f);
		else
			throw new UnsupportedOperationException("Formulas of type " + f.getClass() + " are not supported yet.");

	}
}
