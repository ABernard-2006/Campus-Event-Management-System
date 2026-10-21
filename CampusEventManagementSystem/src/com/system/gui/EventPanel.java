/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.system.gui;

import classModel.*;
import java.time.*;
import classModel.Event;
import classModel.Seminar;
import classModel.Workshop;
import com.system.service.EventManager;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import java.time.ZoneId;
import classModel.Venue;

/**
 *
 * @author Dhustine
 */
public class EventPanel extends javax.swing.JPanel {
private final EventManager manager;
private final DefaultListModel<String> speakerModel = new DefaultListModel<>();
private final JList<String> lstSpeakers = new JList<>(speakerModel);

    /**
     * Creates new form EventPanel
     */
    public EventPanel() {
        this(null);
    }
    
    public EventPanel(EventManager manager) {   // used by MainFrame
    this.manager = manager;
    initComponents();
    setupForm();
    }
    
    private void setupForm() {
    scrollpaneAddedSpeaker.setViewportView(lstSpeakers);
    SpinEventDate.setEditor(new JSpinner.DateEditor(SpinEventDate, "yyyy-MM-dd"));
    SpinEventType.setEditor(new JSpinner.DateEditor(SpinEventType, "hh:mm a"));

    cmbEventType.addActionListener(e -> updateTypeFields());
    btnAddSpeaker.addActionListener(e -> addSpeaker());
    btnRemoveSpeaker.addActionListener(e -> removeSpeaker());
    btnCerateEvent.addActionListener(e -> createEvent());
    btnEventClear.addActionListener(e -> clearForm());
    updateTypeFields();
    }

