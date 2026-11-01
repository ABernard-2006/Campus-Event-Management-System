package com.system.gui;

import classModel.OrganizerKey;
import classModel.UserAccount;
import com.system.service.EventManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Administrator-only window that lists organizer invitation keys (without the secret text),
 * generates new ones and revokes unused ones.
 *
 * Open it with OrganizerKeysDialog.open(...). The service layer checks the administrator
 * role on every operation, so hiding or showing a button is never the only protection.
 */
public class OrganizerKeysDialog extends JDialog {
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EventManager manager;
    private final UserAccount actor;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Label", "Status", "Created", "Expires", "Used by" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton btnGenerate = new JButton("Generate New Key...");
    private final JButton btnRevoke = new JButton("Revoke Selected");
    private final JButton btnClose = new JButton("Close");

    /** Opens the window, or shows a message when the user is not an administrator. */
    public static void open(Frame owner, EventManager manager, UserAccount actor) {
        try {
            new OrganizerKeysDialog(owner, manager, actor).setVisible(true);
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Organizer Keys", JOptionPane.ERROR_MESSAGE);
        }
    }

    public OrganizerKeysDialog(Frame owner, EventManager manager, UserAccount actor) {
        super(owner, "Organizer Invitation Keys", true);
        this.manager = manager;
        this.actor = actor;
        manager.getOrganizerKeys(actor);        // throws SecurityException at once if 'actor' is not an administrator
        buildUi();
        refresh();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel info = new JLabel("<html>Generate a key and give it to the person who should become an Organizer. "
                + "Keys work once and are never shown again after they are created.</html>");
        info.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(125);
        table.getColumnModel().getColumn(4).setPreferredWidth(125);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(660, 260));
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnGenerate);
        buttons.add(btnRevoke);
        buttons.add(btnClose);

        add(info, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        btnRevoke.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateRevokeButton();
        });
        btnGenerate.addActionListener(e -> generate());
        btnRevoke.addActionListener(e -> revoke());
        btnClose.addActionListener(e -> dispose());
    }

    private void refresh() {
        model.setRowCount(0);
        LocalDateTime now = LocalDateTime.now();
        List<OrganizerKey> keys = manager.getOrganizerKeys(actor);
        for (OrganizerKey k : keys) {
            model.addRow(new Object[] {
                k.getKeyId(),
                k.getLabel().isEmpty() ? "(no label)" : k.getLabel(),
                k.getDisplayStatus(now),
                k.getCreatedAt().format(SHOWN),
                k.getExpiresAt().format(SHOWN),
                k.getUsedBy().isEmpty() ? "" : k.getUsedBy()
            });
        }
        updateRevokeButton();
    }

    private void updateRevokeButton() {
        int row = table.getSelectedRow();
        btnRevoke.setEnabled(row >= 0 && "Active".equals(model.getValueAt(row, 2)));
    }

    private void generate() {
        GenerateKeyDialog dialog = new GenerateKeyDialog(this, manager, actor);
        dialog.setVisible(true);
        refresh();
    }

    private void revoke() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int keyId = (Integer) model.getValueAt(row, 0);
        int answer = JOptionPane.showConfirmDialog(this,
                "Revoke key #" + keyId + "? Nobody will be able to sign up with it afterwards.",
                "Revoke Key", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            manager.revokeOrganizerKey(actor, keyId);
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Revoke Key", JOptionPane.WARNING_MESSAGE);
        }
        refresh();
    }
}
