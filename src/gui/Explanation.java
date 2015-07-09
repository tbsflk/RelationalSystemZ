package gui;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class extends {@link DefaultMutableTreeNode} to represent a tree
 * hierarchy of explanations for the evaluation of the acceptance of a
 * conditional. The {@link GUI} renders a tree control for this data structure.
 * 
 * @author Tobias Falke
 * 
 */
public class Explanation extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new explanation tree node below or above the given node.
	 * @param parent
	 *        if true, the new node is the parent, otherwise the given node is
	 *        the parent node
	 * @param node
	 *        Parent or child node
	 * @param text
	 *        Explanation text
	 */
	public Explanation(boolean parent, Explanation node, String text) {
		this.setText(text);
		if (parent) {
			this.add(node);
		} else {
			if (node != null) {
				node.add(this);
			}
		}
	}

	/**
	 * Creates a new explanation tree node below the given parent node.
	 * @param parent
	 *        Parent node
	 * @param text
	 *        Explanation text
	 */
	public Explanation(Explanation parent, String text) {
		this(false, parent, text);
	}

	/**
	 * Creates a new explanation tree node below the given parent node.
	 * @param parent
	 *        Parent node
	 */
	public Explanation(Explanation parent) {
		this(false, parent, "");
	}

	/**
	 * Sets the explanation text.
	 * @param text
	 *        Text
	 */
	public void setText(String text) {
		text = text.replace(Integer.MAX_VALUE + "", "inf");
		this.userObject = text;
	}

	/**
	 * Returns the textual representation for this explanation node.
	 * @return Text
	 */
	@Override
	public String toString() {
		return " " + this.userObject.toString();
	}

	/**
	 * Returns the textual representation of this explanation node and its
	 * subtree.
	 * @return Text for tree
	 */
	public String treeToString() {
		StringBuffer output = new StringBuffer();
		this.toStringIndent(output, 0);
		return output.toString();
	}

	/**
	 * Recursively builds the textual representation for an explanation tree.
	 * @param output
	 *        Textual representation
	 * @param i
	 *        Level
	 */
	private void toStringIndent(StringBuffer output, int i) {
		// indent
		for (int j = 0; j < i; j++) {
			output.append("   ");
		}
		// current nodes text
		output.append(i + ": " + this.userObject + System.lineSeparator());
		// child nodes
		if (!this.isLeaf()) {
			Explanation exp = (Explanation) this.getFirstChild();
			while (exp != null) {
				exp.toStringIndent(output, i + 1);
				exp = (Explanation) exp.getNextSibling();
			}
		}
	}
}
