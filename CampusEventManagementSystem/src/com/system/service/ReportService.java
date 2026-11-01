package com.system.service;

import classModel.Event;
import classModel.Faculty;
import classModel.Participant;
import classModel.Registration;
import classModel.Student;
import classModel.UserAccount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Report numbers and report text. Reads registrations and attendance through the services that own them
 * ({@link RegistrationService}, {@link AttendanceService}) so the counting rules exist in one place.
 * Building a report is staff only. Table layout and labels stay in the report screen.
 */
public class ReportService {
    private final DataRepository data;
    private final AccessControl access;
    private final RegistrationService registrations;
    private final AttendanceService attendance;

    ReportService(DataRepository data, AccessControl access, RegistrationService registrations,
                  AttendanceService attendance) {
        this.data = data;
        this.access = access;
        this.registrations = registrations;
        this.attendance = attendance;
    }

    /**
     * One report row per event. Counting rules:
     *  - Total Registration = registrations that are still valid (cancelled ones are not counted)
     *  - Attended           = valid registrations whose attendance is recorded as present
     *  - Student / Faculty / Others = valid registrations split by participant category,
     *                         so Student + Faculty + Others always equals Total Registration
     *
     * @param eventType  "Workshop", "Seminar", or null / "All" for every type
     * @param onlyEvent  a single event, or null for all events
     * @param from       first event date to include, or null for no lower limit
     * @param to         last event date to include, or null for no upper limit
     */
    public List<EventReportRow> buildReport(UserAccount actor, String eventType, Event onlyEvent,
                                            LocalDate from, LocalDate to) {
        access.requireStaff(actor, "view reports");
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("The report start date must not be after the end date.");
        String type = eventType == null ? "" : eventType.trim();
        boolean anyType = type.isEmpty() || type.equalsIgnoreCase("All") || type.toLowerCase().startsWith("all ");
        List<Event> picked = new ArrayList<>();
        for (Event e : data.events()) {
            if (onlyEvent != null && e != onlyEvent) continue;
            if (!anyType && !e.getEventType().equalsIgnoreCase(type)) continue;
            if (from != null && e.getDate().isBefore(from)) continue;
            if (to != null && e.getDate().isAfter(to)) continue;
            picked.add(e);
        }
        picked.sort(Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime));

        List<EventReportRow> rows = new ArrayList<>();
        for (Event e : picked) {
            int students = 0, faculty = 0, others = 0, total = 0;
            for (Registration r : data.registrations()) {
                if (r.getEvent() != e || !r.isActive()) continue;
                total++;
                Participant p = r.getParticipant();
                if (p instanceof Student) students++;
                else if (p instanceof Faculty) faculty++;
                else others++;                      // Others (and any future category)
            }
            rows.add(new EventReportRow(e, total, attendance.countAttended(e), students, faculty, others));
        }
        return rows;
    }

    /** Totals shown above the report table: registrations, attended, and the attendance rate in percent. */
    public static ReportTotals summarizeReport(List<EventReportRow> rows) {
        int registered = 0, attended = 0;
        for (EventReportRow r : rows) {
            registered += r.getTotalRegistration();
            attended += r.getAttended();
        }
        return new ReportTotals(registered, attended, attendanceRate(attended, registered));
    }

    /** Attendance rate in percent. Returns 0 when nobody is registered (no division by zero). */
    public static double attendanceRate(int attended, int registered) {
        return registered <= 0 ? 0.0 : (attended * 100.0) / registered;
    }

    // =====================================================================
    //  POLYMORPHISM REPORT
    // =====================================================================
    /** Runtime polymorphism: the Event runs ITS OWN displayDetails() (Workshop and Seminar differ). */
    private String describe(Event e) {
        return e.displayDetails() + "\n"
                + "Registered: " + registrations.countActive(e) + " / " + e.getVenue().getCapacity() + "\n"
                + "Attended  : " + attendance.countAttended(e) + "\n";
    }

    /** The same text block used by the full report, for one event. Staff only (it contains counts). */
    public String describeEvent(UserAccount actor, Event e) {
        access.requireStaff(actor, "view event reports");
        if (e == null || !data.events().contains(e)) throw new IllegalArgumentException("That event no longer exists.");
        return describe(e);
    }

    public String generatePolymorphicEventReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CAMPUS EVENT POLYMORPHIC REPORT ===\n\n");
        for (Event e : data.events()) {
            sb.append(describe(e));
            sb.append("---------------------------------------\n");
        }
        return sb.toString();
    }
}
