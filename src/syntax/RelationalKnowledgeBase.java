package syntax;

import java.util.Collection;
import java.util.LinkedList;

import edu.cs.ai.log4KR.logical.syntax.Atom;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Predicate;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * A first-order knowledge base consists of a set of conditionals and a set of
 * closed formulas, called facts. The restrictions required for the first-order
 * system Z are check by {@link RelationalKnowledgeBaseReader}.
 * 
 * @author Tobias Falke
 * 
 */
public class RelationalKnowledgeBase {

	/**
	 * Conditionals
	 */
	private final Collection<RelationalConditional> conds;
	/**
	 * Facts
	 */
	private final Collection<Formula<RelationalAtom>> facts;

	/**
	 * Creates a new first-order knowledge base with conditionals and facts.
	 * @param conds
	 *        Conditionals
	 * @param facts
	 *        Facts
	 */
	public RelationalKnowledgeBase(Collection<RelationalConditional> conds, Collection<Formula<RelationalAtom>> facts) {
		this.conds = conds;
		this.facts = facts;
	}

	/**
	 * Returns the set of conditionals.
	 * @return Conditionals
	 */
	public Collection<RelationalConditional> getConditionals() {
		return this.conds;
	}

	/**
	 * Returns the set of facts.
	 * @return Facts
	 */
	public Collection<Formula<RelationalAtom>> getFacts() {
		return this.facts;
	}

	/**
	 * Returns all atoms for this knowledge base and the given domain.
	 * @param domain
	 *        Domain
	 * @param gop
	 *        Grounding operator
	 * @return Atoms
	 */
	public Collection<RelationalAtom> getAtomsFromKnowledgeBase(Collection<Constant> domain, GroundingOperator gop) {
		LinkedList<RelationalAtom> atoms = new LinkedList<RelationalAtom>();
		for (RelationalConditional c : this.conds) {
			for (Atom<RelationalAtom> a : c.getAtoms()) {
				if (!atoms.contains(a)) {
					atoms.add((RelationalAtom) a);
				}
			}
		}
		for (Formula<RelationalAtom> f : this.facts) {
			for (Atom<RelationalAtom> a : f.getAtoms()) {
				if (!atoms.contains(a)) {
					atoms.add((RelationalAtom) a);
				}
			}
		}
		return gop.groundAtoms(atoms, domain, null);
	}

	/**
	 * Returns all atoms for the given predicates and domain
	 * @param domain
	 *        Domain
	 * @param predicates
	 *        Predicates
	 * @return Atoms
	 */
	public Collection<RelationalAtom> getAtomsFromSignature(Collection<Constant> domain, Collection<Predicate> predicates) {
		LinkedList<RelationalAtom> atoms = new LinkedList<RelationalAtom>();
		for (Predicate pred : predicates) {
			if (pred.getArity() == 1) {
				for (Constant constant : domain) {
					RelationalAtom atom = new RelationalAtom(pred, constant);
					atoms.add(atom);
				}
			} else if (pred.getArity() == 0) {
				RelationalAtom atom = new RelationalAtom(pred);
				atoms.add(atom);
			}
		}
		return atoms;
	}
}