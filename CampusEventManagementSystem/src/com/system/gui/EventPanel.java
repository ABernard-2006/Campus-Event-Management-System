/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.system.gui;

import classModel.Event;
import classModel.Seminar;
import classModel.UserAccount;
import classModel.Venue;
import classModel.Workshop;
import com.system.service.EventManager;
import com.system.service.StorageException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

/**
 * Create and edit events (Workshop / Seminar).
 *
 * The same form is used for both: "Create Event" makes a new event; after an event is chosen in the
 * Event List, the form is filled with its values and the button becomes "Save Changes".
 * The generated NetBeans code below is untouched; everything handwritten is above it.
 *
 * @author Dhustine
 */
public class EventPanel extends javax.swing.JPanel implements Refreshable {
    private final EventManager manager;
    private final UserAccount actor;                       // who is using the panel (checked again inside EventManager)
    private final DefaultListModel<String> speakerModel = new DefaultListModel<>();
    private final JList<String> lstSpeakers = new JList<>(speakerModel);
    private Event editing;                                 // null = creating a new event
    private Runnable onSaved;                              // called after a successful create / update

    /** Creates new form EventPanel (used by the NetBeans designer only). */
    public EventPanel() {
        this(null, null);
    }

    public EventPanel(EventManager manager, UserAccount actor) {
        this.manager = manager;
        this.actor = actor;
        initComponents();
        setupForm();
    }

    /** Something to run after an event was created or saved (for example, switch to the list). */
    public void setOnSaved(Runnable onSaved) { this.onSaved = onSaved; }

