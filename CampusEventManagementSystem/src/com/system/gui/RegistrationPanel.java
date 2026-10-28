/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.system.gui;

import classModel.Event;
import classModel.Faculty;
import classModel.Others;
import classModel.Participant;
import classModel.Registration;
import classModel.Student;
import classModel.UserAccount;
import com.system.service.EventManager;
import com.system.service.StorageException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Registration screen.
 *
 *  - A Participant account registers ONLY itself: the contact/email is fixed to the account's email,
 *    the participant profile (name, student number ...) is filled in once and then locked, and the
 *    history shows only their own registrations. They can cancel only their own.
 *  - Administrators and Organizers can register walk-in participants and cancel any registration.
 *
 * These rules are repeated inside EventManager, so they hold even if a button is changed here.
 * The generated NetBeans code below is untouched; everything handwritten is above it.
 *
 * @author Dhustine
 */
public class RegistrationPanel extends javax.swing.JPanel implements Refreshable {
    private final EventManager manager;
    private final UserAccount actor;
    private final boolean staff;
    private final List<Event> eventChoices = new ArrayList<>();   // same order as the combo box items
    private final List<Registration> shown = new ArrayList<>();   // same order as the table rows
    private final JButton btnCancelReg = new JButton("Cancel Selected Registration");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[][]{}, new String[]{"Registration ID", "Name", "Event", "Type", "Status"}) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };

    /** Creates new form RegistrationPanel (used by the NetBeans designer only). */
    public RegistrationPanel() {
        this(null, null);
    }

    public RegistrationPanel(EventManager manager, UserAccount actor) {
        this.manager = manager;
        this.actor = actor;
        this.staff = manager != null && manager.isStaff(actor);
        initComponents();
        setupForm();
    }

    private void setupForm() {
        regTable.setModel(tableModel);
        regTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lblRegExtra2.setText("Position:");
        if (!staff) lblParticipantsReg.setText("Register for an Event");

        addCancelBar();

        cmbParticipantType.addActionListener(e -> updateTypeFields());
        btnRegister.addActionListener(e -> registerParticipant());
        btnRegClear.addActionListener(e -> clearForm());
        btnSearchReg.addActionListener(e -> loadTable(txtSearchReg.getText()));
        txtSearchReg.addActionListener(e -> loadTable(txtSearchReg.getText()));
        btnCancelReg.addActionListener(e -> cancelSelected());

        updateTypeFields();
        refresh();
    }

    /**
     * Puts a "Cancel Selected Registration" button under the history table without touching the
     * generated layout: the table's scroll pane is swapped for a small holder panel that contains
     * the same scroll pane plus the button bar.
     */
    private void addCancelBar() {
        JPanel holder = new JPanel(new BorderLayout(0, 4));
        holder.setOpaque(false);
        holder.setPreferredSize(new java.awt.Dimension(100, 204));   // same height the form gave the table
        ((javax.swing.GroupLayout) getLayout()).replace(jScrollPane1, holder);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.add(btnCancelReg);
        holder.add(jScrollPane1, BorderLayout.CENTER);
        holder.add(bar, BorderLayout.SOUTH);
    }

    /** Re-reads events and registrations from the manager (also called by the dashboard after any change). */
    @Override
    public void refresh() {
        loadEvents();
        applyProfileMode();
        loadTable(txtSearchReg.getText());
    }

    /** Fills the event combo box with events that can still be registered for (today or later). */
    private void loadEvents() {
        int keepId = -1;
        int old = cmbSelectEvent.getSelectedIndex();
        if (old >= 0 && old < eventChoices.size()) keepId = eventChoices.get(old).getEventId();

        eventChoices.clear();
        cmbSelectEvent.removeAllItems();
        if (manager == null) return;
        List<Event> open = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Event e : manager.getAllEvents()) if (!e.getDate().isBefore(today)) open.add(e);
        open.sort(Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime));
        int select = open.isEmpty() ? -1 : 0;
        for (Event e : open) {
            if (e.getEventId() == keepId) select = eventChoices.size();
            eventChoices.add(e);
            cmbSelectEvent.addItem(e.getEventId() + " - " + e.getEventName() + " (" + e.getEventType() + ")");
        }
        if (select >= 0) cmbSelectEvent.setSelectedIndex(select);
    }

    private String selectedType() {
        Object o = cmbParticipantType.getSelectedItem();
        return o == null ? "" : o.toString().trim();
    }

    /** Dynamic switching of the labels/fields for Student, Faculty and Others. */
    private void updateTypeFields() {
        String t = selectedType();
        boolean student = t.equals("Student");
        boolean faculty = t.equals("Faculty");
        boolean others = t.equals("Others");

        lblRegNum.setText(student ? "Student Number:" : "Faculty ID:");
        lblRegNum.setVisible(!others);
        txtRegNum.setVisible(!others);

        lblRegExtra.setText(student ? "Program:" : faculty ? "Department:" : "Affiliation / Company:");
        lblRegExtra2.setVisible(faculty);
        txtRegExtra2.setVisible(faculty);
    }

    // ---------- Participant accounts: fixed to their own profile ----------
    private boolean hasLockedProfile() {
        return !staff && manager != null && actor != null && manager.findParticipantByContact(actor.getEmail()) != null;
    }

    private void applyProfileMode() {
        if (staff || manager == null || actor == null) return;
        txtRegContact.setText(actor.getEmail());
        txtRegContact.setEditable(false);
        Participant mine = manager.findParticipantByContact(actor.getEmail());
        boolean has = mine != null;
        if (has) fillFrom(mine);
        txtRegName.setEditable(!has);
        txtRegNum.setEditable(!has);
        txtRegExtra.setEditable(!has);
        txtRegExtra2.setEditable(!has);
        cmbParticipantType.setEnabled(!has);
        btnRegClear.setEnabled(!has);
    }

    private void fillFrom(Participant p) {
        if (p instanceof Student) {
            cmbParticipantType.setSelectedItem("Student");
            txtRegNum.setText(((Student) p).getStudentNumber());
            txtRegExtra.setText(((Student) p).getProgram());
            txtRegExtra2.setText("");
        } else if (p instanceof Faculty) {
            cmbParticipantType.setSelectedItem("Faculty");
            txtRegNum.setText(((Faculty) p).getFacultyNumber());
            txtRegExtra.setText(((Faculty) p).getDepartment());
            txtRegExtra2.setText(((Faculty) p).getPosition());
        } else if (p instanceof Others) {
            cmbParticipantType.setSelectedItem("Others");
            txtRegNum.setText("");
            txtRegExtra.setText(((Others) p).getOrgAffiliation());
            txtRegExtra2.setText("");
        }
        txtRegName.setText(p.getFullName());
        updateTypeFields();
    }

    // ---------- Register ----------
    private void registerParticipant() {
        try {
            if (manager == null) throw new IllegalStateException("No EventManager connected to this panel.");

            int idx = cmbSelectEvent.getSelectedIndex();
            if (idx < 0 || idx >= eventChoices.size())
                throw new IllegalArgumentException("No event selected. There may be no upcoming events yet.");
            Event event = eventChoices.get(idx);

            String type = selectedType();
            String name = txtRegName.getText().trim();
            String contact = staff ? txtRegContact.getText().trim() : actor.getEmail();
            String number = txtRegNum.getText().trim();
            String extra = txtRegExtra.getText().trim();
            String extra2 = txtRegExtra2.getText().trim();

            if (type.isEmpty() || !(type.equals("Student") || type.equals("Faculty") || type.equals("Others")))
                throw new IllegalArgumentException("Choose a participant type.");
            if (name.isEmpty()) throw new IllegalArgumentException("Participant name is required.");
            if (contact.isEmpty()) throw new IllegalArgumentException("BU/email address is required.");
            if (contact.matches(".*\\s+.*"))
                throw new IllegalArgumentException("BU/email address must not contain spaces.");
            if (type.equals("Student")) {
                if (number.isEmpty()) throw new IllegalArgumentException("Student number is required.");
                if (extra.isEmpty()) throw new IllegalArgumentException("Program is required.");
            } else if (type.equals("Faculty")) {
                if (number.isEmpty()) throw new IllegalArgumentException("Faculty ID is required.");
                if (extra.isEmpty()) throw new IllegalArgumentException("Department is required.");
                if (extra2.isEmpty()) throw new IllegalArgumentException("Position is required.");
            } else if (extra.isEmpty()) {
                throw new IllegalArgumentException("Affiliation / company is required.");
            }

            // A Participant account always uses its own profile; staff can pick or create anyone.
            Participant p = staff ? manager.findParticipant(type, number, name)
                                  : manager.findParticipantByContact(actor.getEmail());
            if (p == null) {
                switch (type) {
                    case "Student": p = new Student(name, contact, number, extra); break;
                    case "Faculty": p = new Faculty(name, contact, number, extra, extra2); break;
                    default:        p = new Others(name, contact, extra); break;
                }
            }

            // Permission, duplicate, capacity and "event already over" rules are enforced here.
            // A new participant is saved by the same call, together with the registration.
            Registration r = manager.register(actor, p, event);

            JOptionPane.showMessageDialog(this, p.getFullName() + " is registered for "
                    + event.getEventName() + ".\nRegistration ID: " + r.getRegistrationId());
            if (staff) clearForm();
            refresh();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
        } catch (StorageException ex) {
            JOptionPane.showMessageDialog(this, "The registration could not be saved:\n" + ex.getMessage(),
                    "Cannot complete action", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid or Missing Input",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearForm() {
        if (hasLockedProfile()) return;           // a Participant's own profile stays filled in
        txtRegName.setText("");
        if (staff) txtRegContact.setText("");
        txtRegNum.setText("");
        txtRegExtra.setText("");
        txtRegExtra2.setText("");
    }

    // ---------- History table + cancel ----------
    /** Registration history: staff see every registration, a Participant only their own. */
    private void loadTable(String keyword) {
        tableModel.setRowCount(0);
        shown.clear();
        if (manager == null) return;
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Registration r : manager.getVisibleRegistrations(actor)) {
            String name = r.getParticipant().getFullName();
            String type = r.getParticipant().getClass().getSimpleName();
            String eventName = r.getEvent().getEventName();
            String status = r.getStatus();
            if (k.isEmpty() || name.toLowerCase().contains(k) || eventName.toLowerCase().contains(k)
                    || type.toLowerCase().contains(k) || status.toLowerCase().contains(k)) {
                shown.add(r);
                tableModel.addRow(new Object[]{r.getRegistrationId(), name, eventName, type, status});
            }
        }
    }

    private void cancelSelected() {
        try {
            int row = regTable.getSelectedRow();
            if (row < 0 || row >= shown.size()) {
                JOptionPane.showMessageDialog(this, "Select a registration in the history table first.",
                        "No registration selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Registration r = shown.get(row);
            if (!r.isActive()) {
                JOptionPane.showMessageDialog(this, "That registration is already cancelled.",
                        "Cancel registration", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Cancel " + r.getParticipant().getFullName() + "'s registration for \""
                    + r.getEvent().getEventName() + "\"?",
                    "Cancel registration", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
            manager.cancelRegistration(actor, r);
            refresh();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
        } catch (StorageException ex) {
            JOptionPane.showMessageDialog(this, "The change could not be saved:\n" + ex.getMessage(),
                    "Cannot complete action", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot complete action", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblParticipantsReg = new javax.swing.JLabel();
        lblRegName = new javax.swing.JLabel();
        lblRegContact = new javax.swing.JLabel();
        lblRegNum = new javax.swing.JLabel();
        lblSelectEvent = new javax.swing.JLabel();
        lblParticipantsType = new javax.swing.JLabel();
        txtRegName = new javax.swing.JTextField();
        txtRegContact = new javax.swing.JTextField();
        cmbSelectEvent = new javax.swing.JComboBox<>();
        txtRegNum = new javax.swing.JTextField();
        cmbParticipantType = new javax.swing.JComboBox<>();
        btnRegister = new javax.swing.JButton();
        btnRegClear = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        regTable = new javax.swing.JTable();
        lblRegHistory = new javax.swing.JLabel();
        txtSearchReg = new javax.swing.JTextField();
        btnSearchReg = new javax.swing.JButton();
        lblSearchReg = new javax.swing.JLabel();
        lblRegExtra = new javax.swing.JLabel();
        txtRegExtra = new javax.swing.JTextField();
        lblRegExtra2 = new javax.swing.JLabel();
        txtRegExtra2 = new javax.swing.JTextField();

        setMaximumSize(new java.awt.Dimension(780, 550));
        setMinimumSize(new java.awt.Dimension(780, 550));
        setPreferredSize(new java.awt.Dimension(780, 550));

        lblParticipantsReg.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblParticipantsReg.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblParticipantsReg.setText("Participants Registration");
        lblParticipantsReg.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblRegName.setText("Name: ");

        lblRegContact.setText("BU/Email Address:");

        lblRegNum.setText("ID/Reg Number: ");

        lblSelectEvent.setText("Select Event: ");

        lblParticipantsType.setText("Participants Type: ");

        cmbSelectEvent.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seminar", "Workshop", " " }));

        cmbParticipantType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Student", "Faculty", "Others" }));

        btnRegister.setText("Register");

        btnRegClear.setText("Clear");

        regTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Registration ID", "Name", "Event", "Type"
            }
        ));
        jScrollPane1.setViewportView(regTable);

        lblRegHistory.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblRegHistory.setText("Registration History");

        btnSearchReg.setText("Search");

        lblSearchReg.setText("Search Registration:");

        lblRegExtra.setText("Program:");

        lblRegExtra2.setText("Position:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblParticipantsReg, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRegHistory)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblSearchReg)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSearchReg, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSearchReg)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 183, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblParticipantsType)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cmbParticipantType, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblRegContact)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtRegContact, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblRegName)
                                        .addGap(64, 64, 64)
                                        .addComponent(txtRegName, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblRegNum)
                                            .addComponent(lblSelectEvent)
                                            .addComponent(lblRegExtra)
                                            .addComponent(lblRegExtra2))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(txtRegNum, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                            .addComponent(cmbSelectEvent, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtRegExtra)
                                            .addComponent(txtRegExtra2))))
                                .addGap(188, 188, 188))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(btnRegister)
                                .addGap(18, 18, 18)
                                .addComponent(btnRegClear)
                                .addGap(307, 307, 307))))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblParticipantsReg)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRegName)
                    .addComponent(txtRegName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRegContact)
                    .addComponent(txtRegContact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblParticipantsType)
                    .addComponent(cmbParticipantType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRegNum)
                    .addComponent(txtRegNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSelectEvent)
                    .addComponent(cmbSelectEvent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRegExtra)
                    .addComponent(txtRegExtra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRegExtra2)
                    .addComponent(txtRegExtra2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRegister)
                    .addComponent(btnRegClear))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRegHistory)
                    .addComponent(txtSearchReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearchReg)
                    .addComponent(lblSearchReg))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnRegClear;
    private javax.swing.JButton btnRegister;
    private javax.swing.JButton btnSearchReg;
    private javax.swing.JComboBox<String> cmbParticipantType;
    private javax.swing.JComboBox<String> cmbSelectEvent;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblParticipantsReg;
    private javax.swing.JLabel lblParticipantsType;
    private javax.swing.JLabel lblRegContact;
    private javax.swing.JLabel lblRegExtra;
    private javax.swing.JLabel lblRegExtra2;
    private javax.swing.JLabel lblRegHistory;
    private javax.swing.JLabel lblRegName;
    private javax.swing.JLabel lblRegNum;
    private javax.swing.JLabel lblSearchReg;
    private javax.swing.JLabel lblSelectEvent;
    private javax.swing.JTable regTable;
    private javax.swing.JTextField txtRegContact;
    private javax.swing.JTextField txtRegExtra;
    private javax.swing.JTextField txtRegExtra2;
    private javax.swing.JTextField txtRegName;
    private javax.swing.JTextField txtRegNum;
    private javax.swing.JTextField txtSearchReg;
    // End of variables declaration//GEN-END:variables
}
