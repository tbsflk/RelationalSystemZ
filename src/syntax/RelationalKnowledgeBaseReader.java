package syntax;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.cs.ai.log4KR.logical.syntax.Atom;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Predicate;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Variable;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.kbParser.log4KRReader.Log4KRReader;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * This class can read a first-order knowledge base from a textual
 * representation and checks whether it its a restricted first-order knowledge
 * base.
 * 
 * @author Tobias Falke
 * 
 */
public class RelationalKnowledgeBaseReader {

	/**
	 * Log4KR-Reader
	 */
	private Log4KRReader reader;
	/**
	 * First-order knowledge base
	 */
	private RelationalKnowledgeBase kb;

	/**
	 * Returns the parsed first-order knowledge base
	 * @return Knowledge base
	 */
	public RelationalKnowledgeBase getKnowledgeBase() {
		return this.kb;
	}

	/**
	 * Returns the predicates used in the knowledge base
	 * @return Set of predicates
	 */
	public Collection<Predicate> getPredicates() {
		return this.reader.getPredicates();
	}

	/**
	 * Returns the constants of the domain
	 * @return Domain
	 */
	public Collection<Constant> getDomain() {
		return this.reader.getConstants();
	}

	/**
	 * Reads a knowledge base from its textual representation.
	 * @param kbAsString
	 *        Knowledge base as a string
	 * @throws IllegalArgumentException
	 *         Error
	 */
	public void readKnowledgeBase(String kbAsString) throws IllegalArgumentException {

		// parse knowledge base
		this.reader = new Log4KRReader();
		try {
			this.reader.readFromString(kbAsString);
		} catch (Exception e) {
			throw new IllegalArgumentException("Error: " + e.getMessage());
		}

		// read conditionals
		if (this.reader.getKnowledgeBase("Conditionals") == null)
			throw new IllegalArgumentException("Error: No conditionals!");
		Collection<RelationalConditional> conditionals = this.reader.getKnowledgeBase("Conditionals");

		// read facts
		if (this.reader.getKnowledgeBase("Facts") == null)
			throw new IllegalArgumentException("Error: No facts!");
		Collection<RelationalConditional> factsAsConditionals = this.reader.getKnowledgeBase("Facts");
		Collection<Formula<RelationalAtom>> facts = new LinkedList<Formula<RelationalAtom>>();
		for (RelationalConditional c : factsAsConditionals) {
			facts.add(c.getConsequence());
		}

		// check restrictions
		this.kb = new RelationalKnowledgeBase(conditionals, facts);
		try {
			this.checkRestrictions();
		} catch (Exception e) {
			throw new IllegalArgumentException("Error: " + e.getMessage());
		}

	}

	/**
	 * Checks for a loaded knowledge base if it is a restricted first-order
	 * knowledge base.
	 * @throws Exception
	 *         Error
	 */
	private void checkRestrictions() throws IllegalArgumentException {

		// one sort only (=domain)
		if (this.reader.getSorts().size() > 1)
			throw new IllegalArgumentException("Only one sort can be defined");

		// predicates must have arity 0 or 1
		for (Predicate pred : this.reader.getPredicates()) {
			if (pred.getArity() > 1)
				throw new IllegalArgumentException("Predicate " + pred + " must have arity 0 or 1");
		}

		// conditional may only have one variable
		for (RelationalConditional cond : this.kb.getConditionals()) {
			HashSet<Variable> variables = new HashSet<Variable>();
			for (Atom<RelationalAtom> atom : cond.getAntecedence().getAtoms()) {
				RelationalAtom rAtom = (RelationalAtom) atom;
				variables.addAll(rAtom.getVariables());
			}
			for (Atom<RelationalAtom> atom : cond.getConsequence().getAtoms()) {
				RelationalAtom rAtom = (RelationalAtom) atom;
				variables.addAll(rAtom.getVariables());
			}
			if (variables.size() > 1)
				throw new IllegalArgumentException("Conditional " + cond + " has more than one variable");
		}

		// facts must be closed, and as we don't have quantifies, can therefore
		// not have a variable at all
		for (Formula<RelationalAtom> f : this.kb.getFacts()) {
			for (Atom<RelationalAtom> atom : f.getAtoms()) {
				RelationalAtom rAtom = (RelationalAtom) atom;
				if (rAtom.getVariables().size() > 0)
					throw new IllegalArgumentException("Formula " + f + " must be closed");
			}
		}

	}

	/**
	 * Reads a single formula from its textual representation.
	 * @param query
	 *        Formula as a string
	 * @return Formula
	 * @throws IllegalArgumentException
	 *         Error
	 */
	public Formula<RelationalAtom> readFormulaQuery(String query) throws IllegalArgumentException {

		// prepare input
		String[] lines = query.split("\\r?\\n|\\r");
		query = "";
		for (String line : lines) {
			line = line.trim();
			for (char c : line.toCharArray()) {
				if (c == '#') {
					break;
				}
				query += c;
			}
		}
		query = '(' + query + ')';

		if (query == null || query == "")
			throw new IllegalArgumentException("Invalid query");

		// parse
		this.reader.readConditionalsFromString(query);

		// it is stored as the last conditional now
		RelationalConditional queryConditional = null;
		Iterator<RelationalConditional> i = this.reader.getConditionals().iterator();
		while (i.hasNext()) {
			queryConditional = i.next();
		}
		return queryConditional.getConsequence();

	}

	/**
	 * Reads a conditional from its textual representation.
	 * @param query
	 *        Conditional as string
	 * @return Conditional
	 * @throws IllegalArgumentException
	 *         Error
	 */
	public RelationalConditional readConditionalQuery(String query) throws IllegalArgumentException {

		// prepare input
		String[] lines = query.split("\\r?\\n|\\r");
		query = "";
		for (String line : lines) {
			line = line.trim();
			for (char c : line.toCharArray()) {
				if (c == '#') {
					break;
				}
				query += c;
			}
		}

		if (query == null || query == "")
			throw new IllegalArgumentException("Invalid query");

		// parse
		this.reader.readConditionalsFromString(query);

		// it is stored as the last conditional now
		RelationalConditional queryConditional = null;
		Iterator<RelationalConditional> i = this.reader.getConditionals().iterator();
		while (i.hasNext()) {
			queryConditional = i.next();
		}
		return queryConditional;
	}

}
