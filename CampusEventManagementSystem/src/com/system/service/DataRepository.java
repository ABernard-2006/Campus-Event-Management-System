package com.system.service;

import classModel.Attendance;
import classModel.Event;
import classModel.Faculty;
import classModel.Organization;
import classModel.Organizer;
import classModel.OrganizerKey;
import classModel.Others;
import classModel.Participant;
import classModel.Registration;
import classModel.Seminar;
import classModel.Student;
import classModel.UserAccount;
import classModel.Venue;
import classModel.Workshop;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The ONE shared holder of the application's in-memory data and the ONLY class that turns that data into
 * text-file lines and back (through the existing {@link DataStore}).
 *
 * Every service receives this same instance from {@link EventManager}, so there is exactly one copy of each
 * list and one storage layer: services can never disagree about the current state, and the GUI cannot reach
 * the files (this class and everything in it is package-private).
 *
 * What lives here:  the collections, loading with per-line warnings, saving (one method per file), and plain
 *                   look-ups by id/name that loading itself needs.
 * What does NOT:    permissions, validation of user input, or workflows. Services own those, and they own the
 *                   in-memory rollback that follows a failed save.
 */
final class DataRepository {
    /** True when events.txt held old-format lines (no duration). A backup is made before the first rewrite. */
    private boolean legacyEventFileLoaded = false;
    private boolean legacyEventBackupDone = false;

    private final DataStore store;
    private final boolean firstRun;
    private final List<Event> events = new ArrayList<>();
    private final List<Participant> participants = new ArrayList<>();
    private final List<Venue> venues = new ArrayList<>();
    private final List<Organization> organizations = new ArrayList<>();
    private final List<Organizer> organizers = new ArrayList<>();
    private final List<Registration> registrations = new ArrayList<>();
    private final List<Attendance> attendances = new ArrayList<>();
    private final List<UserAccount> userAccounts = new ArrayList<>();
    private final List<OrganizerKey> organizerKeys = new ArrayList<>();

    private final List<String> loadWarnings = new ArrayList<>();
    private final Set<String> problemFiles = new LinkedHashSet<>();

    /** Loads every saved file right away (a missing file simply means "no records yet"). */
    DataRepository(DataStore store) {
        if (store == null) throw new IllegalArgumentException("A data store is required.");
        this.store = store;
        this.firstRun = !store.exists(DataStore.ACCOUNTS);
        loadAll();
    }

    // ---------------- the collections (mutable on purpose: services change them, then call save...) ----------------
    List<UserAccount> accounts() { return userAccounts; }
    List<OrganizerKey> organizerKeys() { return organizerKeys; }
    List<Organization> organizations() { return organizations; }
    List<Organizer> organizers() { return organizers; }
    List<Venue> venues() { return venues; }
    List<Event> events() { return events; }
    List<Participant> participants() { return participants; }
    List<Registration> registrations() { return registrations; }
    List<Attendance> attendances() { return attendances; }

    // ---------------- storage information ----------------
    java.nio.file.Path directory() { return store.getDirectory(); }
    String directoryText() { return store.getDirectory().toAbsolutePath().toString(); }
    List<String> loadWarnings() { return Collections.unmodifiableList(loadWarnings); }
    /** True when accounts.txt did not exist when the application started (the built-in administrator is then created). */
    boolean isFirstRun() { return firstRun; }

    // =====================================================================
    //  LOADING
    // =====================================================================
    private void loadAll() {
        loadAccounts();
        loadOrganizerKeys();
        loadOrganizations();
        loadOrganizers();
        loadVenues();
        loadEvents();
        loadParticipants();
        loadRegistrations();
        loadAttendance();
        for (String file : problemFiles) store.backupFile(file);   // keep skipped records before any rewrite
    }

    private List<DataStore.Record> readFile(String fileName) {
        List<String> problems = new ArrayList<>();
        List<DataStore.Record> records = store.readRecords(fileName, problems);
        for (String p : problems) warn(fileName, p);
        return records;
    }

    private void warn(String fileName, String message) {
        loadWarnings.add(fileName + ": " + message);
        problemFiles.add(fileName);
    }

