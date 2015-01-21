package eddy.lang.net;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import eddy.lang.net.Agent.Party;

/**
 * Provides a graphic user interface to interact with an {@link Agent}.
 * 
 * @author Travis Breaux
 *
 */

public class AgentApp extends JFrame {
	public final static long serialVersionUID = 0;
	private Agent agent = null;
	
	public static void main(String[] args) {
		Agent agent = AgentReader.read("examples/example.net.parties");
		AgentApp app = new AgentApp(agent);
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.pack();
		app.setLocationRelativeTo(null);
		app.setVisible(true);
	}
	
	public AgentApp(Agent agent) {
		super("Eddy Policy Server");
		this.agent = agent;
		
		Container pane = this.getContentPane();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		
		JTabbedPane tabbedPane = new JTabbedPane();
		
		JPanel policyPane = createActivePolicyPane();
		tabbedPane.addTab("Policies", policyPane);
		
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.fill = GridBagConstraints.BOTH;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		pane.add(tabbedPane, gc);
	}
	
	private JPanel createActivePolicyPane() {
		JPanel panel = new JPanel();
		
		
		GridBagConstraints gc = new GridBagConstraints();
		
		final JTextField field = new JTextField(32);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		gc.insets = new Insets(10, 10, 10, 10);
		panel.add(field, gc);

		final JButton accept = new JButton("Import");
		gc.anchor = GridBagConstraints.SOUTHEAST;
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weightx = 0.0;
		gc.weighty = 0.0;
		gc.insets = new Insets(10, 10, 10, 10);
		panel.add(accept, gc);

		final PartyTableModel model = new PartyTableModel();
		for (Party party : agent.parties()) {
			model.add(party);
		}
		
		final JTable table = new JTable(model);
		
		JScrollPane scrollPane = new JScrollPane(table);
		gc.gridx = 0;
		gc.gridy = 1;
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.fill = GridBagConstraints.BOTH;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.gridwidth = 2;
		panel.setLayout(new GridBagLayout());
		panel.add(scrollPane, gc);
		
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					int[] selected = table.getSelectedRows();
					for (int i = 0; i < selected.length; i++) {
						model.removeValuesAt(selected[i]);
					}
				}
			}
		});
		
		return panel;
	}
	
	class PartyDialog extends JDialog {
		final static long serialVersionUID = 0;
		private Party party;
		
		public PartyDialog(Frame f) {
			super(f, "Add New Party");
			Container pane = this.getContentPane();
			pane.setLayout(new GridBagLayout());
			//GridBagConstraints gc = new GridBagConstraints();
			
			setSize(300, 100);
			pack();
		}
		
		public Party getParty() {
			return party;
		}
	}
	
	class PartyTableModel extends AbstractTableModel {
		final static long serialVersionUID = 0;
		private final ArrayList<Party> parties = new ArrayList<Party>();
		private final TreeMap<URI,String> dates = new TreeMap<URI,String>();
		
		public int getColumnCount() {
			return 3;
		}
		
		public void add(Party party) {
			parties.add(party);
			
			long time = System.currentTimeMillis();
			String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(time));
			dates.put(party.uri, date);
			
			this.fireTableRowsInserted(parties.size() - 1, parties.size() - 1);
		}
		
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Actor";
				case 1:
					return "Path";
				case 2:
					return "Date";
				default:
					return "Unknown";
			}
		}

		public int getRowCount() {
			return parties.size();
		}

		public Object getValueAt(int row, int col) {
			if (row < 0 || parties.size() <= row) {
				return "**";
			}
			switch (col) {
				case 0:
					return parties.get(row).actor.name;
				case 1:
					return parties.get(row).uri.toASCIIString();
				case 2:
					return dates.get(parties.get(row).uri);
				default:
					return "--";
			}
		}
		
		public void removeValuesAt(int row) {
			Party party = parties.get(row);
			parties.remove(row);
			dates.remove(party.uri);
			this.fireTableRowsDeleted(row, row);
		}
	}
}
