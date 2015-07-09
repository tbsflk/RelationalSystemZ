package gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import semantics.ranking.ExplainedRelationalRankingFunction;
import semantics.ranking.RelationalSystemZ;
import semantics.tolerancepair.BruteForceTolerancePairCreator;
import semantics.tolerancepair.ProgressListener;
import semantics.tolerancepair.SearchMinTolerancePairCreator;
import semantics.tolerancepair.SearchTolerancePairCreator;
import semantics.tolerancepair.TolerancePair;
import semantics.tolerancepair.TolerancePairCreator;
import semantics.worlds.RelationalPossibleWorld;
import semantics.worlds.RelationalPossibleWorldFactory;
import syntax.RelationalKnowledgeBase;
import syntax.RelationalKnowledgeBaseReader;
import edu.cs.ai.log4KR.logical.syntax.Formula;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.RelationalAtom;
import edu.cs.ai.log4KR.relational.classicalLogic.syntax.signature.Constant;
import edu.cs.ai.log4KR.relational.probabilisticConditionalLogic.syntax.RelationalConditional;

/**
 * This class stores the data of a user session in the program, performs the
 * offered functions on the data and produces the output.
 * 
 * @author Tobias Falke
 * 
 */
public class Session {

	private static final String NL = System.lineSeparator();

	/**
	 * First-order knowledge base
	 */
	private RelationalKnowledgeBase kb;
	/**
	 * Reader for a first-order knowledge base
	 */
	private RelationalKnowledgeBaseReader reader;
	/**
	 * Tolerance-Pairs
	 */
	private List<TolerancePair> tPairs;
	/**
	 * Explanations for tolerance-pairs
	 */
	private List<StringBuffer> tPairExplanations;
	/**
	 * Currently selected tolerance-pair
	 */
	private int selectedTPair;
	/**
	 * First-order system Z
	 */
	private RelationalSystemZ relSystemZ;
	/**
	 * Ranking function
	 */
	private ExplainedRelationalRankingFunction kappa;
	/**
	 * Possible worlds
	 */
	private RelationalPossibleWorld[] worlds;

	/**
	 * Creates a new session.
	 */
	public Session() {
		this.reader = new RelationalKnowledgeBaseReader();
	}

	/**
	 * Returns the domain for the currently loaded knowledge base.
	 * @return Domain
	 */
	public Collection<Constant> getDomain() {
		return this.reader.getDomain();
	}

	/**
	 * Returns the currently loaded knowledge base.
	 * @return Knowledge base
	 */
	public RelationalKnowledgeBase getKnowledgeBase() {
		return this.kb;
	}

	/**
	 * Returns the created tolerance-pairs.
	 * @return Set of tolerance-pairs
	 */
	public Collection<TolerancePair> getTPairs() {
		return this.tPairs;
	}

	/**
	 * Returns the computed ranking function.
	 * @return Ranking function
	 */
	public ExplainedRelationalRankingFunction getKappa() {
		return this.kappa;
	}

	/**
	 * Returns the set of possible worlds for the currently loaded knowledge
	 * base.
	 * @return Possible worlds
	 */
	public RelationalPossibleWorld[] getWorlds() {
		return this.worlds;
	}

	/**
	 * Returns the set of all possible worlds that satisfy the facts of the
	 * currently loaded knowledge base.
	 * @return Possible worlds
	 */
	public RelationalPossibleWorld[] getWorldsForFacts() {
		Collection<RelationalPossibleWorld> pWorlds = new LinkedList<RelationalPossibleWorld>();
		for (RelationalPossibleWorld w : this.worlds) {
			if (this.kappa.getRank(w) != this.kappa.INFINITY) {
				pWorlds.add(w);
			}
		}
		RelationalPossibleWorld[] array = new RelationalPossibleWorld[pWorlds.size()];
		return pWorlds.toArray(array);
	}

