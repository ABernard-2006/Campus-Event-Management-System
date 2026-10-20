/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.system.gui;

import classModel.*;
import com.system.service.EventManager;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author Dhustine
 */
public class RegistrationPanel extends javax.swing.JPanel {
private final EventManager manager;
private final List<Event> eventChoices = new ArrayList<>();   // same order as the combo box items
private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[][]{}, new String[]{"Registration ID", "Name", "Event", "Type"}) {
    @Override public boolean isCellEditable(int row, int col) { return false; }
};

    /**
     * Creates new form RegistrationPanel
     */
    public RegistrationPanel() {
        this(null);
    }

    public RegistrationPanel(EventManager manager) {     // used by MainFrame
    this.manager = manager;
    initComponents();
    setupForm();
}

private void setupForm() {
    regTable.setModel(tableModel);
    lblRegExtra2.setText("Position:");

    cmbParticipantType.addActionListener(e -> updateTypeFields());
    btnRegister.addActionListener(e -> registerParticipant());
    btnRegClear.addActionListener(e -> clearForm());
    btnSearchReg.addActionListener(e -> loadTable(txtSearchReg.getText()));
    txtSearchReg.addActionListener(e -> loadTable(txtSearchReg.getText()));

    loadEvents();
    updateTypeFields();
    loadTable("");
}

/** Fills the event combo box from the manager (replaces the hard-coded items). */
private void loadEvents() {
    eventChoices.clear();
    cmbSelectEvent.removeAllItems();
    if (manager == null) return;
    for (Event e : manager.getAllEvents()) {
        eventChoices.add(e);
        cmbSelectEvent.addItem(e.getEventId() + " - " + e.getEventName() + " (" + e.getEventType() + ")");
    }
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

/** Looks for a participant that is already in the system so nobody is duplicated. */
private Participant findExisting(String type, String number, String name) {
    for (Participant p : manager.getAllParticipants()) {
        if (type.equals("Student") && p instanceof Student
                && ((Student) p).getStudentNumber().equalsIgnoreCase(number)) return p;
        if (type.equals("Faculty") && p instanceof Faculty
                && ((Faculty) p).getFacultyNumber().equalsIgnoreCase(number)) return p;
        if (type.equals("Others") && p instanceof Others
                && p.getFullName().equalsIgnoreCase(name)) return p;
    }
    return null;
}

private void registerParticipant() {
    try {
        if (manager == null) throw new IllegalStateException("No EventManager connected to this panel.");

        int idx = cmbSelectEvent.getSelectedIndex();
        if (idx < 0 || idx >= eventChoices.size())
            throw new IllegalArgumentException("No event selected. Create an event first.");
        Event event = eventChoices.get(idx);

        String type = selectedType();
        String name = txtRegName.getText().trim();
        String contact = txtRegContact.getText().trim();
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

        Participant p = findExisting(type, number, name);
        boolean isNew = (p == null);
        if (isNew) {
            switch (type) {
                case "Student": p = new Student(name, contact, number, extra); break;
                case "Faculty": p = new Faculty(name, contact, number, extra, extra2); break;
                case "Others":  p = new Others(name, contact, extra); break;
                default: throw new IllegalArgumentException("Choose a participant type.");
            }
        }

        Registration r = manager.register(p, event);   // capacity / duplicate rules enforced here
        if (isNew) manager.addParticipant(p);

        JOptionPane.showMessageDialog(this, p.getFullName() + " is registered for "
                + event.getEventName() + ".\nRegistration ID: " + r.getRegistrationId());
        clearForm();
        loadTable("");
    } catch (IllegalArgumentException | IllegalStateException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid or Missing Input",
                JOptionPane.WARNING_MESSAGE);
    }
}

private void clearForm() {
    txtRegName.setText("");
    txtRegContact.setText("");
    txtRegNum.setText("");
    txtRegExtra.setText("");
    txtRegExtra2.setText("");
}

/** Registration history table: every registration for every event, optionally filtered. */
private void loadTable(String keyword) {
    tableModel.setRowCount(0);
    if (manager == null) return;
    String k = keyword == null ? "" : keyword.trim().toLowerCase();
    for (Event e : manager.getAllEvents()) {
        for (Registration r : manager.getRegistrationsFor(e)) {
            String name = r.getParticipant().getFullName();
            String type = r.getParticipant().getClass().getSimpleName();
            String eventName = e.getEventName() + (r.isActive() ? "" : " [Cancelled]");
            if (k.isEmpty() || name.toLowerCase().contains(k)
                    || eventName.toLowerCase().contains(k) || type.toLowerCase().contains(k)) {
                tableModel.addRow(new Object[]{r.getRegistrationId(), name, eventName, type});
            }
        }
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