    private void setupForm() {
        scrollpaneAddedSpeaker.setViewportView(lstSpeakers);
        SpinEventDate.setEditor(new JSpinner.DateEditor(SpinEventDate, "yyyy-MM-dd"));
        SpinEventType.setEditor(new JSpinner.DateEditor(SpinEventType, "hh:mm a"));
        cmbEventType.removeItem(" ");                      // blank placeholder item in the form is not a real type
        txtEventDuration.setText(String.valueOf(Event.DEFAULT_NEW_EVENT_DURATION_MINUTES));

        cmbEventType.addActionListener(e -> updateTypeFields());
        btnAddSpeaker.addActionListener(e -> addSpeaker());
        txtEventSpeaker.addActionListener(e -> addSpeaker());
        btnRemoveSpeaker.addActionListener(e -> removeSpeaker());
        btnCerateEvent.addActionListener(e -> saveEvent());
        btnEventClear.addActionListener(e -> clearOrCancel());
        txtEventVenue.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { suggestCapacity(); }
        });
        updateTypeFields();
    }

    // ---------- helpers ----------
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

    /** When the typed venue already exists, fill in its capacity so the two cannot disagree. */
    private void suggestCapacity() {
        if (manager == null || !txtMaxEventCap.getText().trim().isEmpty()) return;
        Venue known = manager.findVenueByName(txtEventVenue.getText());
        if (known != null) txtMaxEventCap.setText(String.valueOf(known.getCapacity()));
    }

    private void addSpeaker() {
        String s = txtEventSpeaker.getText().trim();
        if (s.isEmpty()) { showError("Enter a speaker name."); return; }
        if (s.contains(",")) { showError("Enter one speaker at a time, without commas."); return; }
        for (int i = 0; i < speakerModel.size(); i++) {
            if (speakerModel.get(i).equalsIgnoreCase(s)) { showError(s + " is already in the speaker list."); return; }
        }
        speakerModel.addElement(s);
        txtEventSpeaker.setText("");
        txtEventSpeaker.requestFocusInWindow();
    }

    private void removeSpeaker() {
        int i = lstSpeakers.getSelectedIndex();
        if (i < 0) { showError("Select a speaker to remove."); return; }
        speakerModel.remove(i);
    }

    private void clearOrCancel() {
        if (editing != null) exitEditMode();
        else clearForm();
    }

    private void clearForm() {
        txtEventName.setText("");
        txtEventVenue.setText("");
        txtMaxEventCap.setText("");
        txtEventSpeaker.setText("");
        txtEventTopic.setText("");
        txtEventMaterials.setText("");
        txtEventDuration.setText(String.valueOf(Event.DEFAULT_NEW_EVENT_DURATION_MINUTES));
        speakerModel.clear();
    }

    // ---------- edit mode ----------
    public boolean isEditing() { return editing != null; }

    /** Fills the form with an existing event; the button now saves changes instead of creating. */
    public void loadForEdit(Event event) {
        if (event == null) return;
        editing = event;
        clearForm();
        cmbEventType.setSelectedItem(event.getEventType());
        cmbEventType.setEnabled(false);                    // a Workshop cannot become a Seminar
        updateTypeFields();
        txtEventName.setText(event.getEventName());
        txtEventVenue.setText(event.getVenue().getVenueName());
        txtMaxEventCap.setText(String.valueOf(event.getVenue().getCapacity()));
        txtEventTopic.setText(event.getTopicOrSubject());
        txtEventMaterials.setText(event.getMaterials());
        txtEventDuration.setText(String.valueOf(event.getDurationMinutes()));
        for (String speaker : event.getSpeakerNames().split(",")) {
            if (!speaker.trim().isEmpty()) speakerModel.addElement(speaker.trim());
        }
        ZoneId zone = ZoneId.systemDefault();
        SpinEventDate.setValue(java.util.Date.from(event.getDate().atStartOfDay(zone).toInstant()));
        SpinEventType.setValue(java.util.Date.from(event.getDate().atTime(event.getStartTime()).atZone(zone).toInstant()));
        lblEventPanelTitle.setText("Edit Event: " + event.getEventName());
        btnCerateEvent.setText("Save Changes");
        btnEventClear.setText("Cancel Edit");
    }

    public void exitEditMode() {
        editing = null;
        cmbEventType.setEnabled(true);
        clearForm();
        lblEventPanelTitle.setText("Event Management ");
        btnCerateEvent.setText("Create Event");
        btnEventClear.setText("Clear");
    }

    /** If the event being edited was deleted meanwhile, leave edit mode. */
    @Override
    public void refresh() {
        if (editing != null && manager != null && manager.findEventById(editing.getEventId()) == null) exitEditMode();
    }

    // ---------- create / save ----------
    private void saveEvent() {
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
            if (eventName.length() > 100) throw new IllegalArgumentException("Event name must be 100 characters or fewer.");
            if (venueName.isEmpty()) throw new IllegalArgumentException("Venue is required.");
            if (venueName.length() > 60) throw new IllegalArgumentException("Venue name must be 60 characters or fewer.");
            if (!type.equals("Workshop") && !type.equals("Seminar"))
                throw new IllegalArgumentException("Choose an event type (Workshop or Seminar).");
            boolean dateChanged = editing == null || !editing.getDate().equals(date);
            if (dateChanged && date.isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Event date cannot be in the past.");

            int durationMinutes = parseDuration(txtEventDuration.getText());

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
            List<String> names = new ArrayList<>(java.util.Collections.list(speakerModel.elements()));
            String speakers = String.join(", ", names);

            // Reuse the saved venue with this name, so two events in "Room 101" share ONE venue record.
            Venue venue = manager.findVenueByName(venueName);
            if (venue == null) {
                venue = new Venue(venueName, "TBA", capacity);
            } else if (venue.getCapacity() != capacity) {
                throw new IllegalArgumentException("The venue \"" + venue.getVenueName() + "\" already exists with a capacity of "
                        + venue.getCapacity() + ". Use that capacity or choose a different venue name.");
            }

            String doneMessage;
            if (editing == null) {
                Event event;
                if (type.equals("Workshop")) {
                    event = new Workshop(eventName, date, time, durationMinutes, venue, topic, materials, speakers);
                } else {
                    event = new Seminar(eventName, date, time, durationMinutes, venue, speakers, topic);
                }
                manager.addEvent(actor, event);            // permission, venue and schedule-conflict rules are enforced here
                doneMessage = "Created: " + event.getEventName() + "\nRuns " + event.getTimeRangeText()
                        + " (" + event.getDurationText() + ")";
                clearForm();
            } else {
                manager.updateEvent(actor, editing, eventName, date, time, durationMinutes, venue, speakers, topic, materials);
                doneMessage = "Saved changes to: " + editing.getEventName() + "\nRuns " + editing.getTimeRangeText()
                        + " (" + editing.getDurationText() + ")";
                exitEditMode();
            }
            JOptionPane.showMessageDialog(this, doneMessage);
            if (onSaved != null) onSaved.run();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
        } catch (StorageException ex) {
            showError("The event could not be saved to the data files:\n" + ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Turns the Duration box into minutes. Explains exactly what is wrong when it is empty, not a
     * whole number, zero/negative, or longer than Event.MAX_DURATION_MINUTES. (EventManager and Event
     * check the range again, so a bad value cannot get in even without this form.)
     */
    static int parseDuration(String text) {          // package-private so the tests can call it
        String t = text == null ? "" : text.trim();
        if (t.isEmpty())
            throw new IllegalArgumentException("Duration is required. Enter the length of the event in minutes, for example 90.");
        if (!t.matches("[+-]?\\d+"))
            throw new IllegalArgumentException("Duration must be a whole number of minutes, for example 90 "
                    + "(no letters, decimals or spaces). You entered: \"" + t + "\".");
        java.math.BigInteger value = new java.math.BigInteger(t.startsWith("+") ? t.substring(1) : t);
        if (value.signum() <= 0)
            throw new IllegalArgumentException("Duration must be greater than zero minutes.");
        if (value.compareTo(java.math.BigInteger.valueOf(Event.MAX_DURATION_MINUTES)) > 0)
            throw new IllegalArgumentException("Duration cannot be more than " + Event.MAX_DURATION_MINUTES
                    + " minutes (24 hours).");
        return value.intValue();
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
        lblEventDuration = new javax.swing.JLabel();
        txtEventDuration = new javax.swing.JTextField();
        lblEventDurationUnit = new javax.swing.JLabel();

        setBackground(new java.awt.Color(247, 247, 252));
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

        lblEventDuration.setText("Duration:");

        txtEventDuration.setText("60");
        txtEventDuration.setToolTipText("Length of the event in whole minutes (1 to 1440). Example: 90 = 1 hour 30 minutes.");

        lblEventDurationUnit.setText("minutes (1 - 1440)");

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
                                .addComponent(lblEventDuration)
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
                                .addComponent(txtEventVenue, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addComponent(txtEventDuration, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(lblEventDurationUnit))))))
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
                    .addComponent(lblEventDuration)
                    .addComponent(txtEventDuration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEventDurationUnit))
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
    private javax.swing.JLabel lblEventDuration;
    private javax.swing.JLabel lblEventDurationUnit;
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
    private javax.swing.JTextField txtEventDuration;
    private javax.swing.JTextField txtEventMaterials;
    private javax.swing.JTextField txtEventName;
    private javax.swing.JTextField txtEventSpeaker;
    private javax.swing.JTextField txtEventTopic;
    private javax.swing.JTextField txtEventVenue;
    private javax.swing.JTextField txtMaxEventCap;
    // End of variables declaration//GEN-END:variables
}