	/**
	 * Loads a knowledge base from its textual representation and creates the
	 * corresponding possible worlds.
	 * @param kbAsString
	 *        Knowledge base as a string
	 * @throws IllegalArgumentException
	 *         Error
	 */
	public void loadKnowledgeBase(String kbAsString) throws IllegalArgumentException {

		try {

			// parse
			this.reader.readKnowledgeBase(kbAsString);
			this.kb = this.reader.getKnowledgeBase();
			this.tPairs = null;
			this.kappa = null;

			// create possible worlds
			Collection<RelationalAtom> atoms = this.kb.getAtomsFromSignature(this.getDomain(), this.reader.getPredicates());
			RelationalPossibleWorldFactory worldFactory = new RelationalPossibleWorldFactory();
			this.worlds = worldFactory.createPossibleWorlds(atoms, this.getDomain());

		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (OutOfMemoryError e) {
			this.kb = null;
			throw new IllegalArgumentException("Error: Heap space cannot store possible worlds." + NL + "Please restart the program with sufficient JVM heap size configured!");
		}

	}

	/**
	 * Returns the output representation for the currently loaded knowledge base
	 * @return Output
	 */
	public String printKnowledgeBase() {

		StringBuffer output = new StringBuffer();
		output.append("Current knowledge base:" + NL + NL);

		output.append("Predicates" + NL);
		if (this.reader != null) {
			output.append(this.reader.getPredicates() + NL + NL);
		} else {
			output.append("-" + NL + NL);
		}
		output.append("Domain" + NL);
		if (this.reader != null) {
			for (Constant constant : this.getDomain()) {
				output.append(constant + ", ");
			}
			output.delete(output.length() - 2, output.length());
			output.append(NL + NL);
		} else {
			output.append("-" + NL + NL);
		}

		output.append("Conditionals" + NL);
		if (this.kb != null) {
			output.append(this.kb.getConditionals() + NL + NL);
		} else {
			output.append("-" + NL + NL);
		}
		output.append("Facts" + NL);
		if (this.kb != null) {
			output.append(this.kb.getFacts() + NL + NL);
		} else {
			output.append("-" + NL + NL);
		}

		output.append("Possible Worlds: " + this.worlds.length);

		return output.toString();

	}

	/**
	 * Creates tolerance-pairs with the selected method. Must be called in a
	 * task that can be aborted.
	 * @param task
	 *        Task
	 * @param method
	 *        Method
	 * @param progressBar
	 *        Progress indicator control
	 */
	public void createTPairs(final SwingWorker<Void, Void> task, int method, final JProgressBar progressBar) {

		this.tPairs = null;
		this.tPairExplanations = null;
		this.kappa = null;

		// initialize corresponding creator
		TolerancePairCreator creator = null;
		switch (method) {
		case 0:
			creator = new BruteForceTolerancePairCreator(this.kb, this.getDomain(), this.worlds);
			break;
		case 1:
			creator = new SearchTolerancePairCreator(this.kb, this.getDomain(), this.worlds);
			break;
		case 2:
			creator = new SearchMinTolerancePairCreator(this.kb, this.getDomain(), this.worlds);
			break;
		default:
			return;
		}

		// start creation
		creator.createPairs(new ProgressListener() {
			@Override
			public boolean progressChanged(double progress) {
				// update progress, if not already canceled
				if (task.isCancelled())
					return false;
				else {
					progressBar.setValue((int) (progress * 100));
					return true;
				}
			}
		});
		this.tPairs = creator.getPairs();
		this.tPairExplanations = creator.getExplanations();
	}

	/**
	 * Returns the output representation for the created tolerance-pairs.
	 * @param explain
	 *        Flag indicating whether proofs should be included
	 * @return Output
	 */
	public String printTPairs(boolean explain) {

		StringBuffer output = new StringBuffer();
		output.append(this.tPairs.size() + " tolerance-pair(s) created" + NL + NL);
		int i = 1;

		// add each t-pair
		for (TolerancePair part : this.tPairs) {

			output.append("Pair " + i + " (m=" + (part.getNOfParts() - 1) + ")" + NL);
			output.append(part.toString() + NL);

			// add proof
			if (explain && this.tPairExplanations != null) {
				output.append("Proof according to Theorem 1 (C2):" + NL + NL);
				StringBuffer explanation = this.tPairExplanations.get(i - 1);
				output.append(explanation + NL);
			}

			i++;
		}

		return output.toString();

	}

	/**
	 * Computes the ranking function for the selected tolerance-pair.
	 * @param i
	 *        Index of selected tolerance-pair
	 */
	public void createKappa(int i) {

		TolerancePair tPair = this.tPairs.get(i);
		this.relSystemZ = new RelationalSystemZ(this.getDomain(), this.kb, tPair, this.worlds);
		this.kappa = this.relSystemZ.getRankingFunction();
		this.selectedTPair = i;

	}

	/**
	 * Returns the output representation of the computed ranking function.
	 * @return Output
	 */
	public String printKappa() {

		StringBuffer output = new StringBuffer();

		TolerancePair tPair = this.tPairs.get(this.selectedTPair);
		output.append("Selected tolerance-pair:" + NL + NL);
		output.append("Pair " + (this.selectedTPair + 1) + " (m=" + (tPair.getNOfParts() - 1) + ")" + NL);
		output.append(tPair + NL);

		output.append("Computed ranking function k:" + NL);
		output.append("(only worlds not mapped to infinity are shown, sorted by assigned values)" + NL + NL);
		output.append(this.kappa.toStringSorted());

		return output.toString();

	}

	/**
	 * Returns the spreadsheet-optimized output representation of the computed
	 * ranking function.
	 * @return Spreadsheet output
	 */
	public String printKappaCSV() {

		StringBuffer output = new StringBuffer();

		// header line
		RelationalPossibleWorld firstWorld = this.worlds[0];
		for (RelationalAtom atom : firstWorld.getInterpretables()) {
			output.append(atom + ";");
		}
		output.append("k" + System.lineSeparator());

		// contents
		for (RelationalPossibleWorld world : this.worlds) {
			for (RelationalAtom atom : world.getInterpretables()) {
				output.append(world.getInterpretation().get(atom) + ";");
			}
			if (this.kappa.getRank(world) == this.kappa.INFINITY) {
				output.append("inf" + System.lineSeparator());
			} else {
				output.append(this.kappa.getRank(world) + System.lineSeparator());
			}
		}

		return output.toString();

	}

	/**
	 * Returns the detailed output about the ranking for a given world.
	 * @param world
	 *        World
	 * @return Output
	 */
	public String explainKappa(RelationalPossibleWorld world) {

		StringBuffer output = new StringBuffer();

		TolerancePair tPair = this.tPairs.get(this.selectedTPair);
		output.append("Selected tolerance-pair:" + NL);
		output.append("Pair " + (this.selectedTPair + 1) + " (m=" + (tPair.getNOfParts() - 1) + ")" + NL);
		output.append(tPair + NL);

		output.append("World:" + NL + "w = " + world + NL + NL);
		output.append("Satisfies facts: " + RelationalSystemZ.satisfiesFacts(world, this.kb.getFacts()) + NL);
		output.append("Ranking: " + this.kappa.getRank(world) + NL + NL);

		if (this.kappa.getRank(world) != this.kappa.INFINITY) {

			int m = tPair.getNOfParts() - 1;
			for (int i = 0; i <= m; i++) {
				output.append("lambda(" + i + ",w) = " + this.relSystemZ.lambda(i, world) + NL);
			}

			output.append(NL + "k_0: " + this.relSystemZ.getKappaZero() + NL + NL);

			output.append("k(w) = ");
			for (int i = 0; i <= m; i++) {
				int f = (int) Math.pow(m + 2, i);
				output.append(f + " * " + this.relSystemZ.lambda(i, world) + " + ");
			}
			output.delete(output.length() - 3, output.length());
			output.append(" - " + this.relSystemZ.getKappaZero() + " = " + this.kappa.getRank(world));
		}

		return output.toString();

	}

	/**
	 * Evaluates a query against the computed ranking function.
	 * @param query
	 *        Query
	 * @param exp
	 *        Root object of explanation tree
	 * @return true, if it is accepted, false otherwise
	 * @throws Exception
	 *         Error
	 */
	public boolean askQuery(String query, Explanation exp) throws Exception {

		// let the reader parse the input
		boolean isFormula = !query.matches("\\s*\\(.*");
		Formula<RelationalAtom> queryFormula = null;
		RelationalConditional queryConditional = null;

		if (isFormula) {
			queryFormula = this.reader.readFormulaQuery(query);
		} else {
			queryConditional = this.reader.readConditionalQuery(query);
		}

		// evaluate the query
		boolean accepted = false;
		if (isFormula) {
			accepted = this.kappa.accepts(queryFormula, exp);
		} else {
			accepted = this.kappa.accepts(queryConditional, exp);
		}
		return accepted;

	}

	/**
	 * Returns a template for the textual representation of a first-order
	 * knowledge base.
	 * @return Template
	 */
	public String getTemplate() {

		StringBuffer template = new StringBuffer();
		template.append("signature" + NL + NL);
		template.append("D={p,t}" + NL + "B(D)" + NL + "P(D)" + NL + "F(D)" + NL + NL);
		template.append("conditionals" + NL + NL);
		template.append("Conditionals{" + NL + "(F(X) | B(X))" + NL + "(!F(X) | P(X))" + NL + "}" + NL + NL);
		template.append("Facts{" + NL + "(B(p))" + NL + "(P(t))" + NL + "}");
		return template.toString();

	}

}