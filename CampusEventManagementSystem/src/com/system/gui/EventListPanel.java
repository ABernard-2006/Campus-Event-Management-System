package com.system.gui;

import classModel.Event;
import classModel.UserAccount;
import com.system.service.EventManager;
import com.system.service.StorageException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Lists every saved event so an Administrator or Organizer can edit or delete one.
 * Handwritten (no .form file). All changes go through the shared EventManager,
 * which checks the permission again.
 */
public class EventListPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final EventManager manager;
    private final UserAccount actor;
    private final Consumer<Event> editHandler;
    private final DefaultTableModel model = new DefaultTableModel(new Object[][]{},
            new String[]{"ID", "Event Name", "Type", "Date", "Time", "Duration", "Venue", "Capacity", "Registered", "Speaker(s)"}) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField txtSearch = new JTextField(16);
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnShowAll = new JButton("Show All");
    private final JButton btnEdit = new JButton("Edit Selected");
    private final JButton btnDelete = new JButton("Delete Selected");
    private final JLabel lblCount = new JLabel(" ");
    private List<Event> shown = new java.util.ArrayList<>();      // same order as the table rows

    public EventListPanel(EventManager manager, UserAccount actor, Consumer<Event> editHandler) {
        this.manager = manager;
        this.actor = actor;
        this.editHandler = editHandler;
        build();
        refresh();
    }

    private void build() {
        setLayout(new BorderLayout(8, 8));
        setBackground(new java.awt.Color(247, 247, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("All Events");
        title.setFont(new java.awt.Font("Segoe UI", 1, 18));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBar.setOpaque(false);
        searchBar.add(new JLabel("Search:"));
        searchBar.add(txtSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnShowAll);
        top.add(title, BorderLayout.WEST);
        top.add(searchBar, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);    // Time shows "start - end"
        table.getColumnModel().getColumn(5).setPreferredWidth(70);     // Duration
        table.getColumnModel().getColumn(6).setPreferredWidth(110);    // Venue
        table.getColumnModel().getColumn(9).setPreferredWidth(150);    // Speaker(s)
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        bottom.add(btnEdit);
        bottom.add(btnDelete);
        bottom.add(lblCount);
        add(bottom, BorderLayout.SOUTH);

        btnSearch.addActionListener(this::btnSearchActionPerformed);
        txtSearch.addActionListener(this::txtSearchActionPerformed);
        btnShowAll.addActionListener(this::btnShowAllActionPerformed);
        btnEdit.addActionListener(this::btnEditActionPerformed);
        btnDelete.addActionListener(this::btnDeleteActionPerformed);
        table.addMouseListener(new java.awt.event.MouseAdapter() {      // double-click a row = Edit Selected
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });
    }

    // ---------- Handlers (hand-built panel, so no .form): each reads the GUI, then calls the service or refreshes ----------
    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) { refresh(); }

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) { refresh(); }

    private void btnShowAllActionPerformed(java.awt.event.ActionEvent evt) {
        txtSearch.setText("");
        refresh();
    }

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) { editSelected(); }

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) { deleteSelected(); }

    /** Re-reads the events from the manager (keeps the selected event selected when it still exists). */
    @Override
    public void refresh() {
        int selectedId = -1;
        int row = table.getSelectedRow();
        if (row >= 0 && row < shown.size()) selectedId = shown.get(row).getEventId();

        shown = manager.searchEventsChronological(txtSearch.getText());
        model.setRowCount(0);
        for (Event e : shown) {
            model.addRow(new Object[]{e.getEventId(), e.getEventName(), e.getEventType(),
                e.getDate().format(DATE_FMT), e.getTimeRangeText(), e.getDurationText(),
                e.getVenue().getVenueName(), e.getVenue().getCapacity(), manager.countActive(e),
                e.getSpeakerNames()});
        }
        lblCount.setText(shown.size() + " event(s)");
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).getEventId() == selectedId) { table.setRowSelectionInterval(i, i); break; }
        }
    }

    private Event selectedEvent() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            JOptionPane.showMessageDialog(this, "Select an event in the table first.", "No event selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return shown.get(row);
    }

    private void editSelected() {
        Event e = selectedEvent();
        if (e != null && editHandler != null) editHandler.accept(e);
    }

    private void deleteSelected() {
        Event e = selectedEvent();
        if (e == null) return;
        int regs;
        try {
            regs = manager.getRegistrationsFor(actor, e).size();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String warning = "Delete \"" + e.getEventName() + "\"?\n\n"
                + (regs == 0 ? "Nobody is registered for it."
                        : "This also permanently removes its " + regs + " registration record(s) and their attendance records.")
                + "\nThis cannot be undone.";
        int choice = JOptionPane.showConfirmDialog(this, warning, "Delete event",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            manager.deleteEvent(actor, e);
            JOptionPane.showMessageDialog(this, "Deleted: " + e.getEventName());
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
        } catch (StorageException ex) {
            JOptionPane.showMessageDialog(this, "The change could not be saved, so nothing was deleted:\n" + ex.getMessage(),
                    "Cannot complete action", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot complete action", JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }
}
