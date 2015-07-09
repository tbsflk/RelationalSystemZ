package syntax;

import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Variable;

/**
 * Data structure for an existentially quantified formula.
 * 
 * @author Tobias Falke
 * 
 */
public class ExistentialQuantification extends Quantification {

	public ExistentialQuantification(Variable v, Formula<RelationalAtom> f) {
		super(v, f);
	}

	@Override
	public String toString() {
		return "(\\exists " + this.v + ": " + this.f + ")";
	}

}
