package com.system.service;

import classModel.Event;
import classModel.Registration;
import classModel.UserAccount;
import classModel.Venue;
import classModel.Workshop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Part 6.2 service tests: the methods added so the GUI no longer decides roles, ordering, date ranges,
 * totals or remember-me storage itself. Plain main() harness, temp folders only. Reuses the Part 6.1 fixture.
 */
public class Part62ServiceTests {
    static int passed = 0, failed = 0;
    static final List<String> failures = new ArrayList<>();
    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; failures.add(name); System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) throws Exception {
        testRoleQueries();
        testDisplayOrder();
        testDatesAndRange();
        testReportTotals();
        testRememberLogin();
        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) { failures.forEach(x -> System.out.println("FAILED: " + x)); System.exit(1); }
    }

    static void testRoleQueries() throws Exception {
        System.out.println("\n[role queries used by Dashboard]");
        Part61ServiceTests.Fx f = Part61ServiceTests.fixture();
        UserAccount forged = UserAccount.createNew("forged", "forged@x.edu", "secret1", UserAccount.ROLE_ADMIN); // never saved
        check("admin: manage events / reports / keys", f.m.canManageEvents(f.admin) && f.m.canViewReports(f.admin) && f.m.canManageOrganizerKeys(f.admin));
        check("organizer: manage events + reports, NOT keys", f.m.canManageEvents(f.organizer) && f.m.canViewReports(f.organizer) && !f.m.canManageOrganizerKeys(f.organizer));
        check("participant: nothing", !f.m.canManageEvents(f.alice) && !f.m.canViewReports(f.alice) && !f.m.canManageOrganizerKeys(f.alice));
        check("null actor: nothing", !f.m.canManageEvents(null) && !f.m.canViewReports(null) && !f.m.canManageOrganizerKeys(null));
        check("forged (unsaved) admin account: nothing", !f.m.canManageEvents(forged) && !f.m.canViewReports(forged) && !f.m.canManageOrganizerKeys(forged));
        check("the answers match what the service enforces (report + keys refuse a participant)",
                refuses(() -> f.m.buildReport(f.alice, null, null, null, null)) && refuses(() -> f.m.getOrganizerKeys(f.alice)));
    }

    static boolean refuses(Runnable r) {
        try { r.run(); return false; } catch (SecurityException ex) { return true; }
    }

    static void testDisplayOrder() throws Exception {
        System.out.println("\n[orderForDisplay]");
        Venue v = new Venue("Order Hall", "Campus", 50);
        LocalDate today = LocalDate.now();
        Event pastOld = new Workshop(9001, "pastOld", today.minusDays(10), LocalTime.of(9, 0), 60, v, "t", "m", "s");
        Event pastNew = new Workshop(9002, "pastNew", today.minusDays(2), LocalTime.of(9, 0), 60, v, "t", "m", "s");
        Event todayLate = new Workshop(9003, "todayLate", today, LocalTime.of(15, 0), 60, v, "t", "m", "s");
        Event todayEarly = new Workshop(9004, "todayEarly", today, LocalTime.of(8, 0), 60, v, "t", "m", "s");
        Event soon = new Workshop(9005, "soon", today.plusDays(1), LocalTime.of(9, 0), 60, v, "t", "m", "s");
        List<Event> input = new ArrayList<>(List.of(pastOld, soon, todayLate, pastNew, todayEarly));
        List<Event> out = EventManager.orderForDisplay(input);
        String order = out.stream().map(Event::getEventName).reduce("", (a, b) -> a + b + ",");
        check("upcoming soonest first, then past most-recent first: " + order,
                order.equals("todayEarly,todayLate,soon,pastNew,pastOld,"));
        check("input list is not modified", input.get(0) == pastOld && input.size() == 5);
        check("empty input gives an empty list", EventManager.orderForDisplay(new ArrayList<>()).isEmpty());
    }

    static void testDatesAndRange() throws Exception {
        System.out.println("\n[getEventDates / getEventDateRange]");
        Part61ServiceTests.Fx f = Part61ServiceTests.fixture();
        LocalDate today = LocalDate.now();
        LocalDate[] none = f.m.getEventDateRange();
        check("no events: range is today..today and no dates", none[0].equals(today) && none[1].equals(today) && f.m.getEventDates().isEmpty());
        Part61ServiceTests.workshop(f, "Late", today.plusDays(20), LocalTime.of(9, 0), 60, "R1", 10);
        Part61ServiceTests.workshop(f, "Early", today.plusDays(3), LocalTime.of(9, 0), 60, "R2", 10);
        Part61ServiceTests.seminar(f, "Same day", today.plusDays(3), LocalTime.of(14, 0), 60, "R3", 10);
        LocalDate[] r = f.m.getEventDateRange();
        check("range = earliest and latest event date", r[0].equals(today.plusDays(3)) && r[1].equals(today.plusDays(20)));
        check("event dates are distinct days", f.m.getEventDates().size() == 2
                && f.m.getEventDates().contains(today.plusDays(3)) && f.m.getEventDates().contains(today.plusDays(20)));
        check("the returned date set is a copy (changing it does not change the service)",
                modifiable(f.m) && f.m.getEventDates().size() == 2);
    }
    static boolean modifiable(EventManager m) {
        m.getEventDates().clear();
        return true;
    }

    static void testReportTotals() throws Exception {
        System.out.println("\n[summarizeReport]");
        Part61ServiceTests.Fx f = Part61ServiceTests.fixture();
        ReportTotals empty = EventManager.summarizeReport(f.m.buildReport(f.admin, "All Types", null, null, null));
        check("no events: 0 / 0 / 0.0%", empty.getTotalRegistrations() == 0 && empty.getTotalAttended() == 0 && empty.getAttendanceRatePercent() == 0.0);

        Event e1 = Part61ServiceTests.workshop(f, "W1", LocalDate.now().plusDays(2), LocalTime.of(9, 0), 60, "R1", 10);
        Event e2 = Part61ServiceTests.seminar(f, "S1", LocalDate.now().plusDays(4), LocalTime.of(9, 0), 60, "R2", 10);
        Registration a = f.m.registerForEvent(f.admin, e1, "Student", "Sam", "sam@x.edu", "2024-1", "BSCS", "");
        Registration b = f.m.registerForEvent(f.admin, e1, "Faculty", "Fay", "fay@x.edu", "F-1", "Math", "Prof");
        Registration c = f.m.registerForEvent(f.admin, e2, "Others", "Otto", "otto@x.edu", "", "ACME", "");
        f.m.registerForEvent(f.admin, e2, "Student", "Gone", "gone@x.edu", "2024-2", "BSIT", "");
        f.m.cancelRegistration(f.admin, f.m.getRegistrationsFor(f.admin, e2).get(1));
        f.m.markAttendance(f.admin, a, true);
        f.m.markAttendance(f.admin, c, true);
        f.m.markAttendance(f.admin, b, false);

        ReportTotals t = EventManager.summarizeReport(f.m.buildReport(f.admin, "All Types", null, null, null));
        check("3 valid registrations (cancelled not counted)", t.getTotalRegistrations() == 3);
        check("2 attended", t.getTotalAttended() == 2);
        check("rate = 66.67%", Math.abs(t.getAttendanceRatePercent() - 200.0 / 3) < 1e-9);
        ReportTotals w = EventManager.summarizeReport(f.m.buildReport(f.admin, "Workshop", null, null, null));
        check("Workshop filter: 2 registered, 1 attended, 50%", w.getTotalRegistrations() == 2 && w.getTotalAttended() == 1 && w.getAttendanceRatePercent() == 50.0);
        check("totals agree with the rows (same numbers the table shows)",
                f.m.buildReport(f.admin, "All Types", null, null, null).stream().mapToInt(EventReportRow::getTotalRegistration).sum() == t.getTotalRegistrations());
    }

    static void testRememberLogin() throws Exception {
        System.out.println("\n[remember me through the service]");
        Part61ServiceTests.Fx f = Part61ServiceTests.fixture();
        check("nothing remembered at first", f.m.getRememberedLogin() == null);
        f.m.rememberLogin(f.alice, true);
        String[] got = f.m.getRememberedLogin();
        check("remembered username and role", got != null && got[0].equals("alice") && got[1].equals(UserAccount.ROLE_PARTICIPANT));
        String file = new String(Files.readAllBytes(f.dir.resolve("login-preferences.properties")), "UTF-8");
        check("the password is never written", !file.contains("secret1") && !file.toLowerCase().contains("password="));
        f.m.rememberLogin(f.bob, true);
        check("a new remembered login replaces the old one", f.m.getRememberedLogin()[0].equals("bob"));
        f.m.rememberLogin(f.bob, false);
        check("unchecking Remember me forgets it (file removed)", f.m.getRememberedLogin() == null
                && !Files.exists(f.dir.resolve("login-preferences.properties")));
        f.m.rememberLogin(null, true);
        check("null account with remember=true is harmless and clears", f.m.getRememberedLogin() == null);
    }
}