    private void warn(String fileName, DataStore.Record r, String message) {
        warn(fileName, "line " + r.getLineNumber() + " skipped - " + message);
    }

    private static int parseInt(String text) {
        return Integer.parseInt(text.trim());
    }

    private static boolean parseBoolean(String text) {
        String t = text.trim();
        if (t.equals("true")) return true;
        if (t.equals("false")) return false;
        throw new IllegalArgumentException("expected true or false but found \"" + text + "\"");
    }

    private void loadAccounts() {
        for (DataStore.Record r : readFile(DataStore.ACCOUNTS)) {
            try {
                r.requireFields(5);
                UserAccount a = new UserAccount(r.get(0), r.get(1), r.get(2), r.get(3), r.get(4));
                if (findAccount(a.getUsername()) != null) {
                    warn(DataStore.ACCOUNTS, r, "duplicate username " + a.getUsername());
                    continue;
                }
                userAccounts.add(a);
            } catch (IllegalArgumentException ex) {
                warn(DataStore.ACCOUNTS, r, ex.getMessage());
            }
        }
    }

    private void loadOrganizerKeys() {
        for (DataStore.Record r : readFile(DataStore.ORGANIZER_KEYS)) {
            try {
                r.requireFields(9);
                String usedAtText = r.get(8).trim();
                OrganizerKey k = new OrganizerKey(parseInt(r.get(0)), r.get(1), r.get(2), r.get(3),
                        LocalDateTime.parse(r.get(4).trim()), LocalDateTime.parse(r.get(5).trim()),
                        r.get(6).trim(), r.get(7),
                        usedAtText.isEmpty() ? null : LocalDateTime.parse(usedAtText));
                if (findKeyIndex(k.getKeyId()) >= 0) {
                    warn(DataStore.ORGANIZER_KEYS, r, "duplicate key ID " + k.getKeyId());
                    continue;
                }
                organizerKeys.add(k);
            } catch (RuntimeException ex) {
                warn(DataStore.ORGANIZER_KEYS, r, ex.getMessage());
            }
        }
    }

    private void loadOrganizations() {
        for (DataStore.Record r : readFile(DataStore.ORGANIZATIONS)) {
            try {
                r.requireFields(3);
                Organization o = new Organization(parseInt(r.get(0)), r.get(1), r.get(2));
                if (findOrganizationById(o.getOrgId()) != null) {
                    warn(DataStore.ORGANIZATIONS, r, "duplicate organization ID " + o.getOrgId());
                    continue;
                }
                organizations.add(o);
            } catch (IllegalArgumentException ex) {
                warn(DataStore.ORGANIZATIONS, r, ex.getMessage());
            }
        }
    }

    private void loadOrganizers() {
        for (DataStore.Record r : readFile(DataStore.ORGANIZERS)) {
            try {
                r.requireFields(3);
                Organization org = findOrganizationById(parseInt(r.get(1)));
                if (org == null) {
                    warn(DataStore.ORGANIZERS, r, "organization " + r.get(1) + " does not exist");
                    continue;
                }
                Organizer o = new Organizer(r.get(0), org, r.get(2));
                if (findOrganizer(o.getUsername()) != null) {
                    warn(DataStore.ORGANIZERS, r, "duplicate organizer " + o.getUsername());
                    continue;
                }
                organizers.add(o);
            } catch (IllegalArgumentException ex) {
                warn(DataStore.ORGANIZERS, r, ex.getMessage());
            }
        }
    }

    private void loadVenues() {
        for (DataStore.Record r : readFile(DataStore.VENUES)) {
            try {
                r.requireFields(4);
                Venue v = new Venue(parseInt(r.get(0)), r.get(1), r.get(2), parseInt(r.get(3)));
                if (findVenueById(v.getVenueId()) != null) {
                    warn(DataStore.VENUES, r, "duplicate venue ID " + v.getVenueId());
                    continue;
                }
                if (findVenueByName(v.getVenueName()) != null) {
                    warn(DataStore.VENUES, r, "duplicate venue name " + v.getVenueName());
                    continue;
                }
                venues.add(v);
            } catch (IllegalArgumentException ex) {
                warn(DataStore.VENUES, r, ex.getMessage());
            }
        }
    }

