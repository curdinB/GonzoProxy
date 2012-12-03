package ch.compass.gonzoproxy.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ch.compass.gonzoproxy.controller.RelayController;
import ch.compass.gonzoproxy.model.RuleModel;
import ch.compass.gonzoproxy.model.RuleSetModel;
import ch.compass.gonzoproxy.relay.modifier.PacketModifier;
import ch.compass.gonzoproxy.relay.modifier.FieldRule;
import ch.compass.gonzoproxy.relay.modifier.PacketRule;
import java.awt.FlowLayout;

public class ModifierDialog extends JDialog {

	private static final long serialVersionUID = 9047578530331858262L;
	private JPanel contentPane;
	private JTable tableRules;
	private RelayController controller;
	private JList<String> listPacketRule;
	private RuleModel fieldRuleModel;
	private PacketModifier modifier;
	protected PacketRule editPacketRule;
	private JCheckBox chckbxUpdateLengthAutomatically;
	protected FieldRule editFieldRule;
	private JPanel panel;
	private JButton btnDeleteSelectedFieldRule;
	private JButton btnDeleteSelectedPacketRule;
	private RuleSetModel packetRuleModel;
	private FieldRule dummyFieldRule;
	private PacketRule dummyPacketRule;
	private JButton btnClose;

	public ModifierDialog(RelayController controller) {
		this.controller = controller;
		this.modifier = controller.getPacketModifier();
		this.dummyFieldRule = new FieldRule("", "", "");
		this.dummyPacketRule = new PacketRule("");
		initGui();
	}

	private void initGui() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(true);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Modifiers for parsable packets");
		setBounds(100, 100, 850, 225);
		setMinimumSize(new Dimension(780, 150));
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 131, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, 1.0,
				Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 0;
		contentPane.add(scrollPane_1, gbc_scrollPane_1);

		packetRuleModel = new RuleSetModel(modifier.getRuleSets());
		listPacketRule = new JList<String>(packetRuleModel);
		listPacketRule.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int index = listPacketRule.getSelectedIndex();
				if (index == -1) {
					setEditRuleSet(dummyPacketRule);
					setEditRule(dummyFieldRule);
					fieldRuleModel.setRules(dummyPacketRule.getRules());
				} else {
					setEditRuleSet(modifier.getRuleSets().get(
							listPacketRule.getSelectedIndex()));
					fieldRuleModel.setRules(editPacketRule.getRules());
				}
			}
		});
		scrollPane_1.setViewportView(listPacketRule);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 0;
		contentPane.add(scrollPane, gbc_scrollPane);

		fieldRuleModel = new RuleModel();
		tableRules = new JTable(fieldRuleModel);
		tableRules.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable target = (JTable) e.getSource();
				int row = target.getSelectedRow();
				if (row == -1) {
					setEditRule(dummyFieldRule);
				} else {
					setEditRule(editPacketRule.getRules().get(row));
				}

			}
		});
		scrollPane.setViewportView(tableRules);

		chckbxUpdateLengthAutomatically = new JCheckBox(
				"Update Content Length automatically");
		chckbxUpdateLengthAutomatically.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(chckbxUpdateLengthAutomatically.isSelected()){
					editPacketRule.shouldUpdateLength(true);
				}else{
					editPacketRule.shouldUpdateLength(false);
				}				
			}
		});
		GridBagConstraints gbc_chckbxUpdateLengthAutomatically = new GridBagConstraints();
		gbc_chckbxUpdateLengthAutomatically.anchor = GridBagConstraints.WEST;
		gbc_chckbxUpdateLengthAutomatically.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxUpdateLengthAutomatically.gridx = 0;
		gbc_chckbxUpdateLengthAutomatically.gridy = 1;
		contentPane.add(chckbxUpdateLengthAutomatically,
				gbc_chckbxUpdateLengthAutomatically);

		panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.EAST;
		gbc_panel.fill = GridBagConstraints.VERTICAL;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 1;
		contentPane.add(panel, gbc_panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		btnDeleteSelectedPacketRule = new JButton("Delete selected ruleset");
		btnDeleteSelectedPacketRule.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				modifier.getRuleSets().remove(editPacketRule);
				packetRuleModel.setRules(modifier.getRuleSets());
				if(listPacketRule.getSelectedIndex() < modifier.getRuleSets().size()){
					setEditRuleSet(modifier.getRuleSets().get(listPacketRule.getSelectedIndex()));
					fieldRuleModel.setRules(editPacketRule.getRules());
				}else{
					fieldRuleModel.setRules(dummyPacketRule.getRules());
					setEditRuleSet(dummyPacketRule);
				}
				setEditRule(dummyFieldRule);
			}
		});
		panel.add(btnDeleteSelectedPacketRule);

		btnDeleteSelectedFieldRule = new JButton("Delete selected rule");
		btnDeleteSelectedFieldRule.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				editPacketRule.getRules().remove(editFieldRule);
				fieldRuleModel.setRules(editPacketRule.getRules());
			}
		});
		panel.add(btnDeleteSelectedFieldRule);
		
		btnClose = new JButton("Close");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		panel.add(btnClose);
	}

	protected void setEditRule(FieldRule rule) {
		this.editFieldRule = rule;
	}

	protected void setEditRuleSet(PacketRule ruleSet) {
		this.editPacketRule = ruleSet;
			chckbxUpdateLengthAutomatically.setSelected(ruleSet
					.shouldUpdateContentLength());
	}

}
