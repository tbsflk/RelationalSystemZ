package gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

import semantics.tolerancepair.TolerancePair;
import semantics.worlds.RelationalPossibleWorld;

/**
 * This class creates the user interfaces and handles all user interaction with
 * the GUI.
 * 
 * @author Tobias Falke
 * 
 */
public class GUI extends JFrame {

	private static final long serialVersionUID = 1L;

	// UI elements
	private JPanel contentPane;
	private JComboBox<String> comboAlgorithm;
	private JComboBox<String> comboTPairs;
	private JTextArea inputArea;
	private JTextArea outputArea;
	private JProgressBar progressBar;
	private JButton btnLoadKB;
	private JButton btnShowKB;
	private JButton btnCreateTPair;
	private JButton btnShowTPair;
	private JButton btnCreateKappa;
	private JButton btnShowKappa;
	private JButton btnShowKappaCsv;
	private JButton btnExplainKappa;
	private JButton btnAskQuery;
	private JFileChooser fileChooser;
	private ByteArrayOutputStream errorStream;
	private JCheckBox explainQuery;
	private JCheckBox explainTPair;
	private JPanel outputPanel;
	private final Font font = new Font("Sans Serif", Font.BOLD, 11);

	/**
	 * Session
	 */
	private Session session;
	/**
	 * Tolerance-pair creation task (runs asynchronously)
	 */
	private SwingWorker<Void, Void> tPairTask;

	/**
	 * Creates a new GUI instance.
	 */
	public GUI() {
		this.session = new Session();
		this.createFrame();
		this.setButtons();
		this.fileChooser = new JFileChooser();
		// set error stream to receive error messages (Log4KR sometimes just
		// prints to the error stream instead of using exceptions)
		this.errorStream = new ByteArrayOutputStream();
		System.setErr(new PrintStream(this.errorStream));
	}

	/*
	 * ******************************************************************
	 * Methods called for function buttons
	 * ******************************************************************
	 */

