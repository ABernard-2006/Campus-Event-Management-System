package com.system.gui;

import classModel.UserAccount;
import com.system.service.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Lets an administrator create ONE organizer invitation key and shows it once.
 * The key is not saved anywhere readable, so it cannot be displayed again after this window closes.
 * The service layer (EventManager) re-checks that the user really is an administrator.
 */
public class GenerateKeyDialog extends JDialog {
    private final EventManager manager;
    private final UserAccount actor;

    private final JTextField txtLabel = new JTextField(22);
    private final JSpinner spnDays = new JSpinner(new SpinnerNumberModel(7, 1, 30, 1));
    private final JButton btnGenerate = new JButton("Generate Key");
    private final JButton btnClose = new JButton("Cancel");

    private final JPanel pnlResult = new JPanel(new GridBagLayout());
    private final JTextField txtKey = new JTextField(22);
    private final JButton btnCopy = new JButton("Copy");

    private boolean generated = false;

    public GenerateKeyDialog(JDialog owner, EventManager manager, UserAccount actor) {
        super(owner, "Generate Organizer Key", true);
        this.manager = manager;
        this.actor = actor;
        buildUi();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** True when a key was created while this window was open (so the list behind it should refresh). */
    public boolean wasGenerated() { return generated; }

    private void buildUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Label (e.g. the person's name):"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(txtLabel, c);

        c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(new JLabel("Valid for (days, 1-30):"), c);
        c.gridx = 1;
        form.add(spnDays, c);

        // The result area stays hidden until a key has been generated.
        txtKey.setEditable(false);
        txtKey.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        txtKey.setHorizontalAlignment(JTextField.CENTER);
        JLabel warning = new JLabel("<html><b>Copy this key now.</b> It is shown only once and cannot be<br>"
                + "recovered later. If it is lost, revoke it and generate a new one.</html>");
        warning.setForeground(new Color(150, 40, 40));
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(4, 0, 4, 4);
        r.gridx = 0; r.gridy = 0; r.fill = GridBagConstraints.HORIZONTAL; r.weightx = 1;
        pnlResult.add(txtKey, r);
        r.gridx = 1; r.weightx = 0;
        pnlResult.add(btnCopy, r);
        r.gridx = 0; r.gridy = 1; r.gridwidth = 2;
        pnlResult.add(warning, r);
        pnlResult.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        pnlResult.setVisible(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnGenerate);
        buttons.add(btnClose);

        add(form, BorderLayout.NORTH);
        add(pnlResult, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        btnGenerate.addActionListener(e -> generate());
        btnCopy.addActionListener(e -> copyKey());
        btnClose.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnGenerate);
    }

    private void generate() {
        try {
            try {
                spnDays.commitEdit();            // accept a number that was typed but not yet confirmed with Enter
            } catch (java.text.ParseException ex) {
                throw new IllegalArgumentException("Enter the number of days as a whole number from 1 to 30.");
            }
            int days = ((Number) spnDays.getValue()).intValue();
            String key = manager.generateOrganizerKey(actor, txtLabel.getText(), days);
            generated = true;
            txtKey.setText(key);
            pnlResult.setVisible(true);
            // One key per window: stop further clicks and switch to "Done".
            btnGenerate.setEnabled(false);
            txtLabel.setEditable(false);
            spnDays.setEnabled(false);
            btnClose.setText("Done");
            getRootPane().setDefaultButton(btnClose);
            pack();
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Generate Key", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void copyKey() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txtKey.getText()), null);
            btnCopy.setText("Copied");
        } catch (RuntimeException ex) {
            txtKey.selectAll();   // clipboard unavailable: the key is selected so Ctrl+C still works
            txtKey.requestFocusInWindow();
        }
    }
}
