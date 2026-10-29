package com.system.gui;

import classModel.Event;
import classModel.UserAccount;
import com.system.service.EventManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * The "Manage Event" screen: three tabs that all use the same shared EventManager.
 *   1. Create / Edit  - the existing EventPanel form
 *   2. Event List     - edit or delete events
 *   3. Attendance     - mark who attended
 */
public class ManageEventsPanel extends JPanel implements Refreshable {
    private final EventPanel eventPanel;
    private final EventListPanel listPanel;
    private final AttendancePanel attendancePanel;
    private final JTabbedPane tabs = new JTabbedPane();

    public ManageEventsPanel(EventManager manager, UserAccount actor) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(780, 580));
        setBackground(new java.awt.Color(247, 247, 252));

        eventPanel = new EventPanel(manager, actor);
        listPanel = new EventListPanel(manager, actor, this::startEditing);
        attendancePanel = new AttendancePanel(manager, actor);
        eventPanel.setOnSaved(() -> tabs.setSelectedIndex(1));   // after saving, show the list again

        tabs.addTab("Create / Edit Event", eventPanel);
        tabs.addTab("Event List", listPanel);
        tabs.addTab("Attendance", attendancePanel);
        add(tabs, BorderLayout.CENTER);
    }

    private void startEditing(Event event) {
        eventPanel.loadForEdit(event);
        tabs.setSelectedIndex(0);
    }

    @Override
    public void refresh() {
        eventPanel.refresh();
        listPanel.refresh();
        attendancePanel.refresh();
    }
}