	/**
	 * Loads the knowledge base given in the input area.
	 */
	private void loadKB() {

		// get input
		String input = this.inputArea.getText();
		if (input == null || input.equals(""))
			return;

		// adapt UI
		this.outputArea.setText("Loading knowledge base");
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// call parser
		try {
			this.session.loadKnowledgeBase(input);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		// handle error or success
		if (this.errorStream.size() > 0) {
			this.setOutput(this.errorStream.toString());
			this.errorStream.reset();
		} else {
			this.setOutput(this.session.printKnowledgeBase());
			this.comboTPairs.removeAllItems();
		}

		this.setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * Writes the currently loaded knowledge base to the output area.
	 */
	private void showKB() {
		this.setOutput(this.session.printKnowledgeBase());
	}

	/**
	 * Starts the creation of tolerance-pairs in a separate background task or
	 * cancels the running task.
	 */
	private void createTPairs() {
		this.comboTPairs.removeAllItems();

		// if already running -> cancel
		if (this.tPairTask != null) {

			this.tPairTask.cancel(false); // calls done() directly

		} else {
			// otherwise -> start

			// adapt UI
			this.outputArea.setText("Creating tolerance-pairs");
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			this.btnCreateTPair.setText("Cancel");

			// create background task
			try {
				this.tPairTask = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						// get selected method
						int i = GUI.this.comboAlgorithm.getSelectedIndex();
						GUI.this.session.createTPairs(this, i, GUI.this.progressBar);
						return null;
					}

					@Override
					public void done() {
						GUI.this.createTPairsDone(this.isCancelled());
					}
				};
				this.tPairTask.execute(); // start
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	/**
	 * Adapts the UI if the creation of tolerance-pairs is finished to show the
	 * result.
	 * @param canceled
	 *        Task was canceled
	 */
	private void createTPairsDone(boolean canceled) {
		this.tPairTask = null;

		// canceled
		if (canceled) {

			this.setOutput("Creation cancelled");

		} else {
			// finished
			// with error
			if (this.errorStream.size() > 0) {
				this.outputArea.setText(this.errorStream.toString());
				this.errorStream.reset();
			} else {
				// with success
				boolean explain = this.explainTPair.isSelected();
				this.setOutput(this.session.printTPairs(explain));
				this.comboTPairs.removeAllItems();
				int i = 1;
				for (TolerancePair part : this.session.getTPairs()) {
					this.comboTPairs.addItem("Pair " + i + " (m=" + (part.getNOfParts() - 1) + ")");
					i++;
				}
			}
		}

		// adapt UI
		this.setCursor(Cursor.getDefaultCursor());
		this.progressBar.setValue(0);
		this.btnCreateTPair.setText("Create");
		this.setButtons();
	}

	/**
	 * Writes the created tolerance-pairs to the output area.
	 */
	private void showTPairs() {
		boolean explain = GUI.this.explainTPair.isSelected();
		this.setOutput(this.session.printTPairs(explain));
	}

	/**
	 * Computes the ranking function for the selected tolerance-pair.
	 */
	private void createKappa() {

		this.outputArea.setText("Computing ranking function");
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		this.session.createKappa(this.comboTPairs.getSelectedIndex());

		this.setOutput(this.session.printKappa());
		this.setCursor(Cursor.getDefaultCursor());

	}

	/**
	 * Opens a dialog to select a world and writes the details for this world to
	 * the output area.
	 */
	private void explainKappa() {
		RelationalPossibleWorld world = (RelationalPossibleWorld) JOptionPane.showInputDialog(this, "Please select a world:", "World", JOptionPane.QUESTION_MESSAGE, null, this.session.getWorldsForFacts(), null);
		this.setOutput(this.session.explainKappa(world));
	}

	/**
	 * Writes the computed ranking function to the output area.
	 */
	private void showKappa() {
		this.setOutput(this.session.printKappa());
	}

	/**
	 * Writes the computed ranking function spreadsheet-optimized to the output
	 * area.
	 */
	private void showKappaCSV() {
		this.setOutput(this.session.printKappaCSV());
	}

	/**
	 * Reads a query from the input area, evaluates it and writes the result to
	 * the output area.
	 */
	private void askQuery() {

		// get input
		String input = this.inputArea.getText();
		if (input == null || input.equals(""))
			return;

		// adapt UI
		this.outputArea.setText("Evaluating query");
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// evaluate and get result and explanation
		boolean result = false;
		Explanation exp = new Explanation(null, "");
		try {
			result = this.session.askQuery(input, exp);
			exp = new Explanation(true, exp, "Acceptance by ranking function k");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		// handle error or success
		if (this.errorStream.size() > 0) {
			this.setOutput(this.errorStream.toString());
			this.errorStream.reset();
		} else {
			if (this.explainQuery.isSelected()) {
				this.switchOutput(true, exp);
				this.outputArea.setText(exp.treeToString());
			} else {
				this.setOutput("Acceptance by ranking function: " + result);
			}
		}

		// adapt UI
		this.outputArea.setCaretPosition(0);
		this.setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * Writes a template for a knowledge base to the input area.
	 */
	protected void createKBTemplate() {
		this.inputArea.setText(this.session.getTemplate());
		this.inputArea.setCaretPosition(0);
	}

	/*
	 * ******************************************************************
	 * Utility methods for functionality
	 * ******************************************************************
	 */

	/**
	 * Reads the content of a file and writes it to a text area.
	 * @param textArea
	 *        Text area
	 */
	private void openFile(JTextArea textArea) {
		int response = this.fileChooser.showOpenDialog(this);
		if (response == JFileChooser.APPROVE_OPTION) {
			try {
				File file = this.fileChooser.getSelectedFile();
				BufferedReader reader = new BufferedReader(new FileReader(file));
				StringBuffer content = new StringBuffer();
				String line = null;
				while ((line = reader.readLine()) != null) {
					content.append(line + System.lineSeparator());
				}
				reader.close();
				textArea.setText(content.toString());
				textArea.setCaretPosition(0);
			} catch (Exception e) {
				textArea.setText(e.getStackTrace().toString());
				textArea.setCaretPosition(0);
			}
		}
	}

	/**
	 * Saves the content of a text area to a file.
	 * @param textArea
	 *        Text area
	 */
	private void saveFile(JTextArea textArea) {
		int response = this.fileChooser.showSaveDialog(this);
		if (response == JFileChooser.APPROVE_OPTION) {
			try {
				File file = this.fileChooser.getSelectedFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				writer.write(textArea.getText());
				writer.close();
				JOptionPane.showMessageDialog(this, "File successfully saved!");
			} catch (Exception e) {
				textArea.setText(e.getStackTrace().toString());
			}
		}
	}

	/**
	 * Writes text to the output area and scrolls to the top afterwards.
	 * @param text
	 *        Text
	 */
	private void setOutput(String text) {
		this.switchOutput(false, null);
		this.outputArea.setText(text);
		this.outputArea.setCaretPosition(0);
	}

	/**
	 * Switches between the text output area and the tree control used to show
	 * detailed query evaluations.
	 * @param isTree
	 *        Flag indicating that the tree control should be displayed
	 * @param exp
	 *        Tree to be shown in tree control
	 */
	private void switchOutput(boolean isTree, Explanation exp) {
		// remove current control
		this.outputPanel.remove(((BorderLayout) this.outputPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER));
		JScrollPane scrollPaneOutput = null;
		if (isTree) {
			// create tree control
			JTree tree = new JTree(exp);
			DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
			renderer.setLeafIcon(null);
			renderer.setClosedIcon(null);
			renderer.setOpenIcon(null);
			tree.setFont(tree.getFont().deriveFont(12f));
			scrollPaneOutput = new JScrollPane(tree);
		} else {
			// create pane with output area
			scrollPaneOutput = new JScrollPane(this.outputArea);
		}
		// set new control
		scrollPaneOutput.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneOutput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.outputPanel.add(scrollPaneOutput, BorderLayout.CENTER);
		this.outputPanel.revalidate();
	}

	/**
	 * Updates the availability of function buttons based on the session status.
	 */
	private void setButtons() {
		if (this.session.getKnowledgeBase() == null) {
			this.btnShowKB.setEnabled(false);
			this.btnCreateTPair.setEnabled(false);
		} else {
			this.btnShowKB.setEnabled(true);
			this.btnCreateTPair.setEnabled(true);
		}
		if (this.session.getTPairs() == null || this.session.getTPairs().size() == 0) {
			this.btnShowTPair.setEnabled(false);
			this.btnCreateKappa.setEnabled(false);
		} else {
			this.btnShowTPair.setEnabled(true);
			this.btnCreateKappa.setEnabled(true);
		}
		if (this.session.getKappa() == null) {
			this.btnShowKappa.setEnabled(false);
			this.btnExplainKappa.setEnabled(false);
			this.btnShowKappaCsv.setEnabled(false);
			this.btnAskQuery.setEnabled(false);
		} else {
			this.btnShowKappa.setEnabled(true);
			this.btnExplainKappa.setEnabled(true);
			this.btnShowKappaCsv.setEnabled(true);
			this.btnAskQuery.setEnabled(true);
		}
	}

	/*
	 * ******************************************************************
	 * GUI creation
	 * ******************************************************************
	 */

	/**
	 * Creates the parent frame with all UI elements
	 */
	private void createFrame() {

		// main frame properties
		this.setTitle("Relational System Z Reasoner");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBounds(100, 100, 880, 550);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(this.contentPane);
		this.contentPane.setLayout(new BorderLayout(0, 0));

		// function tool bar
		JPanel functionPanel = new JPanel();
		functionPanel.setPreferredSize(new Dimension(750, 140));
		functionPanel.setLayout(null);
		this.contentPane.add(functionPanel, BorderLayout.NORTH);

		// step 1
		JLabel titleKB = new JLabel("1. Load Knowledge Base");
		titleKB.setFont(this.font);
		titleKB.setBounds(10, 11, 175, 14);
		functionPanel.add(titleKB);

		// step 2
		JLabel titlePartitioning = new JLabel("2. Create Tolerance-Pairs");
		titlePartitioning.setFont(this.font);
		titlePartitioning.setBounds(215, 11, 175, 14);
		functionPanel.add(titlePartitioning);

		// step 3
		JLabel titleKappa = new JLabel("3. Compute Ranking Function");
		titleKappa.setFont(this.font);
		titleKappa.setBounds(420, 11, 193, 14);
		functionPanel.add(titleKappa);

		// step 4
		JLabel titleQuery = new JLabel("4. Ask Query");
		titleQuery.setFont(this.font);
		titleQuery.setBounds(625, 11, 175, 14);
		functionPanel.add(titleQuery);

		// dropdown tolerance-pair creation method
		this.comboAlgorithm = new JComboBox<String>();
		this.comboAlgorithm.setBounds(215, 35, 175, 20);
		this.comboAlgorithm.setFont(this.font);
		this.comboAlgorithm.setModel(new DefaultComboBoxModel<String>(new String[] { "All via Brute Force", "All via Search", "Minimal via Search" }));
		functionPanel.add(this.comboAlgorithm);

		// dropdown for created tolerance-pairs
		this.comboTPairs = new JComboBox<String>();
		this.comboTPairs.setFont(this.font);
		this.comboTPairs.setBounds(420, 35, 180, 20);
		functionPanel.add(this.comboTPairs);

		// explain checkbox for step 4
		this.explainQuery = new JCheckBox("Explain Result");
		this.explainQuery.setBounds(625, 64, 120, 23);
		this.explainQuery.setFont(this.font);
		functionPanel.add(this.explainQuery);

		// explain checkbox for step 2
		this.explainTPair = new JCheckBox("Detailed Output");
		this.explainTPair.setBounds(215, 64, 175, 23);
		this.explainTPair.setFont(this.font);
		functionPanel.add(this.explainTPair);

		// step 1 buttons

		// template
		JButton btnTemplate = new JButton("Create Template");
		btnTemplate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.createKBTemplate();
					GUI.this.setButtons();
				}
			}
		});
		btnTemplate.setBounds(10, 64, 175, 23);
		btnTemplate.setFont(this.font);
		functionPanel.add(btnTemplate);

		// load
		this.btnLoadKB = new JButton("Load");
		this.btnLoadKB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.loadKB();
					GUI.this.setButtons();
				}
			}
		});
		this.btnLoadKB.setBounds(10, 96, 83, 23);
		this.btnLoadKB.setFont(this.font);
		functionPanel.add(this.btnLoadKB);

		// show
		this.btnShowKB = new JButton("Show");
		this.btnShowKB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.showKB();
					GUI.this.setButtons();
				}
			}
		});
		this.btnShowKB.setBounds(103, 96, 83, 23);
		this.btnShowKB.setFont(this.font);
		functionPanel.add(this.btnShowKB);

		// step 2 buttons

		// create
		this.btnCreateTPair = new JButton("Create");
		this.btnCreateTPair.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				GUI.this.createTPairs();
				GUI.this.setButtons();
			}
		});
		this.btnCreateTPair.setBounds(215, 96, 83, 23);
		this.btnCreateTPair.setFont(this.font);
		functionPanel.add(this.btnCreateTPair);

		// show
		this.btnShowTPair = new JButton("Show");
		this.btnShowTPair.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.showTPairs();
				}
			}
		});
		this.btnShowTPair.setBounds(307, 96, 83, 23);
		this.btnShowTPair.setFont(this.font);
		functionPanel.add(this.btnShowTPair);

		// step 3 buttons

		// compute
		this.btnCreateKappa = new JButton("Compute");
		this.btnCreateKappa.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.createKappa();
					GUI.this.setButtons();
				}
			}
		});
		this.btnCreateKappa.setBounds(420, 66, 95, 23);
		this.btnCreateKappa.setFont(this.font);
		functionPanel.add(this.btnCreateKappa);

		// show
		this.btnShowKappa = new JButton("Show");
		this.btnShowKappa.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.showKappa();
					GUI.this.setButtons();
				}
			}
		});
		this.btnShowKappa.setBounds(525, 66, 75, 23);
		this.btnShowKappa.setFont(this.font);
		functionPanel.add(this.btnShowKappa);

		// details
		this.btnExplainKappa = new JButton("Details");
		this.btnExplainKappa.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.explainKappa();
					GUI.this.setButtons();
				}
			}
		});
		this.btnExplainKappa.setBounds(420, 96, 95, 23);
		this.btnExplainKappa.setFont(this.font);
		functionPanel.add(this.btnExplainKappa);

		// csv
		this.btnShowKappaCsv = new JButton("CSV");
		this.btnShowKappaCsv.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.showKappaCSV();
					GUI.this.setButtons();
				}
			}
		});
		this.btnShowKappaCsv.setBounds(525, 96, 75, 23);
		this.btnShowKappaCsv.setFont(this.font);
		functionPanel.add(this.btnShowKappaCsv);

		// step 4 buttons

		// ask
		this.btnAskQuery = new JButton("Ask");
		this.btnAskQuery.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.askQuery();
					GUI.this.setButtons();
				}
			}
		});
		this.btnAskQuery.setBounds(625, 96, 83, 23);
		this.btnAskQuery.setFont(this.font);
		functionPanel.add(this.btnAskQuery);

		// text step 1
		JLabel labelKB = new JLabel("Enter the KB in the input area");
		labelKB.setBounds(10, 36, 175, 20);
		labelKB.setFont(this.font);
		functionPanel.add(labelKB);

		// text step 4
		JLabel labelQuery = new JLabel("Enter the query in the input area");
		labelQuery.setBounds(625, 36, 218, 20);
		labelQuery.setFont(this.font);
		functionPanel.add(labelQuery);

		// input / output
		JPanel inputOutputPanel = new JPanel();
		this.contentPane.add(inputOutputPanel, BorderLayout.CENTER);
		inputOutputPanel.setLayout(new BorderLayout(0, 0));

		JSeparator separator = new JSeparator();
		inputOutputPanel.add(separator, BorderLayout.NORTH);

		// input area
		JPanel inputPanel = new JPanel();
		inputPanel.setPreferredSize(new Dimension(300, 10));
		inputPanel.setLayout(new BorderLayout(0, 0));

		JPanel inputPanelHeader = new JPanel();
		inputPanelHeader.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		inputPanel.add(inputPanelHeader, BorderLayout.NORTH);

		JLabel inputPanelTitle = new JLabel("Input");
		inputPanelTitle.setFont(this.font);
		inputPanelHeader.add(inputPanelTitle);

		JButton btnInputOpen = new JButton("Open");
		btnInputOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.openFile(GUI.this.inputArea);
				}
			}
		});
		btnInputOpen.setFont(this.font);
		inputPanelHeader.add(btnInputOpen);

		JButton btnInputSave = new JButton("Save");
		btnInputSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.saveFile(GUI.this.inputArea);
				}
			}
		});
		btnInputSave.setFont(this.font);
		inputPanelHeader.add(btnInputSave);

		this.inputArea = new JTextArea();
		this.inputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		JScrollPane scrollPaneInput = new JScrollPane(this.inputArea);
		scrollPaneInput.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneInput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		inputPanel.add(scrollPaneInput, BorderLayout.CENTER);

		// output area
		this.outputPanel = new JPanel();
		this.outputPanel.setPreferredSize(new Dimension(300, 10));
		this.outputPanel.setLayout(new BorderLayout(0, 0));

		JPanel outputPanelHeader = new JPanel();
		outputPanelHeader.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		this.outputPanel.add(outputPanelHeader, BorderLayout.NORTH);

		JLabel outputPanelTitle = new JLabel("Output");
		outputPanelTitle.setFont(this.font);
		outputPanelHeader.add(outputPanelTitle);

		JButton outputSave = new JButton("Save");
		outputSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (GUI.this.tPairTask == null) {
					GUI.this.saveFile(GUI.this.outputArea);
				}
			}
		});
		outputSave.setFont(this.font);
		outputPanelHeader.add(outputSave);

		this.outputArea = new JTextArea();
		this.outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		JScrollPane scrollPaneOutput = new JScrollPane(this.outputArea);
		scrollPaneOutput.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneOutput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.outputPanel.add(scrollPaneOutput, BorderLayout.CENTER);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, this.outputPanel);
		splitPane.setResizeWeight(0.5);
		splitPane.setBorder(null);
		inputOutputPanel.add(splitPane, BorderLayout.CENTER);

		// progress bar
		this.progressBar = new JProgressBar(0, 100);
		this.progressBar.setStringPainted(true);
		this.contentPane.add(this.progressBar, BorderLayout.SOUTH);
	}

	/**
	 * Launches the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}