    private void loadEvents() {
        for (DataStore.Record r : readFile(DataStore.EVENTS)) {
            try {
                // New format has 10 fields (the last is durationMinutes); old format has 9 and no duration.
                if (r.size() != 9 && r.size() != 10)
                    throw new IllegalArgumentException("expected 9 or 10 fields but found " + r.size());
                int id = parseInt(r.get(1));
                if (findEventById(id) != null) {
                    warn(DataStore.EVENTS, r, "duplicate event ID " + id);
                    continue;
                }
                Venue venue = findVenueById(parseInt(r.get(5)));
                if (venue == null) {
                    warn(DataStore.EVENTS, r, "venue " + r.get(5) + " does not exist");
                    continue;
                }
                LocalDate date = LocalDate.parse(r.get(3).trim());
                LocalTime time = LocalTime.parse(r.get(4).trim());
                int duration;
                if (r.size() == 10) {
                    try {
                        duration = Event.validateDuration(parseInt(r.get(9)));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("duration \"" + r.get(9) + "\" is not a whole number of minutes");
                    }
                } else {
                    duration = Event.LEGACY_DURATION_MINUTES;     // old record: no duration was saved
                    legacyEventFileLoaded = true;
                }
                Event event;
                switch (r.get(0).trim()) {
                    case "WORKSHOP":   // fields: topic | materials | speaker
                        event = new Workshop(id, r.get(2), date, time, duration, venue, r.get(6), r.get(7), r.get(8));
                        break;
                    case "SEMINAR":    // fields: speaker | subject | (unused)
                        event = new Seminar(id, r.get(2), date, time, duration, venue, r.get(6), r.get(7));
                        break;
                    default:
                        throw new IllegalArgumentException("unknown event type \"" + r.get(0) + "\"");
                }
                events.add(event);
            } catch (IllegalArgumentException | DateTimeException ex) {
                warn(DataStore.EVENTS, r, ex.getMessage());
            }
        }
    }

    private void loadParticipants() {
        for (DataStore.Record r : readFile(DataStore.PARTICIPANTS)) {
            try {
                r.requireFields(7);
                int id = parseInt(r.get(1));
                if (findParticipantById(id) != null) {
                    warn(DataStore.PARTICIPANTS, r, "duplicate participant ID " + id);
                    continue;
                }
                Participant p;
                switch (r.get(0).trim()) {
                    case "STUDENT":
                        p = new Student(id, r.get(2), r.get(3), r.get(4), r.get(5));
                        break;
                    case "FACULTY":
                        p = new Faculty(id, r.get(2), r.get(3), r.get(4), r.get(5), r.get(6));
                        break;
                    case "OTHERS":
                        p = new Others(id, r.get(2), r.get(3), r.get(4));
                        break;
                    default:
                        throw new IllegalArgumentException("unknown participant type \"" + r.get(0) + "\"");
                }
                checkParticipantNotDuplicate(p);
                participants.add(p);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                warn(DataStore.PARTICIPANTS, r, ex.getMessage());
            }
        }
    }

    private void loadRegistrations() {
        for (DataStore.Record r : readFile(DataStore.REGISTRATIONS)) {
            try {
                r.requireFields(5);
                int id = parseInt(r.get(0));
                Participant p = findParticipantById(parseInt(r.get(1)));
                Event e = findEventById(parseInt(r.get(2)));
                if (p == null || e == null) {
                    warn(DataStore.REGISTRATIONS, r, "it refers to a participant or event that does not exist");
                    continue;
                }
                if (findRegistrationById(id) != null) {
                    warn(DataStore.REGISTRATIONS, r, "duplicate registration ID " + id);
                    continue;
                }
                registrations.add(new Registration(id, p, e, LocalDate.parse(r.get(3).trim()), r.get(4)));
            } catch (IllegalArgumentException | DateTimeException ex) {
                warn(DataStore.REGISTRATIONS, r, ex.getMessage());
            }
        }
    }

