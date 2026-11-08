package com.system.service;

import classModel.Event;

/**
 * One line of the Reports table. It is a read-only snapshot of the numbers for one event,
 * produced by EventManager.buildReport(...) from the real Registration and Attendance objects.
 */
public class EventReportRow {
    private final Event event;
    private final int totalRegistration;
    private final int attended;
    private final int students;
    private final int faculty;
    private final int others;

    EventReportRow(Event event, int totalRegistration, int attended, int students, int faculty, int others) {
        this.event = event;
        this.totalRegistration = totalRegistration;
        this.attended = attended;
        this.students = students;
        this.faculty = faculty;
        this.others = others;
    }

    public Event getEvent() { return event; }
    public String getEventName() { return event.getEventName(); }
    public int getTotalRegistration() { return totalRegistration; }
    public int getAttended() { return attended; }
    public int getStudents() { return students; }
    public int getFaculty() { return faculty; }
    public int getOthers() { return others; }
}
