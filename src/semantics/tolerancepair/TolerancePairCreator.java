package semantics.tolerancepair;

import java.util.Collection;
import java.util.List;

import semantics.worlds.RelationalPossibleWorld;
import syntax.RelationalKnowledgeBase;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;

/**
 * Abstract class modeling an approach to create tolerance-pairs .
 * 
 * @author Tobias Falke
 * 
 */
public abstract class TolerancePairCreator {

	/**
	 * First-order knowledge base
	 */
	protected RelationalKnowledgeBase kb;
	/**
	 * Domain of individuals
	 */
	protected Collection<Constant> domain;
	/**
	 * Possible worlds
	 */
	protected RelationalPossibleWorld[] worlds;

	/**
	 * Tolerance-pairs
	 */
	protected List<TolerancePair> pairs;
	/**
	 * Explanations for tolerance-pairs
	 */
	protected List<StringBuffer> explanations;

	/**
	 * Creates a tolerance-pair creator for a knowledge base and a domain.
	 * @param kb
	 *        First-order knowledge base
	 * @param domain
	 *        Domain of individuals
	 * @param worlds
	 *        Possible worlds
	 */
	public TolerancePairCreator(RelationalKnowledgeBase kb, Collection<Constant> domain, RelationalPossibleWorld[] worlds) {
		this.kb = kb;
		this.domain = domain;
		this.worlds = worlds;
		this.pairs = null;
		this.explanations = null;
	}

	/**
	 * Starts the creation progress. A reference to an object implementing the
	 * listener interface can be passed, which will then receive updates
	 * regarding the progress.
	 * @param listener
	 *        Progress listener
	 */
	public abstract void createPairs(ProgressListener listener);

	/**
	 * Returns the list of created tolerance-pairs.
	 * @return List of tolerance-pairs
	 */
	public List<TolerancePair> getPairs() {
		return this.pairs;
	}

	/**
	 * Returns the list of explanations for each tolerance-pair.
	 * @return List of explanations
	 */
	public List<StringBuffer> getExplanations() {
		return this.explanations;
	}

}
