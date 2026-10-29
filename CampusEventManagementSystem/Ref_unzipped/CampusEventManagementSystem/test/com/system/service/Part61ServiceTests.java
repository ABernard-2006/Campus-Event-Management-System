package com.system.service;

import classModel.Attendance;
import classModel.Event;
import classModel.Participant;
import classModel.Registration;
import classModel.Student;
import classModel.UserAccount;
import classModel.Venue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Plain-Java test harness for the service methods added or changed in Part 6.1
 * (no JUnit needed: run main(); exit code 1 when any check fails).
 * Every test uses its own temporary data folder, so the real data/ folder is never touched.
 */
public class Part61ServiceTests {
    static int passed = 0, failed = 0;
    static final List<String> failures = new ArrayList<>();
    static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    static final LocalTime NINE = LocalTime.of(9, 0);

    // ---------- tiny assertion helpers ----------
    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; failures.add(name); System.out.println("  FAIL  " + name); }
    }
    static void expect(String name, Class<? extends Throwable> type, Runnable r) {
        try { r.run(); check(name + " (expected " + type.getSimpleName() + ", nothing thrown)", false); }
        catch (Throwable t) {
            boolean ok = type.isInstance(t);
            check(name + (ok ? "" : " (expected " + type.getSimpleName() + ", got " + t + ")"), ok);
        }
    }
    static void expectMsg(String name, Class<? extends Throwable> type, String fragment, Runnable r) {
        try { r.run(); check(name + " (nothing thrown)", false); }
        catch (Throwable t) {
            boolean ok = type.isInstance(t) && t.getMessage() != null && t.getMessage().contains(fragment);
            check(name + (ok ? "" : " (got " + t + ")"), ok);
        }
    }

    // ---------- fixture ----------
    static Path newDir() throws IOException { return Files.createTempDirectory("cems-test"); }

    static UserAccount writeAccounts(Path dir, UserAccount... accounts) throws IOException {
        StringBuilder sb = new StringBuilder("# username|email|salt|passwordHash|role\n");
        for (UserAccount a : accounts)
            sb.append(a.getUsername()).append('|').append(a.getEmail()).append('|').append(a.getSalt()).append('|')
              .append(a.getPasswordHash()).append('|').append(a.getRole()).append('\n');
        Files.write(dir.resolve("accounts.txt"), sb.toString().getBytes(StandardCharsets.UTF_8));
        return accounts[0];
    }

    static class Fx {
        Path dir; EventManager m; UserAccount admin, organizer, alice, bob;
    }
    static Fx fixture() throws IOException {
        Fx f = new Fx();
        f.dir = newDir();
        f.admin = UserAccount.createNew("admin", "admin@cems.local", "secret1", UserAccount.ROLE_ADMIN);
        f.organizer = UserAccount.createNew("orga", "orga@cems.local", "secret1", UserAccount.ROLE_ORGANIZER);
        f.alice = UserAccount.createNew("alice", "alice@school.edu", "secret1", UserAccount.ROLE_PARTICIPANT);
        f.bob = UserAccount.createNew("bob", "bob@school.edu", "secret1", UserAccount.ROLE_PARTICIPANT);
        writeAccounts(f.dir, f.admin, f.organizer, f.alice, f.bob);
        f.m = new EventManager(new DataStore(f.dir));
        // the manager compares accounts by identity, so use the instances it loaded
        f.admin = f.m.authenticate("admin", "secret1", UserAccount.ROLE_ADMIN);
        f.organizer = f.m.authenticate("orga", "secret1", UserAccount.ROLE_ORGANIZER);
        f.alice = f.m.authenticate("alice", "secret1", UserAccount.ROLE_PARTICIPANT);
        f.bob = f.m.authenticate("bob", "secret1", UserAccount.ROLE_PARTICIPANT);
        return f;
    }
    static Event workshop(Fx f, String name, LocalDate d, LocalTime t, int dur, String venue, int cap) {
        return f.m.createEvent(f.admin, "Workshop", name, d, t, dur, venue, cap, "Dr. Cruz, Ms. Lee", "Java", "Laptop");
    }
    static Event seminar(Fx f, String name, LocalDate d, LocalTime t, int dur, String venue, int cap) {
        return f.m.createEvent(f.organizer, "Seminar", name, d, t, dur, venue, cap, "Prof. Tan", "AI Ethics", "");
    }

    // =====================================================================
    public static void main(String[] args) throws Exception {
        testCreateEvent();
        testCreateEventValidation();
        testVenueRules();
        testConflictsAndIds();
        testUpdateEvent();
        testRegisterForEvent();
        testRegistrationVisibilityAndSearch();
        testCancelAndAttendance();
        testAccountsStillEnforced();
        testOrderingHelpers();
        testPersistenceAndLegacyData(args.length > 0 ? args[0] : null);
        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) { failures.forEach(x -> System.out.println("FAILED: " + x)); System.exit(1); }
    }

    static void testCreateEvent() throws Exception {
        System.out.println("\n[createEvent: happy path, permissions, persistence]");
        Fx f = fixture();
        Event w = workshop(f, "  Java Basics ", TOMORROW, NINE, 90, "Room 101", 30);
        check("workshop created and listed", f.m.getAllEvents().contains(w));
        check("event name trimmed", w.getEventName().equals("Java Basics"));
        check("type is Workshop", w.getEventType().equals("Workshop"));
        check("custom duration kept (90)", w.getDurationMinutes() == 90);
        check("end time computed (10:30)", w.getEndTime().equals(LocalTime.of(10, 30)));
        check("speakers/topic/materials stored", w.getSpeakerNames().equals("Dr. Cruz, Ms. Lee")
                && w.getTopicOrSubject().equals("Java") && w.getMaterials().equals("Laptop"));
        check("new venue saved with capacity", f.m.findVenueByName("Room 101") != null
                && f.m.findVenueByName("Room 101").getCapacity() == 30);
        Event s = seminar(f, "AI Talk", TOMORROW, NINE, 60, "Hall A", 100);
        check("organizer can create a seminar", s.getEventType().equals("Seminar") && s.getTopicOrSubject().equals("AI Ethics"));
        expect("participant cannot create events", SecurityException.class,
                () -> f.m.createEvent(f.alice, "Seminar", "X", TOMORROW, NINE, 60, "Hall B", 10, "S", "T", ""));
        expect("null actor cannot create events", SecurityException.class,
                () -> f.m.createEvent(null, "Seminar", "X", TOMORROW, NINE, 60, "Hall B", 10, "S", "T", ""));
        UserAccount forged = UserAccount.createNew("mallory", "m@x.io", "secret1", UserAccount.ROLE_ADMIN);
        expect("unsaved forged admin cannot create events", SecurityException.class,
                () -> f.m.createEvent(forged, "Seminar", "X", TOMORROW, NINE, 60, "Hall B", 10, "S", "T", ""));
        check("rejected attempts left the event list unchanged", f.m.getAllEvents().size() == 2);
        EventManager reloaded = new EventManager(new DataStore(f.dir));
        check("events persisted to disk", reloaded.getAllEvents().size() == 2);
        check("reloaded event keeps duration", reloaded.getAllEvents().stream()
                .anyMatch(e -> e.getEventName().equals("Java Basics") && e.getDurationMinutes() == 90));
        final int[] fired = {0};
        f.m.addChangeListener(() -> fired[0]++);
        workshop(f, "Another", TOMORROW.plusDays(1), NINE, 60, "Room 101", 30);
        check("change listeners fire after createEvent (dashboard refresh)", fired[0] == 1);
    }

    static void testCreateEventValidation() throws Exception {
        System.out.println("\n[createEvent: business validation enforced by the service]");
        Fx f = fixture();
        EventManager m = f.m; UserAccount a = f.admin;
        String longName = "x".repeat(101), longVenue = "v".repeat(61);
        expectMsg("empty name", IllegalArgumentException.class, "Event name is required",
                () -> m.createEvent(a, "Workshop", "  ", TOMORROW, NINE, 60, "R1", 10, "S", "T", "M"));
        expectMsg("null name", IllegalArgumentException.class, "Event name is required",
                () -> m.createEvent(a, "Workshop", null, TOMORROW, NINE, 60, "R1", 10, "S", "T", "M"));
        expectMsg("name > 100", IllegalArgumentException.class, "100 characters",
                () -> m.createEvent(a, "Workshop", longName, TOMORROW, NINE, 60, "R1", 10, "S", "T", "M"));
        m.createEvent(a, "Workshop", "x".repeat(100), TOMORROW, NINE, 60, "R1", 10, "S", "T", "M");
        check("name of exactly 100 saved", m.getAllEvents().size() == 1);
        expectMsg("empty venue", IllegalArgumentException.class, "Venue is required",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, "", 10, "S", "T", "M"));
        expectMsg("venue > 60", IllegalArgumentException.class, "60 characters",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, longVenue, 10, "S", "T", "M"));
        expectMsg("bad type", IllegalArgumentException.class, "event type",
                () -> m.createEvent(a, "Party", "N", TOMORROW, NINE, 60, "R2", 10, "S", "T", "M"));
        expectMsg("blank type", IllegalArgumentException.class, "event type",
                () -> m.createEvent(a, " ", "N", TOMORROW, NINE, 60, "R2", 10, "S", "T", "M"));
        expectMsg("date in the past", IllegalArgumentException.class, "past",
                () -> m.createEvent(a, "Workshop", "N", LocalDate.now().minusDays(1), NINE, 60, "R2", 10, "S", "T", "M"));
        expectMsg("null date", IllegalArgumentException.class, "date is required",
                () -> m.createEvent(a, "Workshop", "N", null, NINE, 60, "R2", 10, "S", "T", "M"));
        expectMsg("null time", IllegalArgumentException.class, "Start time",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, null, 60, "R2", 10, "S", "T", "M"));
        m.createEvent(a, "Seminar", "Today ok", LocalDate.now(), NINE, 60, "R3", 10, "S", "T", "");
        check("today's date is accepted", m.findVenueByName("R3") != null);
        expectMsg("capacity 0", IllegalArgumentException.class, "greater than zero",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, "R2", 0, "S", "T", "M"));
        expectMsg("capacity negative", IllegalArgumentException.class, "greater than zero",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, "R2", -5, "S", "T", "M"));
        expectMsg("no speakers", IllegalArgumentException.class, "speaker",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, "R2", 10, "  ", "T", "M"));
        expectMsg("workshop without topic", IllegalArgumentException.class, "topic",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, "R2", 10, "S", "", "M"));
        expectMsg("workshop without materials", IllegalArgumentException.class, "materials",
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 60, "R2", 10, "S", "T", " "));
        expectMsg("seminar without subject", IllegalArgumentException.class, "subject",
                () -> m.createEvent(a, "Seminar", "N", TOMORROW, NINE, 60, "R2", 10, "S", "", ""));
        m.createEvent(a, "Seminar", "Sem no materials", TOMORROW.plusDays(2), NINE, 60, "R4", 10, "S", "Topic", null);
        check("seminar ignores missing materials", m.findVenueByName("R4") != null);
        expect("duration 0", IllegalArgumentException.class,
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 0, "R2", 10, "S", "T", "M"));
        expect("duration 1441", IllegalArgumentException.class,
                () -> m.createEvent(a, "Workshop", "N", TOMORROW, NINE, 1441, "R2", 10, "S", "T", "M"));
        Event longDay = m.createEvent(a, "Workshop", "Long day", TOMORROW.plusDays(3), NINE, 1440, "R5", 10, "S", "T", "M");
        check("duration 1440 (24h) accepted", longDay.getDurationMinutes() == 1440 && longDay.endsNextDay());
        Event shortOne = m.createEvent(a, "Workshop", "Short", TOMORROW.plusDays(3), LocalTime.of(10, 0), 1, "R6", 10, "S", "T", "M");
        check("duration 1 accepted", shortOne.getDurationMinutes() == 1);
        check("no failed attempt created a venue", m.findVenueByName("R2") == null && m.getVenues().size() == 5);
        check("exactly the 5 valid events exist", m.getAllEvents().size() == 5);
    }

    static void testVenueRules() throws Exception {
        System.out.println("\n[venue resolution by name]");
        Fx f = fixture();
        Event a = workshop(f, "A", TOMORROW, NINE, 60, "Room 101", 30);
        int venuesBefore = f.m.getVenues().size();
        Event b = workshop(f, "B", TOMORROW.plusDays(1), NINE, 60, "room 101 ", 30);
        check("same name (any case/spaces) reuses ONE venue record", a.getVenue() == b.getVenue()
                && f.m.getVenues().size() == venuesBefore);
        expectMsg("same venue, different capacity is rejected", IllegalArgumentException.class, "already exists with a capacity of 30",
                () -> workshop(f, "C", TOMORROW.plusDays(2), NINE, 60, "Room 101", 50));
        check("rejected attempt created nothing", f.m.getAllEvents().size() == 2 && f.m.getVenues().size() == venuesBefore);
    }

    static void testConflictsAndIds() throws Exception {
        System.out.println("\n[venue conflicts; rejected attempts do not burn IDs]");
        Fx f = fixture();
        Event a = workshop(f, "A", TOMORROW, NINE, 60, "Room 101", 30);             // 09:00-10:00
        expectMsg("overlapping event at same venue rejected", IllegalStateException.class, "Schedule conflict",
                () -> workshop(f, "B", TOMORROW, LocalTime.of(9, 30), 60, "Room 101", 30));
        Event c = workshop(f, "C", TOMORROW, LocalTime.of(10, 0), 60, "Room 101", 30);  // back-to-back
        check("back-to-back at same venue allowed", c.getEventId() == a.getEventId() + 1);
        Event d = workshop(f, "D", TOMORROW, NINE, 60, "Room 202", 30);
        check("same time at a DIFFERENT venue allowed", d.getEventId() == c.getEventId() + 1);
        expect("conflict with a custom 3h duration", IllegalStateException.class,
                () -> workshop(f, "E", TOMORROW, LocalTime.of(8, 0), 180, "Room 101", 30));
        Event g = workshop(f, "G", TOMORROW.plusDays(1), LocalTime.of(23, 0), 120, "Room 101", 30);   // crosses midnight
        check("event crossing midnight saved (+1 day)", g.endsNextDay());
        expectMsg("conflict across midnight", IllegalStateException.class, "Schedule conflict",
                () -> workshop(f, "H", TOMORROW.plusDays(2), LocalTime.of(0, 30), 30, "Room 101", 30));
        Event ok = workshop(f, "I", TOMORROW.plusDays(5), NINE, 60, "Room 101", 30);
        check("rejected conflicts consumed no event IDs (next ID is contiguous)", ok.getEventId() == g.getEventId() + 1);
    }

    static void testUpdateEvent() throws Exception {
        System.out.println("\n[updateEvent by form values]");
        Fx f = fixture();
        Event w = workshop(f, "W", TOMORROW, NINE, 60, "Room 101", 30);
        Event other = workshop(f, "Other", TOMORROW, LocalTime.of(12, 0), 60, "Room 101", 30);
        f.m.updateEvent(f.organizer, w, "W renamed", TOMORROW.plusDays(1), LocalTime.of(14, 0), 45,
                "Room 101", 30, "New Speaker", "New Topic", "New Materials");
        check("fields updated", w.getEventName().equals("W renamed") && w.getDate().equals(TOMORROW.plusDays(1))
                && w.getStartTime().equals(LocalTime.of(14, 0)) && w.getDurationMinutes() == 45
                && w.getSpeakerNames().equals("New Speaker") && w.getTopicOrSubject().equals("New Topic")
                && w.getMaterials().equals("New Materials"));
        check("update persisted", new EventManager(new DataStore(f.dir)).getAllEvents().stream()
                .anyMatch(e -> e.getEventName().equals("W renamed") && e.getDurationMinutes() == 45));
        expect("participant cannot update", SecurityException.class,
                () -> f.m.updateEvent(f.alice, w, "Hack", TOMORROW, NINE, 60, "Room 101", 30, "S", "T", "M"));
        expectMsg("changing date to the past rejected", IllegalArgumentException.class, "past",
                () -> f.m.updateEvent(f.admin, w, "W", LocalDate.now().minusDays(1), NINE, 60, "Room 101", 30, "S", "T", "M"));
        expectMsg("update into a conflict rejected", IllegalStateException.class, "Schedule conflict",
                () -> f.m.updateEvent(f.admin, w, "W", TOMORROW, LocalTime.of(12, 30), 60, "Room 101", 30, "S", "T", "M"));
        check("failed update left the event untouched", w.getEventName().equals("W renamed")
                && w.getStartTime().equals(LocalTime.of(14, 0)));
        expect("duration 0 rejected on update", IllegalArgumentException.class,
                () -> f.m.updateEvent(f.admin, w, "W", w.getDate(), NINE, 0, "Room 101", 30, "S", "T", "M"));
        expectMsg("venue name with different capacity rejected on update", IllegalArgumentException.class, "already exists",
                () -> f.m.updateEvent(f.admin, w, "W", w.getDate(), NINE, 60, "Room 101", 99, "S", "T", "M"));
        expectMsg("workshop update needs materials", IllegalArgumentException.class, "materials",
                () -> f.m.updateEvent(f.admin, w, "W", w.getDate(), NINE, 60, "Room 101", 30, "S", "T", ""));
        // Old (already past) event stays editable when its date is unchanged.
        Path dir = newDir();
        try {
            UserAccount adm = UserAccount.createNew("admin", "a@a.io", "secret1", UserAccount.ROLE_ADMIN);
            writeAccounts(dir, adm);
            LocalDate past = LocalDate.now().minusDays(10);
            Files.write(dir.resolve("venues.txt"), "1|Old Hall|TBA|20\n".getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("events.txt"), ("SEMINAR|1|Old Talk|" + past + "|10:00|1|Spk|Subj||60\n").getBytes(StandardCharsets.UTF_8));
            EventManager m2 = new EventManager(new DataStore(dir));
            UserAccount adm2 = m2.authenticate("admin", "secret1", UserAccount.ROLE_ADMIN);
            Event old = m2.getAllEvents().get(0);
            m2.updateEvent(adm2, old, "Old Talk v2", past, LocalTime.of(10, 0), 60, "Old Hall", 20, "Spk", "Subj", "");
            check("past event can be edited when its date is unchanged", old.getEventName().equals("Old Talk v2"));
        } finally { /* temp dirs are left for the OS to clean */ }
        // capacity check against registrations
        Event small = workshop(f, "Small", TOMORROW.plusDays(4), NINE, 60, "Tiny", 2);
        f.m.registerForEvent(f.admin, small, "Others", "G1", "g1@x.io", "", "Co", "");
        f.m.registerForEvent(f.admin, small, "Others", "G2", "g2@x.io", "", "Co", "");
        expectMsg("cannot move event to a venue too small for its registrations", IllegalStateException.class, "holds only",
                () -> f.m.updateEvent(f.admin, small, "Small", small.getDate(), NINE, 60, "Closet", 1, "S", "T", "M"));
    }

    static void testRegisterForEvent() throws Exception {
        System.out.println("\n[registerForEvent: participant resolution, field rules, authorization]");
        Fx f = fixture();
        EventManager m = f.m;
        Event e1 = workshop(f, "E1", TOMORROW, NINE, 60, "Room 101", 2);
        Event e2 = workshop(f, "E2", TOMORROW.plusDays(1), NINE, 60, "Room 101", 2);
        // ----- staff walk-ins
        Registration r = m.registerForEvent(f.organizer, e1, "Student", "Ana Cruz", "ana@school.edu", "2024-001", "BSCS", "");
        check("staff registers a new Student walk-in", r.isActive() && r.getParticipant() instanceof Student
                && ((Student) r.getParticipant()).getProgram().equals("BSCS"));
        check("new participant saved", m.findParticipantByContact("ana@school.edu") != null);
        Registration r2 = m.registerForEvent(f.admin, e2, "Student", "Ana C.", "different@school.edu", "2024-001", "BSCS", "");
        check("same student number reuses the saved participant", r2.getParticipant() == r.getParticipant()
                && m.getAllParticipants().size() == 1);
        expectMsg("duplicate active registration", IllegalStateException.class, "already registered",
                () -> m.registerForEvent(f.admin, e1, "Student", "Ana Cruz", "ana@school.edu", "2024-001", "BSCS", ""));
        Registration rf = m.registerForEvent(f.admin, e1, "Faculty", "Dr. Reyes", "reyes@school.edu", "F-9", "Math", "Professor");
        check("staff registers Faculty", rf.getParticipant().getClass().getSimpleName().equals("Faculty"));
        expectMsg("venue full (capacity 2)", IllegalStateException.class, "Venue is full",
                () -> m.registerForEvent(f.admin, e1, "Others", "Guest", "guest@x.io", "", "ACME", ""));
        // ----- field rules
        expectMsg("no event", IllegalArgumentException.class, "No event selected",
                () -> m.registerForEvent(f.admin, null, "Others", "Guest", "g@x.io", "", "ACME", ""));
        expectMsg("no type", IllegalArgumentException.class, "participant type",
                () -> m.registerForEvent(f.admin, e2, "", "Guest", "g@x.io", "", "ACME", ""));
        expectMsg("bad type", IllegalArgumentException.class, "participant type",
                () -> m.registerForEvent(f.admin, e2, "Alien", "Guest", "g@x.io", "", "ACME", ""));
        expectMsg("no name", IllegalArgumentException.class, "name is required",
                () -> m.registerForEvent(f.admin, e2, "Others", " ", "g@x.io", "", "ACME", ""));
        expectMsg("no contact", IllegalArgumentException.class, "email address is required",
                () -> m.registerForEvent(f.admin, e2, "Others", "G", "", "", "ACME", ""));
        expectMsg("contact with spaces", IllegalArgumentException.class, "must not contain spaces",
                () -> m.registerForEvent(f.admin, e2, "Others", "G", "g @x.io", "", "ACME", ""));
        expectMsg("student without number", IllegalArgumentException.class, "Student number",
                () -> m.registerForEvent(f.admin, e2, "Student", "S", "s@x.io", "", "BSIT", ""));
        expectMsg("student without program", IllegalArgumentException.class, "Program",
                () -> m.registerForEvent(f.admin, e2, "Student", "S", "s@x.io", "1", "", ""));
        expectMsg("faculty without id", IllegalArgumentException.class, "Faculty ID",
                () -> m.registerForEvent(f.admin, e2, "Faculty", "F", "f@x.io", "", "Dept", "Pos"));
        expectMsg("faculty without department", IllegalArgumentException.class, "Department",
                () -> m.registerForEvent(f.admin, e2, "Faculty", "F", "f@x.io", "9", "", "Pos"));
        expectMsg("faculty without position", IllegalArgumentException.class, "Position",
                () -> m.registerForEvent(f.admin, e2, "Faculty", "F", "f@x.io", "9", "Dept", ""));
        expectMsg("others without affiliation", IllegalArgumentException.class, "Affiliation",
                () -> m.registerForEvent(f.admin, e2, "Others", "G", "g@x.io", "", "", ""));
        expectMsg("email already used by another participant", IllegalStateException.class, "already belongs to",
                () -> m.registerForEvent(f.admin, e2, "Others", "Other Ana", "ana@school.edu", "", "ACME", ""));
        check("failed attempts saved no participants", m.getAllParticipants().size() == 2);
        // ----- Participant account
        Event e3 = workshop(f, "E3", TOMORROW.plusDays(2), NINE, 60, "Room 101", 2);
        Event e4 = workshop(f, "E4", TOMORROW.plusDays(3), NINE, 60, "Room 101", 2);
        Registration mine = m.registerForEvent(f.alice, e3, "Student", "Alice Lim", "someone.else@x.io", "2024-777", "BSIT", "");
        check("Participant registers ONLY itself: contact forced to own account email",
                mine.getParticipant().getContactInfo().equals("alice@school.edu"));
        Registration mine2 = m.registerForEvent(f.alice, e4, "Faculty", "Totally Different", "x@x.io", "ZZ", "ZZ", "ZZ");
        check("existing own profile is reused and locked (other fields ignored)", mine2.getParticipant() == mine.getParticipant()
                && mine2.getParticipant().getFullName().equals("Alice Lim"));
        expectMsg("Participant duplicate", IllegalStateException.class, "already registered",
                () -> m.registerForEvent(f.alice, e3, "Student", "Alice Lim", "", "", "", ""));
        expectMsg("Participant first-time profile still needs required fields", IllegalArgumentException.class, "name is required",
                () -> m.registerForEvent(f.bob, e3, "Others", " ", "", "", "ACME", ""));
        expect("not logged in", SecurityException.class,
                () -> m.registerForEvent(null, e3, "Others", "G", "g@x.io", "", "ACME", ""));
        // an event that is already over cannot be created via the form; load one from disk instead
        Path dir = newDir();
        UserAccount adm = UserAccount.createNew("admin", "a@a.io", "secret1", UserAccount.ROLE_ADMIN);
        writeAccounts(dir, adm);
        Files.write(dir.resolve("venues.txt"), "1|Old Hall|TBA|20\n".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("events.txt"), ("SEMINAR|1|Old Talk|" + LocalDate.now().minusDays(3) + "|10:00|1|Spk|Subj||60\n").getBytes(StandardCharsets.UTF_8));
        EventManager m2 = new EventManager(new DataStore(dir));
        UserAccount adm2 = m2.authenticate("admin", "secret1", UserAccount.ROLE_ADMIN);
        expectMsg("registration closed for an event that already took place", IllegalStateException.class, "closed",
                () -> m2.registerForEvent(adm2, m2.getAllEvents().get(0), "Others", "G", "g@x.io", "", "ACME", ""));
        check("registration persisted", new EventManager(new DataStore(f.dir)).getAllRegistrations().size() == m.getAllRegistrations().size());
    }

    static void testRegistrationVisibilityAndSearch() throws Exception {
        System.out.println("\n[searchVisibleRegistrations]");
        Fx f = fixture();
        Event e1 = workshop(f, "Robotics Lab", TOMORROW, NINE, 60, "Room 101", 10);
        Event e2 = seminar(f, "Poetry Night", TOMORROW.plusDays(1), NINE, 60, "Hall A", 10);
        f.m.registerForEvent(f.alice, e1, "Student", "Alice Lim", "", "1", "BSCS", "");
        f.m.registerForEvent(f.bob, e2, "Others", "Bob Ray", "", "", "ACME", "");
        f.m.registerForEvent(f.admin, e2, "Faculty", "Dr. Who", "who@x.io", "F1", "Physics", "Prof");
        check("empty keyword: staff sees all 3", f.m.searchVisibleRegistrations(f.admin, "").size() == 3);
        check("null keyword treated as empty", f.m.searchVisibleRegistrations(f.admin, null).size() == 3);
        check("participant sees only their own", f.m.searchVisibleRegistrations(f.alice, "").size() == 1
                && f.m.searchVisibleRegistrations(f.alice, "").get(0).getParticipant().getFullName().equals("Alice Lim"));
        check("keyword by participant name", f.m.searchVisibleRegistrations(f.admin, "bob").size() == 1);
        check("keyword by event name (case-insensitive)", f.m.searchVisibleRegistrations(f.admin, "POETRY").size() == 2);
        check("keyword by participant type", f.m.searchVisibleRegistrations(f.admin, "faculty").size() == 1);
        check("keyword by status", f.m.searchVisibleRegistrations(f.admin, "registered").size() == 3);
        check("participant search cannot reveal others' rows", f.m.searchVisibleRegistrations(f.alice, "bob").isEmpty());
        check("unknown account sees nothing", f.m.searchVisibleRegistrations(null, "").isEmpty());
        check("newest registration first", f.m.searchVisibleRegistrations(f.admin, "").get(0).getParticipant().getFullName().equals("Dr. Who"));
    }

    static void testCancelAndAttendance() throws Exception {
        System.out.println("\n[cancellation and attendance: authorization preserved]");
        Fx f = fixture();
        Event e = workshop(f, "E", TOMORROW, NINE, 60, "Room 101", 10);
        Registration ra = f.m.registerForEvent(f.alice, e, "Student", "Alice", "", "1", "BSCS", "");
        Registration rb = f.m.registerForEvent(f.bob, e, "Student", "Bob", "", "2", "BSCS", "");
        expect("participant cannot cancel someone else's registration", SecurityException.class, () -> f.m.cancelRegistration(f.alice, rb));
        f.m.cancelRegistration(f.alice, ra);
        check("participant cancels own registration", !ra.isActive());
        expect("cancelling twice is rejected", IllegalStateException.class, () -> f.m.cancelRegistration(f.alice, ra));
        f.m.registerForEvent(f.alice, e, "Student", "Alice", "", "1", "BSCS", "");
        check("re-registering after cancellation works", f.m.countActive(e) == 2);
        expect("participant cannot mark attendance", SecurityException.class, () -> f.m.markAttendance(f.bob, rb, true));
        f.m.markAttendance(f.organizer, rb, true);
        Attendance at = f.m.getAttendance(rb);
        check("organizer marks present", at != null && at.isAttended() && f.m.countAttended(e) == 1);
        expect("marking present twice is rejected", IllegalStateException.class, () -> f.m.markAttendance(f.admin, rb, true));
        f.m.markAttendance(f.admin, rb, false);
        check("present -> absent changes the same record", f.m.getAllAttendances().size() == 1 && !at.isAttended());
        expect("cancelled registration cannot be marked", IllegalStateException.class, () -> f.m.markAttendance(f.admin, ra, true));
        check("attendance persisted", new EventManager(new DataStore(f.dir)).getAllAttendances().size() == 1);
    }

    static void testAccountsStillEnforced() throws Exception {
        System.out.println("\n[authentication / sign-up rules unchanged]");
        Fx f = fixture();
        EventManager m = f.m;
        check("authenticate with username", m.authenticate("alice", "secret1", "Participant") != null);
        check("authenticate with email", m.authenticate("alice@school.edu", "secret1", "Participant") != null);
        check("wrong password -> null", m.authenticate("alice", "nope", "Participant") == null);
        check("wrong role -> null", m.authenticate("alice", "secret1", "Administrator") == null);
        expect("empty identifier", IllegalArgumentException.class, () -> m.authenticate(" ", "x", "Participant"));
        expect("empty password", IllegalArgumentException.class, () -> m.authenticate("alice", "", "Participant"));
        expect("missing role", IllegalArgumentException.class, () -> m.authenticate("alice", "x", ""));
        UserAccount u = m.registerAccount("newuser", "new@school.edu", "secret1", "secret1", "Participant", "");
        check("participant sign-up works", u.getRole().equals("Participant") && m.usernameExists("newuser"));
        expectMsg("duplicate email rejected (Part 5 fix)", IllegalStateException.class, "email address is already registered",
                () -> m.registerAccount("another", "NEW@school.edu", "secret1", "secret1", "Participant", ""));
        expectMsg("duplicate username rejected", IllegalStateException.class, "already taken",
                () -> m.registerAccount("NewUser", "x@school.edu", "secret1", "secret1", "Participant", ""));
        expect("admin sign-up rejected", SecurityException.class,
                () -> m.registerAccount("root", "root@school.edu", "secret1", "secret1", "Administrator", ""));
        expectMsg("organizer without key rejected", IllegalArgumentException.class, "invitation key is required",
                () -> m.registerAccount("org2", "org2@school.edu", "secret1", "secret1", "Organizer", ""));
        expect("organizer with bogus key rejected", IllegalArgumentException.class,
                () -> m.registerAccount("org2", "org2@school.edu", "secret1", "secret1", "Organizer", "AAAA-BBBB-CCCC"));
        String key = m.generateOrganizerKey(f.admin, "test", 7);
        expect("non-admin cannot list keys", SecurityException.class, () -> m.getOrganizerKeys(f.organizer));
        UserAccount org = m.registerAccount("org2", "org2@school.edu", "secret1", "secret1", "Organizer", key);
        check("organizer sign-up with valid key works", org.getRole().equals("Organizer"));
        expectMsg("key cannot be reused", IllegalArgumentException.class, "already been used",
                () -> m.registerAccount("org3", "org3@school.edu", "secret1", "secret1", "Organizer", key));
        expectMsg("mismatched confirmation", IllegalArgumentException.class, "do not match",
                () -> m.registerAccount("zed", "zed@school.edu", "secret1", "other11", "Participant", ""));
        check("accounts persisted", new EventManager(new DataStore(f.dir)).usernameExists("org2"));
    }

    static void testOrderingHelpers() throws Exception {
        System.out.println("\n[chronological helpers]");
        Fx f = fixture();
        Event late = workshop(f, "Zeta lab", TOMORROW.plusDays(2), NINE, 60, "R1", 5);
        Event early = workshop(f, "Alpha lab", TOMORROW, LocalTime.of(15, 0), 60, "R2", 5);
        Event earlier = seminar(f, "Alpha talk", TOMORROW, LocalTime.of(8, 0), 60, "R3", 5);
        List<Event> all = f.m.getEventsChronological();
        check("sorted by date then start time", all.get(0) == earlier && all.get(1) == early && all.get(2) == late);
        all.clear();
        check("returned list is a copy (manager unaffected)", f.m.getAllEvents().size() == 3);
        List<Event> s = f.m.searchEventsChronological("alpha");
        check("search + sort", s.size() == 2 && s.get(0) == earlier && s.get(1) == early);
        check("search empty keyword returns all", f.m.searchEventsChronological("").size() == 3);
        check("getUpcomingEvents(0) still returns open events sorted", f.m.getUpcomingEvents(0).size() == 3);
    }

    static void testPersistenceAndLegacyData(String realDataDir) throws Exception {
        System.out.println("\n[existing project data still loads]");
        if (realDataDir == null) { System.out.println("  (skipped: pass the project's data/ folder as argument 1)"); return; }
        Path copy = newDir();
        try (var files = Files.list(Path.of(realDataDir))) {
            for (Path p : (Iterable<Path>) files::iterator) Files.copy(p, copy.resolve(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        EventManager m = new EventManager(new DataStore(copy));
        check("shipped data loads with no warnings", m.getLoadWarnings().isEmpty());
        check("shipped accounts loaded", m.hasAccounts() && m.usernameExists("admin"));
        check("shipped venues loaded", m.getVenues().size() >= 2);
        check("shipped events loaded with their duration", !m.getAllEvents().isEmpty()
                && m.getAllEvents().get(0).getDurationMinutes() == 90);
        // Use the shipped data for a real create: reuse its first venue, pick a free day.
        Venue v = m.getVenues().get(0);
        UserAccount copyAdmin = UserAccount.createNew("tmpadmin", "tmp@cems.local", "secret1", UserAccount.ROLE_ADMIN);
        // Accounts cannot be injected into a running manager, so add one to the copied file and reload.
        String line = copyAdmin.getUsername() + "|" + copyAdmin.getEmail() + "|" + copyAdmin.getSalt() + "|"
                + copyAdmin.getPasswordHash() + "|" + copyAdmin.getRole() + "\n";
        Files.write(copy.resolve("accounts.txt"), line.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
        EventManager m2 = new EventManager(new DataStore(copy));
        UserAccount adm = m2.authenticate("tmpadmin", "secret1", UserAccount.ROLE_ADMIN);
        int before = m2.getAllEvents().size();
        Event added = m2.createEvent(adm, "Seminar", "Post-load event", TOMORROW.plusDays(30), NINE, 45,
                v.getVenueName(), v.getCapacity(), "Spk", "Subj", "");
        check("createEvent works on top of the shipped data", m2.getAllEvents().size() == before + 1
                && added.getVenue() == m2.findVenueByName(v.getVenueName()));
        check("shipped events.txt is rewritten in a loadable form",
                new EventManager(new DataStore(copy)).getAllEvents().size() == before + 1);
    }
}
