/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.system.gui;

import classModel.Event;
import classModel.UserAccount;
import com.system.service.EventManager;
import com.system.service.EventReportRow;
import com.system.service.ReportTotals;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

/**
 * Event and attendance report (Administrators and Organizers only).
 *
 * Table columns:  Event Name | Total Registration | Attended | Student | Faculty | Others
 *  - Total Registration = valid (not cancelled) registrations of the event
 *  - Attended           = valid registrations marked present
 *  - Student/Faculty/Others = valid registrations by participant category
 * Every number comes from EventManager.buildReport(...), which counts the real Registration,
 * Participant and Attendance objects. Double-click a row to see the event's own
 * displayDetails() text (each Event subclass describes itself differently = polymorphism).
 *
 * The generated NetBeans code below is untouched; everything handwritten is above it.
 *
 * @author Dhustine
 */
public class ReportPanel extends javax.swing.JPanel implements Refreshable {
    private static final String ALL_EVENTS = "All Events";
    private static final String ALL_TYPES = "All Types";

    private final EventManager manager;
    private final UserAccount actor;
    private final List<Event> eventChoices = new ArrayList<>();   // index i  <->  combo item i + 1 (item 0 = all)
    private final List<EventReportRow> rows = new ArrayList<>();  // same order as the table rows
    private boolean updating;                                     // true while the screen itself changes controls
    private boolean dateRangeEditedByUser;                        // false = range follows the saved events

    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[][]{},
            new String[]{"Event Name", "Total Registration", "Attended", "Student", "Faculty", "Others"}) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
    };

    /** Creates new form ReportPanel (used by the NetBeans designer only). */
    public ReportPanel() {
        this(null, null);
    }

    public ReportPanel(EventManager manager, UserAccount actor) {
        this.manager = manager;
        this.actor = actor;
        initComponents();
        setupReport();
    }

    private void setupReport() {
        reportTable.setModel(tableModel);
        reportTable.setAutoCreateRowSorter(true);
        reportTable.setRowHeight(22);

        spinMinDateRange.setEditor(new JSpinner.DateEditor(spinMinDateRange, "yyyy-MM-dd"));
        spinMaxDateRange.setEditor(new JSpinner.DateEditor(spinMaxDateRange, "yyyy-MM-dd"));

        cmbReportType.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{ALL_TYPES, EventManager.TYPE_WORKSHOP, EventManager.TYPE_SEMINAR}));

        // The Generate button, both combo boxes and both date spinners are wired by the form
        // (btnGanerateReportActionPerformed, cmbSelectEventsActionPerformed, cmbReportTypeActionPerformed,
        //  spinMinDateRangeStateChanged, spinMaxDateRangeStateChanged).
        reportTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) showDetails();
            }
        });
        refresh();
    }

    // ---------- date helpers ----------
    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Object spinnerValue) {
        return ((Date) spinnerValue).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Re-reads the events: rebuilds the event list, widens the date range if needed, and redraws the table. */
    @Override
    public void refresh() {
        if (manager == null) return;
        updating = true;
        try {
            int keepId = -1;
            int sel = cmbSelectEvents.getSelectedIndex();
            if (sel > 0 && sel - 1 < eventChoices.size()) keepId = eventChoices.get(sel - 1).getEventId();

            eventChoices.clear();
            cmbSelectEvents.removeAllItems();
            cmbSelectEvents.addItem(ALL_EVENTS);
            List<Event> sorted = manager.getEventsChronological();
            int select = 0;
            for (Event e : sorted) {
                if (e.getEventId() == keepId) select = eventChoices.size() + 1;
                eventChoices.add(e);
                cmbSelectEvents.addItem(e.getEventId() + " - " + e.getEventName());
            }
            cmbSelectEvents.setSelectedIndex(select);

            if (!dateRangeEditedByUser) {            // until the user picks dates, the range covers every event
                LocalDate[] range = manager.getEventDateRange();      // {earliest, latest} event date
                spinMinDateRange.setValue(toDate(range[0]));
                spinMaxDateRange.setValue(toDate(range[1]));
            }
        } finally {
            updating = false;
        }
        generate();
    }

    /** Builds the table and the three summary tiles from the CURRENT filter settings. */
    private void generate() {
        if (manager == null || updating) return;
        tableModel.setRowCount(0);
        rows.clear();
        try {
            int sel = cmbSelectEvents.getSelectedIndex();
            Event only = (sel > 0 && sel - 1 < eventChoices.size()) ? eventChoices.get(sel - 1) : null;
            String type = String.valueOf(cmbReportType.getSelectedItem());
            LocalDate from = toLocalDate(spinMinDateRange.getValue());
            LocalDate to = toLocalDate(spinMaxDateRange.getValue());

            rows.addAll(manager.buildReport(actor, type, only, from, to));
            for (EventReportRow r : rows) {
                tableModel.addRow(new Object[]{r.getEventName(), r.getTotalRegistration(), r.getAttended(),
                    r.getStudents(), r.getFaculty(), r.getOthers()});
            }
            ReportTotals totals = EventManager.summarizeReport(rows);      // the numbers come from the service
            lblTotalRegNum.setText(String.valueOf(totals.getTotalRegistrations()));
            lblTotalAttendNum.setText(String.valueOf(totals.getTotalAttended()));
            lblTotalAttendRateNum.setText(String.format("%.1f%%", totals.getAttendanceRatePercent()));
            lblGenerateReport.setText("Generated Report Table (" + rows.size() + " event" + (rows.size() == 1 ? "" : "s") + ")");
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            lblTotalRegNum.setText("0");
            lblTotalAttendNum.setText("0");
            lblTotalAttendRateNum.setText("0.0%");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Report filter", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Double-click a row: shows the event's own description (Workshop and Seminar print different details). */
    private void showDetails() {
        int viewRow = reportTable.getSelectedRow();
        if (viewRow < 0) return;
        int row = reportTable.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= rows.size()) return;
        try {
            JTextArea text = new JTextArea(manager.describeEvent(actor, rows.get(row).getEvent()), 12, 40);
            text.setEditable(false);
            text.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
            JOptionPane.showMessageDialog(this, new JScrollPane(text), "Event details", JOptionPane.PLAIN_MESSAGE);
        } catch (SecurityException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Event details", JOptionPane.WARNING_MESSAGE);
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

        lblEventReports = new javax.swing.JLabel();
        lblFilterControls = new javax.swing.JLabel();
        lblSelectEvents = new javax.swing.JLabel();
        lblReportType = new javax.swing.JLabel();
        lblDataRange = new javax.swing.JLabel();
        cmbSelectEvents = new javax.swing.JComboBox<>();
        cmbReportType = new javax.swing.JComboBox<>();
        btnGanerateReport = new javax.swing.JButton();
        spinMinDateRange = new javax.swing.JSpinner();
        spinMaxDateRange = new javax.swing.JSpinner();
        lblTo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        reportTable = new javax.swing.JTable();
        lblGenerateReport = new javax.swing.JLabel();
        panelTotalReg = new javax.swing.JPanel();
        lblTatalReg = new javax.swing.JLabel();
        lblTotalRegNum = new javax.swing.JLabel();
        panelTotalAttend = new javax.swing.JPanel();
        lblTotalAttend = new javax.swing.JLabel();
        lblTotalAttendNum = new javax.swing.JLabel();
        panelTotalAttendRateNum = new javax.swing.JPanel();
        lblTotalAttendRate = new javax.swing.JLabel();
        lblTotalAttendRateNum = new javax.swing.JLabel();

        setMaximumSize(new java.awt.Dimension(780, 550));
        setMinimumSize(new java.awt.Dimension(780, 550));

        lblEventReports.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblEventReports.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblEventReports.setText("Event & Attendance Reports");

        lblFilterControls.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblFilterControls.setText("Filter and Controls");

        cmbSelectEvents.addActionListener(this::cmbSelectEventsActionPerformed);

        cmbReportType.addActionListener(this::cmbReportTypeActionPerformed);

        lblSelectEvents.setText("Select Event:");

        lblReportType.setText("Report Type:");

        lblDataRange.setText("Date Range:");

        btnGanerateReport.setText("Generate Report");
        btnGanerateReport.addActionListener(this::btnGanerateReportActionPerformed);

        spinMinDateRange.setModel(new javax.swing.SpinnerDateModel());
        spinMinDateRange.addChangeListener(this::spinMinDateRangeStateChanged);

        spinMaxDateRange.setModel(new javax.swing.SpinnerDateModel());
        spinMaxDateRange.addChangeListener(this::spinMaxDateRangeStateChanged);

        lblTo.setText("to");

        reportTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Event Name", "Total Registration", "Attended", "Student", "Faculty", "Others"
            }
        ));
        jScrollPane1.setViewportView(reportTable);

        lblGenerateReport.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblGenerateReport.setText("Generated Report Table");

        panelTotalReg.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblTatalReg.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTatalReg.setText("Total Registered: ");

        lblTotalRegNum.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTotalRegNum.setText("0");

        javax.swing.GroupLayout panelTotalRegLayout = new javax.swing.GroupLayout(panelTotalReg);
        panelTotalReg.setLayout(panelTotalRegLayout);
        panelTotalRegLayout.setHorizontalGroup(
            panelTotalRegLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTotalRegLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblTatalReg)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTotalRegNum)
                .addContainerGap(26, Short.MAX_VALUE))
        );
        panelTotalRegLayout.setVerticalGroup(
            panelTotalRegLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTotalRegLayout.createSequentialGroup()
                .addContainerGap(39, Short.MAX_VALUE)
                .addGroup(panelTotalRegLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTatalReg)
                    .addComponent(lblTotalRegNum))
                .addGap(31, 31, 31))
        );

        panelTotalAttend.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblTotalAttend.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTotalAttend.setText("Total Attended:");

        lblTotalAttendNum.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTotalAttendNum.setText("0");

        javax.swing.GroupLayout panelTotalAttendLayout = new javax.swing.GroupLayout(panelTotalAttend);
        panelTotalAttend.setLayout(panelTotalAttendLayout);
        panelTotalAttendLayout.setHorizontalGroup(
            panelTotalAttendLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTotalAttendLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(lblTotalAttend)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTotalAttendNum)
                .addContainerGap(32, Short.MAX_VALUE))
        );
        panelTotalAttendLayout.setVerticalGroup(
            panelTotalAttendLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTotalAttendLayout.createSequentialGroup()
                .addContainerGap(36, Short.MAX_VALUE)
                .addGroup(panelTotalAttendLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalAttend)
                    .addComponent(lblTotalAttendNum))
                .addGap(34, 34, 34))
        );

        panelTotalAttendRateNum.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblTotalAttendRate.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTotalAttendRate.setText("Attendance Rate:");

        lblTotalAttendRateNum.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTotalAttendRateNum.setText("0.0%");

        javax.swing.GroupLayout panelTotalAttendRateNumLayout = new javax.swing.GroupLayout(panelTotalAttendRateNum);
        panelTotalAttendRateNum.setLayout(panelTotalAttendRateNumLayout);
        panelTotalAttendRateNumLayout.setHorizontalGroup(
            panelTotalAttendRateNumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTotalAttendRateNumLayout.createSequentialGroup()
                .addContainerGap(8, Short.MAX_VALUE)
                .addComponent(lblTotalAttendRate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTotalAttendRateNum)
                .addGap(12, 12, 12))
        );
        panelTotalAttendRateNumLayout.setVerticalGroup(
            panelTotalAttendRateNumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTotalAttendRateNumLayout.createSequentialGroup()
                .addContainerGap(40, Short.MAX_VALUE)
                .addGroup(panelTotalAttendRateNumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalAttendRate)
                    .addComponent(lblTotalAttendRateNum))
                .addGap(33, 33, 33))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblEventReports, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(panelTotalReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(panelTotalAttend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(panelTotalAttendRateNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblGenerateReport)))
                .addGap(0, 24, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(121, 121, 121)
                        .addComponent(btnGanerateReport))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblSelectEvents)
                            .addComponent(lblFilterControls)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblDataRange)
                                .addComponent(lblReportType)))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(cmbReportType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(spinMinDateRange)
                            .addComponent(cmbSelectEvents, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMaxDateRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(147, 147, 147))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(lblEventReports)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFilterControls)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblSelectEvents)
                            .addComponent(cmbSelectEvents, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblReportType)
                            .addComponent(cmbReportType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblDataRange)
                            .addComponent(spinMinDateRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblTo)
                            .addComponent(spinMaxDateRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(134, 134, 134)
                        .addComponent(btnGanerateReport)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(panelTotalReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(panelTotalAttend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(panelTotalAttendRateNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(lblGenerateReport)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // ---------- Generated handlers (wired in initComponents): each one re-reads the filters and calls generate() ----------
    private void btnGanerateReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGanerateReportActionPerformed
        generate();
    }//GEN-LAST:event_btnGanerateReportActionPerformed

    private void cmbSelectEventsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSelectEventsActionPerformed
        if (!updating) generate();
    }//GEN-LAST:event_cmbSelectEventsActionPerformed

    private void cmbReportTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbReportTypeActionPerformed
        if (!updating) generate();
    }//GEN-LAST:event_cmbReportTypeActionPerformed

    private void spinMinDateRangeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinMinDateRangeStateChanged
        dateRangeChanged();
    }//GEN-LAST:event_spinMinDateRangeStateChanged

    private void spinMaxDateRangeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinMaxDateRangeStateChanged
        dateRangeChanged();
    }//GEN-LAST:event_spinMaxDateRangeStateChanged

    /** The user picked a date: from now on the range no longer follows the saved events. */
    private void dateRangeChanged() {
        if (updating) return;
        dateRangeEditedByUser = true;
        generate();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGanerateReport;
    private javax.swing.JComboBox<String> cmbReportType;
    private javax.swing.JComboBox<String> cmbSelectEvents;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblDataRange;
    private javax.swing.JLabel lblEventReports;
    private javax.swing.JLabel lblFilterControls;
    private javax.swing.JLabel lblGenerateReport;
    private javax.swing.JLabel lblReportType;
    private javax.swing.JLabel lblSelectEvents;
    private javax.swing.JLabel lblTatalReg;
    private javax.swing.JLabel lblTo;
    private javax.swing.JLabel lblTotalAttend;
    private javax.swing.JLabel lblTotalAttendNum;
    private javax.swing.JLabel lblTotalAttendRate;
    private javax.swing.JLabel lblTotalAttendRateNum;
    private javax.swing.JLabel lblTotalRegNum;
    private javax.swing.JPanel panelTotalAttend;
    private javax.swing.JPanel panelTotalAttendRateNum;
    private javax.swing.JPanel panelTotalReg;
    private javax.swing.JTable reportTable;
    private javax.swing.JSpinner spinMaxDateRange;
    private javax.swing.JSpinner spinMinDateRange;
    // End of variables declaration//GEN-END:variables
}