    private String selectedType() {
    Object o = cmbEventType.getSelectedItem();
    return o == null ? "" : o.toString().trim();
}

/** Dynamic switching: Workshop shows Topic + Materials, Seminar shows Subject only. */
private void updateTypeFields() {
    boolean workshop = selectedType().equals("Workshop");
    lblEventTopic.setText(workshop ? "Topic:" : "Subject:");
    lblEventMaterials.setVisible(workshop);
    txtEventMaterials.setVisible(workshop);
}

private void addSpeaker() {
    String s = txtEventSpeaker.getText().trim();
    if (s.isEmpty()) { showError("Enter a speaker name."); return; }
    speakerModel.addElement(s);
    txtEventSpeaker.setText("");
    txtEventSpeaker.requestFocusInWindow();
}

private void removeSpeaker() {
    int i = lstSpeakers.getSelectedIndex();
    if (i < 0) { showError("Select a speaker to remove."); return; }
    speakerModel.remove(i);
}

private void clearForm() {
    txtEventName.setText("");
    txtEventVenue.setText("");
    txtMaxEventCap.setText("");
    txtEventSpeaker.setText("");
    txtEventTopic.setText("");
    txtEventMaterials.setText("");
    speakerModel.clear();
}

private void createEvent() {
    try {
         if (manager == null) throw new IllegalStateException("No EventManager connected to this panel.");

        // spinners hold java.util.Date -> convert to LocalDate / LocalTime
        java.util.Date d = (java.util.Date) SpinEventDate.getValue();
        java.util.Date t = (java.util.Date) SpinEventType.getValue();
        LocalDate date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalTime time = t.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                          .withSecond(0).withNano(0);

        String eventName = txtEventName.getText().trim();
        String venueName = txtEventVenue.getText().trim();
        String topic = txtEventTopic.getText().trim();
        String materials = txtEventMaterials.getText().trim();
        String type = selectedType();

        if (eventName.isEmpty()) throw new IllegalArgumentException("Event name is required.");
        if (venueName.isEmpty()) throw new IllegalArgumentException("Venue is required.");
        if (!type.equals("Workshop") && !type.equals("Seminar"))
            throw new IllegalArgumentException("Choose an event type (Workshop or Seminar).");
        if (date.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Event date cannot be in the past.");

        int capacity;
        try {
            String capacityText = txtMaxEventCap.getText().trim();
            if (capacityText.isEmpty()) throw new NumberFormatException();
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Max capacity is required and must be a whole number.");
        }
        if (capacity <= 0) throw new IllegalArgumentException("Max capacity must be greater than zero.");

        if (speakerModel.isEmpty()) throw new IllegalArgumentException("Add at least one speaker.");
        if (type.equals("Workshop")) {
            if (topic.isEmpty()) throw new IllegalArgumentException("Workshop topic is required.");
            if (materials.isEmpty()) throw new IllegalArgumentException("Workshop materials are required.");
        } else if (topic.isEmpty()) {
            throw new IllegalArgumentException("Seminar subject is required.");
        }
        String speakers = String.join(", ", java.util.Collections.list(speakerModel.elements()));

        Venue venue = new Venue(venueName, "TBA", capacity);

        Event event;
        if (type.equals("Workshop")) {
            event = new Workshop(eventName, date, time, venue, topic, materials, speakers);
        } else {
            event = new Seminar(eventName, date, time, venue, speakers, topic);
        }

        manager.addEvent(event);   // conflict rule is enforced here
        JOptionPane.showMessageDialog(this, "Created: " + event.getEventName());
        clearForm();
    } catch (IllegalArgumentException | IllegalStateException ex) {
        showError(ex.getMessage());
    }
}

private void showError(String msg) {
    JOptionPane.showMessageDialog(this, msg, "Cannot complete action", JOptionPane.ERROR_MESSAGE);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblEventPanelTitle = new javax.swing.JLabel();
        txtEventName = new javax.swing.JTextField();
        cmbEventType = new javax.swing.JComboBox<>();
        txtEventVenue = new javax.swing.JTextField();
        btnCerateEvent = new javax.swing.JButton();
        btnEventClear = new javax.swing.JButton();
        lblEventName = new javax.swing.JLabel();
        lblEventType = new javax.swing.JLabel();
        lblEventDate = new javax.swing.JLabel();
        lblEventVenue = new javax.swing.JLabel();
        lblEventSpeaker = new javax.swing.JLabel();
        txtEventSpeaker = new javax.swing.JTextField();
        btnAddSpeaker = new javax.swing.JButton();
        scrollpaneAddedSpeaker = new javax.swing.JScrollPane();
        btnRemoveSpeaker = new javax.swing.JButton();
        lblAddedSpeaker = new javax.swing.JLabel();
        lblMaxEventCap = new javax.swing.JLabel();
        txtMaxEventCap = new javax.swing.JTextField();
        SpinEventDate = new javax.swing.JSpinner();
        SpinEventType = new javax.swing.JSpinner();
        lblEventTime = new javax.swing.JLabel();
        lblEventTopic = new javax.swing.JLabel();
        txtEventTopic = new javax.swing.JTextField();
        lblEventMaterials = new javax.swing.JLabel();
        txtEventMaterials = new javax.swing.JTextField();

        setMaximumSize(new java.awt.Dimension(780, 550));
        setMinimumSize(new java.awt.Dimension(780, 550));
        setPreferredSize(new java.awt.Dimension(780, 550));

        lblEventPanelTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblEventPanelTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblEventPanelTitle.setText("Event Management ");

        txtEventName.addActionListener(this::txtEventNameActionPerformed);

        cmbEventType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Workshop", "Seminar", " " }));

        btnCerateEvent.setText("Create Event");

        btnEventClear.setText("Clear");

        lblEventName.setText("Event name:");

        lblEventType.setText("Event type: ");

        lblEventDate.setText("Date: ");

        lblEventVenue.setText("Venue: ");

        lblEventSpeaker.setText("Speaker name:");

        txtEventSpeaker.addActionListener(this::txtEventSpeakerActionPerformed);

        btnAddSpeaker.setText("+ Add Speaker");

        btnRemoveSpeaker.setText("- Remove Speaker");

        lblAddedSpeaker.setText("Added Speaker/s: ");

        lblMaxEventCap.setText("Max Capacity:");

        txtMaxEventCap.addActionListener(this::txtMaxEventCapActionPerformed);

        SpinEventDate.setModel(new javax.swing.SpinnerDateModel());

        SpinEventType.setModel(new javax.swing.SpinnerDateModel(new java.util.Date(1792478371647L), null, null, java.util.Calendar.MINUTE));

        lblEventTime.setText("Time:");

        lblEventTopic.setText("Topic:");

        txtEventTopic.addActionListener(this::txtEventTopicActionPerformed);

        lblEventMaterials.setText("Materials:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lblEventPanelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 780, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(97, 97, 97)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(lblMaxEventCap)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblAddedSpeaker)
                                .addComponent(lblEventSpeaker))
                            .addGap(26, 26, 26)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtMaxEventCap, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(txtEventSpeaker, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(scrollpaneAddedSpeaker, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnRemoveSpeaker)
                                        .addComponent(btnEventClear, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAddSpeaker, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnCerateEvent, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblEventName)
                                .addComponent(lblEventType)
                                .addComponent(lblEventDate)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(lblEventVenue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblEventMaterials)
                                    .addComponent(lblEventTopic, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(58, 58, 58)
                                    .addComponent(SpinEventDate, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(0, 0, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(txtEventTopic)
                                            .addComponent(txtEventMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(txtEventName, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cmbEventType, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(121, 121, 121)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(lblEventTime)
                                    .addGap(18, 18, 18)
                                    .addComponent(SpinEventType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(78, 78, 78))
                                .addComponent(txtEventVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(lblEventPanelTitle)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEventName)
                    .addComponent(txtEventName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEventType)
                    .addComponent(cmbEventType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEventDate)
                    .addComponent(SpinEventDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SpinEventType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEventTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEventVenue)
                    .addComponent(txtEventVenue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtEventTopic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEventTopic))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEventMaterials)
                    .addComponent(txtEventMaterials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMaxEventCap)
                    .addComponent(txtMaxEventCap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtEventSpeaker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEventSpeaker)
                    .addComponent(btnAddSpeaker))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblAddedSpeaker)
                            .addComponent(btnRemoveSpeaker))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCerateEvent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEventClear))
                    .addComponent(scrollpaneAddedSpeaker, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(158, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtEventNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEventNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventNameActionPerformed

    private void txtEventSpeakerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEventSpeakerActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventSpeakerActionPerformed

    private void txtMaxEventCapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMaxEventCapActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtMaxEventCapActionPerformed

    private void txtEventTopicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEventTopicActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventTopicActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner SpinEventDate;
    private javax.swing.JSpinner SpinEventType;
    private javax.swing.JButton btnAddSpeaker;
    private javax.swing.JButton btnCerateEvent;
    private javax.swing.JButton btnEventClear;
    private javax.swing.JButton btnRemoveSpeaker;
    private javax.swing.JComboBox<String> cmbEventType;
    private javax.swing.JLabel lblAddedSpeaker;
    private javax.swing.JLabel lblEventDate;
    private javax.swing.JLabel lblEventMaterials;
    private javax.swing.JLabel lblEventName;
    private javax.swing.JLabel lblEventPanelTitle;
    private javax.swing.JLabel lblEventSpeaker;
    private javax.swing.JLabel lblEventTime;
    private javax.swing.JLabel lblEventTopic;
    private javax.swing.JLabel lblEventType;
    private javax.swing.JLabel lblEventVenue;
    private javax.swing.JLabel lblMaxEventCap;
    private javax.swing.JScrollPane scrollpaneAddedSpeaker;
    private javax.swing.JTextField txtEventMaterials;
    private javax.swing.JTextField txtEventName;
    private javax.swing.JTextField txtEventSpeaker;
    private javax.swing.JTextField txtEventTopic;
    private javax.swing.JTextField txtEventVenue;
    private javax.swing.JTextField txtMaxEventCap;
    // End of variables declaration//GEN-END:variables
}