    private void loadAttendance() {
        for (DataStore.Record r : readFile(DataStore.ATTENDANCE)) {
            try {
                r.requireFields(3);
                Registration reg = findRegistrationById(parseInt(r.get(1)));
                if (reg == null) {
                    warn(DataStore.ATTENDANCE, r, "registration " + r.get(1) + " does not exist");
                    continue;
                }
                if (getAttendance(reg) != null) {
                    warn(DataStore.ATTENDANCE, r, "registration " + reg.getRegistrationId() + " already has an attendance record");
                    continue;
                }
                attendances.add(new Attendance(parseInt(r.get(0)), reg, parseBoolean(r.get(2))));
            } catch (IllegalArgumentException ex) {
                warn(DataStore.ATTENDANCE, r, ex.getMessage());
            }
        }
    }


    // =====================================================================
    //  SAVING (each method rewrites one whole file)
    // =====================================================================
    void saveAccounts() {
        List<String> lines = new ArrayList<>();
        for (UserAccount a : userAccounts)
            lines.add(DataStore.join(a.getUsername(), a.getEmail(), a.getSalt(), a.getPasswordHash(), a.getRole()));
        store.writeRecords(DataStore.ACCOUNTS, "username|email|salt|passwordHash|role", lines);
    }

    void saveOrganizerKeys() {
        List<String> lines = new ArrayList<>();
        for (OrganizerKey k : organizerKeys)
            lines.add(DataStore.join(k.getKeyId(), k.getKeyHash(), k.getLabel(), k.getCreatedBy(),
                    k.getCreatedAt(), k.getExpiresAt(), k.getStatus(), k.getUsedBy(),
                    k.getUsedAt() == null ? "" : k.getUsedAt()));
        store.writeRecords(DataStore.ORGANIZER_KEYS,
                "keyId|keyHash|label|createdBy|createdAt|expiresAt|status|usedBy|usedAt", lines);
    }

    void saveOrganizations() {
        List<String> lines = new ArrayList<>();
        for (Organization o : organizations)
            lines.add(DataStore.join(o.getOrgId(), o.getOrgName(), o.getDescription()));
        store.writeRecords(DataStore.ORGANIZATIONS, "orgId|name|description", lines);
    }

    void saveOrganizers() {
        List<String> lines = new ArrayList<>();
        for (Organizer o : organizers)
            lines.add(DataStore.join(o.getUsername(), o.getOrganization().getOrgId(), o.getPosition()));
        store.writeRecords(DataStore.ORGANIZERS, "username|orgId|position", lines);
    }

    void saveVenues() {
        List<String> lines = new ArrayList<>();
        for (Venue v : venues)
            lines.add(DataStore.join(v.getVenueId(), v.getVenueName(), v.getLocation(), v.getCapacity()));
        store.writeRecords(DataStore.VENUES, "venueId|name|location|capacity", lines);
    }

    void saveEvents() {
        // Old-format lines are upgraded when the file is rewritten; keep a copy of the original once first.
        if (legacyEventFileLoaded && !legacyEventBackupDone) {
            store.backupFile(DataStore.EVENTS);
            legacyEventBackupDone = true;
        }
        List<String> lines = new ArrayList<>();
        for (Event e : events) {
            String type, f1, f2, f3;
            if (e instanceof Workshop) {
                Workshop w = (Workshop) e;
                type = "WORKSHOP"; f1 = w.getTopic(); f2 = w.getMaterials(); f3 = w.getSpeaker();
            } else if (e instanceof Seminar) {
                Seminar s = (Seminar) e;
                type = "SEMINAR"; f1 = s.getSpeakerName(); f2 = s.getSubject(); f3 = "";
            } else {
                throw new StorageException("Cannot save unsupported event type " + e.getClass().getSimpleName());
            }
            lines.add(DataStore.join(type, e.getEventId(), e.getEventName(), e.getDate(), e.getStartTime(),
                    e.getVenue().getVenueId(), f1, f2, f3, e.getDurationMinutes()));
        }
        store.writeRecords(DataStore.EVENTS,
                "type|eventId|name|date|startTime|venueId|WORKSHOP: topic,materials,speaker / SEMINAR: speaker,subject,(blank)|durationMinutes", lines);
    }

