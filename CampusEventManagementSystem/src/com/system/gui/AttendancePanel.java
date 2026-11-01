package com.system.gui;

import classModel.Attendance;
import classModel.Event;
import classModel.Registration;
import classModel.UserAccount;
import com.system.service.EventManager;
import com.system.service.StorageException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Attendance screen for Administrators and Organizers: choose an event, see who registered,
 * and mark the selected people Present or Absent. Handwritten (no .form file).
 * Every change goes through EventManager.markAttendance(...), which checks the permission,
 * the registration, and that a registration never gets two attendance records.
 */
public class AttendancePanel extends JPanel implements Refreshable {
    private final EventManager manager;
    private final UserAccount actor;
    private final List<Event> eventChoices = new ArrayList<>();            // same order as the combo box
    private List<Registration> shown = new ArrayList<>();                  // same order as the table rows
    private boolean loading;                                               // true while the combo box is rebuilt

    private final JComboBox<String> cmbEvent = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new Object[][]{},
            new String[]{"Reg. ID", "Participant", "Type", "Contact", "Registered On", "Registration", "Attendance"}) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblSummary = new JLabel(" ");

    public AttendancePanel(EventManager manager, UserAccount actor) {
        this.manager = manager;
        this.actor = actor;
        build();
        refresh();
    }

    private void build() {
        setLayout(new BorderLayout(8, 8));
        setBackground(new java.awt.Color(247, 247, 252));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("Attendance");
        title.setFont(new java.awt.Font("Segoe UI", 1, 18));
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pick.setOpaque(false);
        pick.add(new JLabel("Event:"));
        cmbEvent.setPrototypeDisplayValue("0000 - A fairly long event name (Workshop)");
        pick.add(cmbEvent);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(pick, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnPresent = new JButton("Mark Present");
        JButton btnAbsent = new JButton("Mark Absent");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        bottom.add(btnPresent);
        bottom.add(btnAbsent);
        bottom.add(lblSummary);
        add(bottom, BorderLayout.SOUTH);

        cmbEvent.addActionListener(e -> { if (!loading) loadRegistrations(); });
        btnPresent.addActionListener(e -> mark(true));
        btnAbsent.addActionListener(e -> mark(false));
    }

    /** Rebuilds the event list and the table, keeping the same event selected when it still exists. */
    @Override
    public void refresh() {
        int keepId = -1;
        int idx = cmbEvent.getSelectedIndex();
        if (idx >= 0 && idx < eventChoices.size()) keepId = eventChoices.get(idx).getEventId();

        loading = true;
        eventChoices.clear();
        cmbEvent.removeAllItems();
        List<Event> sorted = new ArrayList<>(manager.getAllEvents());
        sorted.sort(Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime));
        int select = sorted.isEmpty() ? -1 : 0;
        for (Event e : sorted) {
            if (e.getEventId() == keepId) select = eventChoices.size();
            eventChoices.add(e);
            cmbEvent.addItem(e.getEventId() + " - " + e.getEventName() + " (" + e.getDate() + ")");
        }
        if (select >= 0) cmbEvent.setSelectedIndex(select);
        loading = false;
        loadRegistrations();
    }

    private Event currentEvent() {
        int i = cmbEvent.getSelectedIndex();
        return (i < 0 || i >= eventChoices.size()) ? null : eventChoices.get(i);
    }

    private static String attendanceLabel(Registration r, Attendance a) {
        if (!r.isActive()) return "-";
        if (a == null) return "Not marked";
        return a.isAttended() ? "Present" : "Absent";
    }

    private void loadRegistrations() {
        model.setRowCount(0);
        shown = new ArrayList<>();
        Event event = currentEvent();
        if (event == null) {
            lblSummary.setText("No events yet. Create an event first.");
            return;
        }
        try {
            shown = new ArrayList<>(manager.getRegistrationsFor(actor, event));
        } catch (SecurityException ex) {
            lblSummary.setText(ex.getMessage());
            return;
        }
        for (Registration r : shown) {
            model.addRow(new Object[]{r.getRegistrationId(), r.getParticipant().getFullName(),
                r.getParticipant().getClass().getSimpleName(), r.getParticipant().getContactInfo(),
                r.getRegistrationDate().toString(), r.getStatus(), attendanceLabel(r, manager.getAttendance(r))});
        }
        lblSummary.setText("Registered: " + manager.countActive(event) + "   Present: " + manager.countAttended(event));
    }

    private void mark(boolean present) {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more registered participants first.",
                    "No participant selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Registration> picked = new ArrayList<>();
        for (int row : rows) if (row >= 0 && row < shown.size()) picked.add(shown.get(row));

        int done = 0;
        List<String> problems = new ArrayList<>();
        for (Registration r : picked) {
            try {
                manager.markAttendance(actor, r, present);
                done++;
            } catch (SecurityException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
                return;
            } catch (StorageException ex) {
                problems.add(r.getParticipant().getFullName() + ": could not be saved (" + ex.getMessage() + ")");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                problems.add(ex.getMessage());
            }
        }
        loadRegistrations();
        if (!problems.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    (done > 0 ? done + " updated.\n\n" : "") + String.join("\n", problems),
                    "Attendance", JOptionPane.WARNING_MESSAGE);
        }
    }
}
