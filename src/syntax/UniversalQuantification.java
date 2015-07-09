package syntax;

import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Variable;

/**
 * Data structure for an universally quantified formula.
 * 
 * @author Tobias Falke
 * 
 */
public class UniversalQuantification extends Quantification {

	public UniversalQuantification(Variable v, Formula<RelationalAtom> f) {
		super(v, f);
	}

	@Override
	public String toString() {
		return "(\\forall " + this.v + ": " + this.f + ")";
	}
}