    void saveParticipants() {
        List<String> lines = new ArrayList<>();
        for (Participant p : participants) {
            String type, a, b, c;
            if (p instanceof Student) {
                Student s = (Student) p;
                type = "STUDENT"; a = s.getStudentNumber(); b = s.getProgram(); c = "";
            } else if (p instanceof Faculty) {
                Faculty f = (Faculty) p;
                type = "FACULTY"; a = f.getFacultyNumber(); b = f.getDepartment(); c = f.getPosition();
            } else if (p instanceof Others) {
                type = "OTHERS"; a = ((Others) p).getOrgAffiliation(); b = ""; c = "";
            } else {
                throw new StorageException("Cannot save unsupported participant type " + p.getClass().getSimpleName());
            }
            lines.add(DataStore.join(type, p.getParticipantId(), p.getFullName(), p.getContactInfo(), a, b, c));
        }
        store.writeRecords(DataStore.PARTICIPANTS,
                "type|participantId|fullName|contact|STUDENT: number,program / FACULTY: number,department,position / OTHERS: affiliation", lines);
    }

    void saveRegistrations() {
        List<String> lines = new ArrayList<>();
        for (Registration r : registrations)
            lines.add(DataStore.join(r.getRegistrationId(), r.getParticipant().getParticipantId(),
                    r.getEvent().getEventId(), r.getRegistrationDate(), r.getStatus()));
        store.writeRecords(DataStore.REGISTRATIONS, "registrationId|participantId|eventId|registrationDate|status", lines);
    }

    void saveAttendance() {
        List<String> lines = new ArrayList<>();
        for (Attendance a : attendances)
            lines.add(DataStore.join(a.getAttendanceId(), a.getRegistration().getRegistrationId(), a.isAttended()));
        store.writeRecords(DataStore.ATTENDANCE, "attendanceId|registrationId|attended", lines);
    }

    // =====================================================================
    //  LOOK-UPS (needed while loading, shared by the services)
    // =====================================================================
    UserAccount findAccount(String username) {
        if (username == null) return null;
        for (UserAccount a : userAccounts)
            if (a.getUsername().equalsIgnoreCase(username.trim())) return a;
        return null;
    }

    UserAccount findAccountByEmail(String email) {
        if (email == null) return null;
        for (UserAccount a : userAccounts)
            if (a.getEmail().equalsIgnoreCase(email.trim())) return a;
        return null;
    }

    int findKeyIndex(int keyId) {
        for (int i = 0; i < organizerKeys.size(); i++)
            if (organizerKeys.get(i).getKeyId() == keyId) return i;
        return -1;
    }

    Organization findOrganizationById(int id) {
        for (Organization o : organizations) if (o.getOrgId() == id) return o;
        return null;
    }

    Organizer findOrganizer(String username) {
        if (username == null) return null;
        for (Organizer o : organizers)
            if (username.trim().equalsIgnoreCase(o.getUsername())) return o;
        return null;
    }

    Venue findVenueById(int id) {
        for (Venue v : venues) if (v.getVenueId() == id) return v;
        return null;
    }

    Venue findVenueByName(String name) {
        if (name == null) return null;
        for (Venue v : venues) if (v.getVenueName().equalsIgnoreCase(name.trim())) return v;
        return null;
    }

    Event findEventById(int id) {
        for (Event e : events) if (e.getEventId() == id) return e;
        return null;
    }

    Participant findParticipantById(int id) {
        for (Participant p : participants) if (p.getParticipantId() == id) return p;
        return null;
    }

    Registration findRegistrationById(int id) {
        for (Registration r : registrations) if (r.getRegistrationId() == id) return r;
        return null;
    }

    Attendance getAttendance(Registration r) {
        for (Attendance a : attendances) {
            if (a.getRegistration() == r) return a;
        }
        return null;
    }

    /**
     * Rejects a participant whose student number or faculty ID is already used by a saved participant.
     * The rule itself (what counts as "the same ID", and the message) belongs to the participant classes;
     * this method only applies it to the saved collection. Used both when loading and when registering.
     *
     * @throws IllegalStateException when a saved participant already has the same official ID
     */
    void checkParticipantNotDuplicate(Participant p) {
        for (Participant x : participants) {
            if (p.sharesIdentityWith(x)) throw new IllegalStateException(p.getDuplicateIdentityMessage());
        }
    }
}
