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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service manager: applies the business rules for accounts, events, venues, participants,
 * registrations and attendance, and keeps the text files in sync with memory.
 *
 * Every method that changes data saves it right away. If the save fails, the in-memory
 * change is undone and a StorageException is thrown, so the screen never reports a false success.
 */
public class EventManager {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final DataStore store;
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

    /** Uses the default "data" folder. */
    public EventManager() {
        this(new DataStore());
    }

    public EventManager(DataStore store) {
        if (store == null) throw new IllegalArgumentException("A data store is required.");
        this.store = store;
        loadAll();
    }

    public String getDataDirectory() { return store.getDirectory().toAbsolutePath().toString(); }
    public List<String> getLoadWarnings() { return Collections.unmodifiableList(loadWarnings); }

    // =====================================================================
    //  LOADING
    // =====================================================================
    private void loadAll() {
        boolean firstRun = !store.exists(DataStore.ACCOUNTS);
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

        if (firstRun && userAccounts.isEmpty()) {                   // only when accounts.txt never existed
            // Only the built-in administrator is created. Organizers and participants must sign up,
            // and organizers need an invitation key that this administrator generates.
            userAccounts.add(UserAccount.createNew("admin", "admin@cems.local", "admin123", UserAccount.ROLE_ADMIN));
            saveAccounts();
        }
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
                r.requireFields(9);
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
                Event event;
                switch (r.get(0).trim()) {
                    case "WORKSHOP":   // fields: topic | materials | speaker
                        event = new Workshop(id, r.get(2), date, time, venue, r.get(6), r.get(7), r.get(8));
                        break;
                    case "SEMINAR":    // fields: speaker | subject | (unused)
                        event = new Seminar(id, r.get(2), date, time, venue, r.get(6), r.get(7));
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
    private void saveAccounts() {
        List<String> lines = new ArrayList<>();
        for (UserAccount a : userAccounts)
            lines.add(DataStore.join(a.getUsername(), a.getEmail(), a.getSalt(), a.getPasswordHash(), a.getRole()));
        store.writeRecords(DataStore.ACCOUNTS, "username|email|salt|passwordHash|role", lines);
    }

    private void saveOrganizerKeys() {
        List<String> lines = new ArrayList<>();
        for (OrganizerKey k : organizerKeys)
            lines.add(DataStore.join(k.getKeyId(), k.getKeyHash(), k.getLabel(), k.getCreatedBy(),
                    k.getCreatedAt(), k.getExpiresAt(), k.getStatus(), k.getUsedBy(),
                    k.getUsedAt() == null ? "" : k.getUsedAt()));
        store.writeRecords(DataStore.ORGANIZER_KEYS,
                "keyId|keyHash|label|createdBy|createdAt|expiresAt|status|usedBy|usedAt", lines);
    }

    private void saveOrganizations() {
        List<String> lines = new ArrayList<>();
        for (Organization o : organizations)
            lines.add(DataStore.join(o.getOrgId(), o.getOrgName(), o.getDescription()));
        store.writeRecords(DataStore.ORGANIZATIONS, "orgId|name|description", lines);
    }

    private void saveOrganizers() {
        List<String> lines = new ArrayList<>();
        for (Organizer o : organizers)
            lines.add(DataStore.join(o.getUsername(), o.getOrganization().getOrgId(), o.getPosition()));
        store.writeRecords(DataStore.ORGANIZERS, "username|orgId|position", lines);
    }

    private void saveVenues() {
        List<String> lines = new ArrayList<>();
        for (Venue v : venues)
            lines.add(DataStore.join(v.getVenueId(), v.getVenueName(), v.getLocation(), v.getCapacity()));
        store.writeRecords(DataStore.VENUES, "venueId|name|location|capacity", lines);
    }

    private void saveEvents() {
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
                    e.getVenue().getVenueId(), f1, f2, f3));
        }
        store.writeRecords(DataStore.EVENTS,
                "type|eventId|name|date|startTime|venueId|WORKSHOP: topic,materials,speaker / SEMINAR: speaker,subject,(blank)", lines);
    }

    private void saveParticipants() {
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

    private void saveRegistrations() {
        List<String> lines = new ArrayList<>();
        for (Registration r : registrations)
            lines.add(DataStore.join(r.getRegistrationId(), r.getParticipant().getParticipantId(),
                    r.getEvent().getEventId(), r.getRegistrationDate(), r.getStatus()));
        store.writeRecords(DataStore.REGISTRATIONS, "registrationId|participantId|eventId|registrationDate|status", lines);
    }

    private void saveAttendance() {
        List<String> lines = new ArrayList<>();
        for (Attendance a : attendances)
            lines.add(DataStore.join(a.getAttendanceId(), a.getRegistration().getRegistrationId(), a.isAttended()));
        store.writeRecords(DataStore.ATTENDANCE, "attendanceId|registrationId|attended", lines);
    }

    // =====================================================================
    //  ACCOUNTS
    // =====================================================================
    private UserAccount findAccount(String username) {
        if (username == null) return null;
        for (UserAccount a : userAccounts)
            if (a.getUsername().equalsIgnoreCase(username.trim())) return a;
        return null;
    }

    private UserAccount findAccountByEmail(String email) {
        if (email == null) return null;
        for (UserAccount a : userAccounts)
            if (a.getEmail().equalsIgnoreCase(email.trim())) return a;
        return null;
    }

    public boolean hasAccounts() { return !userAccounts.isEmpty(); }
    public boolean usernameExists(String username) { return findAccount(username) != null; }
    public boolean emailExists(String email) { return findAccountByEmail(email) != null; }

    /**
     * Checks the username-or-email, password and selected role against the saved accounts.
     *
     * @return the matching account, or null when the credentials do not match
     * @throws IllegalArgumentException when an input is empty
     * @throws IllegalStateException    when no accounts exist at all
     */
    public synchronized UserAccount authenticate(String identifier, String password, String role) {
        if (identifier == null || identifier.trim().isEmpty())
            throw new IllegalArgumentException("Enter your username or email.");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Enter your password.");
        if (role == null || role.trim().isEmpty())
            throw new IllegalArgumentException("Select the role you are logging in as.");
        if (userAccounts.isEmpty())
            throw new IllegalStateException("No accounts exist yet. Please register first.");
        for (UserAccount account : userAccounts) {
            if (account.matches(identifier, password, role)) return account;
        }
        return null;
    }

    /**
     * Public sign-up. Creates a Participant account, or an Organizer account when a valid invitation key is given.
     *
     * Rules enforced here (not only on the screen):
     *  - Administrator accounts can never be created by sign-up.
     *  - An Organizer needs an invitation key that is unused, not revoked and not expired.
     *  - Using a key and creating the account happen together inside one synchronized method,
     *    so the same key cannot be spent by two sign-ups.
     *  - The key is only spent when every other field is valid.
     *
     * @param invitationKey ignored for Participants
     * @throws SecurityException        when the role is Administrator
     * @throws IllegalArgumentException when a field is missing or invalid, or the key is not accepted
     * @throws IllegalStateException    when the username or email is already taken (StorageException if saving fails)
     */
    public synchronized UserAccount registerAccount(String username, String email, String password,
                                                    String confirmPassword, String role, String invitationKey) {
        String normalizedRole;
        try {
            normalizedRole = UserAccount.normalizeRole(role);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Select what you are registering as.");
        }
        if (UserAccount.ROLE_ADMIN.equals(normalizedRole))
            throw new SecurityException("Administrator accounts cannot be created through sign up.");

        String u = username == null ? "" : username.trim();
        String e = email == null ? "" : email.trim();
        if (u.isEmpty()) throw new IllegalArgumentException("Username is required.");
        if (!USERNAME_PATTERN.matcher(u).matches())
            throw new IllegalArgumentException("Username must be 3-30 characters: letters, numbers, dot, dash or underscore.");
        if (e.isEmpty()) throw new IllegalArgumentException("Email address is required.");
        if (!EMAIL_PATTERN.matcher(e).matches())
            throw new IllegalArgumentException("Enter a valid email address, for example name@school.edu.");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Password is required.");
        if (password.length() < MIN_PASSWORD_LENGTH)
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        if (confirmPassword != null && !password.equals(confirmPassword))
            throw new IllegalArgumentException("Passwords do not match.");
        if (findAccount(u) != null) throw new IllegalStateException("The username \"" + u + "\" is already taken.");
        if (findAccountByEmail(e) != null) throw new IllegalStateException("That email address is already registered.");

        boolean organizer = UserAccount.ROLE_ORGANIZER.equals(normalizedRole);
        LocalDateTime now = LocalDateTime.now();
        int keyIndex = -1;
        OrganizerKey spentKey = null;
        if (organizer) {
            if (invitationKey == null || OrganizerKey.normalize(invitationKey).isEmpty())
                throw new IllegalArgumentException("An invitation key is required to register as an Organizer.");
            keyIndex = findKeyIndexByText(invitationKey);
            if (keyIndex < 0)
                throw new IllegalArgumentException("That invitation key is not valid. Check it and try again.");
            OrganizerKey key = organizerKeys.get(keyIndex);
            if (OrganizerKey.STATUS_USED.equals(key.getStatus()))
                throw new IllegalArgumentException("That invitation key has already been used.");
            if (OrganizerKey.STATUS_REVOKED.equals(key.getStatus()))
                throw new IllegalArgumentException("That invitation key has been revoked. Ask an administrator for a new one.");
            if (key.isExpired(now))
                throw new IllegalArgumentException("That invitation key has expired. Ask an administrator for a new one.");
            spentKey = key;
        }

        UserAccount account = UserAccount.createNew(u, e, password, normalizedRole);

        if (organizer) {
            // Spend the key first and save it. If the account cannot be saved afterwards the key is
            // put back; in the worst case a key is lost and the administrator simply issues another.
            organizerKeys.set(keyIndex, spentKey.markUsed(u, now.withNano(0)));
            try {
                saveOrganizerKeys();
            } catch (StorageException ex) {
                organizerKeys.set(keyIndex, spentKey);
                throw ex;
            }
        }
        userAccounts.add(account);
        try {
            saveAccounts();
        } catch (StorageException ex) {
            userAccounts.remove(account);
            if (organizer) {
                organizerKeys.set(keyIndex, spentKey);
                try {
                    saveOrganizerKeys();
                } catch (StorageException restoreFailure) {
                    ex.addSuppressed(restoreFailure);
                }
            }
            throw ex;
        }
        return account;
    }

    // =====================================================================
    //  ORGANIZER INVITATION KEYS (administrator only)
    // =====================================================================

    /** True only for an account that is really in the saved account list and has the Administrator role. */
    private boolean isAdministrator(UserAccount actor) {
        return actor != null && UserAccount.ROLE_ADMIN.equals(actor.getRole()) && userAccounts.contains(actor);
    }

    private void requireAdministrator(UserAccount actor) {
        if (!isAdministrator(actor))
            throw new SecurityException("Only a logged-in administrator can manage organizer invitation keys.");
    }

    private int findKeyIndex(int keyId) {
        for (int i = 0; i < organizerKeys.size(); i++)
            if (organizerKeys.get(i).getKeyId() == keyId) return i;
        return -1;
    }

    private int findKeyIndexByText(String typedKey) {
        int found = -1;
        for (int i = 0; i < organizerKeys.size(); i++)      // no early exit, so timing does not reveal a match
            if (organizerKeys.get(i).matches(typedKey)) found = i;
        return found;
    }

    private int nextKeyId() {
        int max = 0;
        for (OrganizerKey k : organizerKeys) max = Math.max(max, k.getKeyId());
        return max + 1;
    }

    /**
     * Creates a new invitation key and returns its readable text. This is the ONLY time the readable key
     * exists: only a hash is stored, so it can never be shown again.
     *
     * @param label     a note such as the intended person's name (optional, up to 60 characters)
     * @param validDays how many days the key can be used, from 1 to 30
     * @throws SecurityException when 'actor' is not a logged-in administrator
     */
    public synchronized String generateOrganizerKey(UserAccount actor, String label, int validDays) {
        requireAdministrator(actor);
        if (validDays < 1 || validDays > 30)
            throw new IllegalArgumentException("A key must be valid for between 1 and 30 days.");
        String note = label == null ? "" : label.trim();
        if (note.length() > 60) throw new IllegalArgumentException("The label can be at most 60 characters.");

        String plain;
        String hash;
        do {
            plain = OrganizerKey.generatePlainKey();
            hash = OrganizerKey.hashKey(plain);
            final String candidate = hash;
            if (organizerKeys.stream().noneMatch(k -> k.getKeyHash().equals(candidate))) break;
        } while (true);

        LocalDateTime now = LocalDateTime.now().withNano(0);
        OrganizerKey key = new OrganizerKey(nextKeyId(), hash, note, actor.getUsername(),
                now, now.plusDays(validDays), OrganizerKey.STATUS_UNUSED, "", null);
        organizerKeys.add(key);
        try {
            saveOrganizerKeys();
        } catch (StorageException ex) {
            organizerKeys.remove(key);
            throw ex;
        }
        return plain;
    }

    /** A read-only copy of every key (newest first). The copies hold no readable key text. */
    public synchronized List<OrganizerKey> getOrganizerKeys(UserAccount actor) {
        requireAdministrator(actor);
        List<OrganizerKey> copy = new ArrayList<>(organizerKeys);
        Collections.reverse(copy);
        return Collections.unmodifiableList(copy);
    }

    /** Cancels a key that has not been used yet. A lost key is handled by revoking it and generating a new one. */
    public synchronized void revokeOrganizerKey(UserAccount actor, int keyId) {
        requireAdministrator(actor);
        int index = findKeyIndex(keyId);
        if (index < 0) throw new IllegalArgumentException("That key no longer exists.");
        OrganizerKey key = organizerKeys.get(index);
        if (OrganizerKey.STATUS_USED.equals(key.getStatus()))
            throw new IllegalStateException("That key has already been used and cannot be revoked.");
        if (OrganizerKey.STATUS_REVOKED.equals(key.getStatus()))
            throw new IllegalStateException("That key is already revoked.");
        organizerKeys.set(index, key.revoke());
        try {
            saveOrganizerKeys();
        } catch (StorageException ex) {
            organizerKeys.set(index, key);
            throw ex;
        }
    }

    // =====================================================================
    //  VENUES
    // =====================================================================
    public Venue findVenueById(int id) {
        for (Venue v : venues) if (v.getVenueId() == id) return v;
        return null;
    }

    public Venue findVenueByName(String name) {
        if (name == null) return null;
        for (Venue v : venues) if (v.getVenueName().equalsIgnoreCase(name.trim())) return v;
        return null;
    }

    public List<Venue> getVenues() { return Collections.unmodifiableList(venues); }

    public void addVenue(Venue v) {
        if (v == null) throw new IllegalArgumentException("Venue is required.");
        if (venues.contains(v)) return;
        if (findVenueByName(v.getVenueName()) != null)
            throw new IllegalStateException("A venue named \"" + v.getVenueName() + "\" already exists.");
        venues.add(v);
        try {
            saveVenues();
        } catch (StorageException ex) {
            venues.remove(v);
            throw ex;
        }
    }

    /**
     * Decides which Venue object an event should use: the same object if it is already known,
     * the existing venue with the same name, or the new venue itself. Nothing is changed here.
     * (This makes the schedule-conflict rule work, because two events at "Room 101" share one venue.)
     */
    private Venue resolveVenue(Venue wanted) {
        if (wanted == null) throw new IllegalArgumentException("A venue must be assigned.");
        if (venues.contains(wanted)) return wanted;
        Venue existing = findVenueByName(wanted.getVenueName());
        if (existing == null) return wanted;
        if (existing.getCapacity() != wanted.getCapacity()) {
            throw new IllegalStateException("The venue \"" + existing.getVenueName() + "\" already exists with a capacity of "
                    + existing.getCapacity() + ". Use that capacity or choose a different venue name.");
        }
        return existing;
    }

    // =====================================================================
    //  EVENTS
    // =====================================================================
    public Event findEventById(int id) {
        for (Event e : events) if (e.getEventId() == id) return e;
        return null;
    }

    private void checkConflict(Event ignore, Venue venue, LocalDate date, LocalTime time) {
        for (Event e : events) {
            if (e == ignore) continue;
            if (e.getVenue().getVenueId() == venue.getVenueId()
                    && e.getDate().equals(date) && e.getStartTime().equals(time)) {
                throw new IllegalStateException("Schedule conflict: \"" + e.getEventName()
                        + "\" already uses " + e.getVenue().getVenueName() + " at that date and time.");
            }
        }
    }

    public void addEvent(Event event) {
        if (event == null) throw new IllegalArgumentException("Event is required.");
        if (events.contains(event)) throw new IllegalStateException("This event has already been added.");
        Venue venue = resolveVenue(event.getVenue());
        checkConflict(null, venue, event.getDate(), event.getStartTime());

        boolean newVenue = !venues.contains(venue);
        event.setVenue(venue);
        if (newVenue) venues.add(venue);
        events.add(event);
        try {
            if (newVenue) saveVenues();
            saveEvents();
        } catch (StorageException ex) {
            events.remove(event);
            if (newVenue) venues.remove(venue);
            throw ex;
        }
    }

    /**
     * Changes an existing event. The event type (Workshop/Seminar) cannot change.
     * 'materials' is ignored for seminars.
     */
    public void updateEvent(Event event, String name, LocalDate date, LocalTime time, Venue venue,
                            String speakers, String topicOrSubject, String materials) {
        if (event == null || !events.contains(event)) throw new IllegalArgumentException("Select an event to update.");
        if (date == null) throw new IllegalArgumentException("Event date is required.");
        if (time == null) throw new IllegalArgumentException("Start time is required.");
        Venue newVenue = resolveVenue(venue);
        checkConflict(event, newVenue, date, time);
        if (countActive(event) > newVenue.getCapacity())
            throw new IllegalStateException("That venue holds only " + newVenue.getCapacity()
                    + " people but " + countActive(event) + " are already registered.");

        String oldName = event.getEventName();
        LocalDate oldDate = event.getDate();
        LocalTime oldTime = event.getStartTime();
        Venue oldVenue = event.getVenue();
        String oldSpeakers = event.getSpeakerNames();
        String oldTopic = event.getTopicOrSubject();
        String oldMaterials = event.getMaterials();
        boolean venueIsNew = !venues.contains(newVenue);
        try {
            event.setEventName(name);
            event.setDate(date);
            event.setStartTime(time);
            event.setVenue(newVenue);
            event.updateTypeDetails(speakers, topicOrSubject, materials);
            if (venueIsNew) {
                venues.add(newVenue);
                saveVenues();
            }
            saveEvents();
        } catch (RuntimeException ex) {          // undo everything, then report the original problem
            event.setEventName(oldName);
            event.setDate(oldDate);
            event.setStartTime(oldTime);
            event.setVenue(oldVenue);
            event.updateTypeDetails(oldSpeakers, oldTopic, oldMaterials);
            if (venueIsNew) venues.remove(newVenue);
            throw ex;
        }
    }

    /** Deletes an event together with its registrations and attendance records. */
    public void deleteEvent(Event event) {
        if (event == null || !events.contains(event)) throw new IllegalArgumentException("Select an event to delete.");
        List<Event> oldEvents = new ArrayList<>(events);
        List<Registration> oldRegistrations = new ArrayList<>(registrations);
        List<Attendance> oldAttendances = new ArrayList<>(attendances);

        attendances.removeIf(a -> a.getRegistration().getEvent() == event);
        registrations.removeIf(r -> r.getEvent() == event);
        events.remove(event);
        try {
            saveAttendance();
            saveRegistrations();
            saveEvents();
        } catch (StorageException ex) {
            events.clear();
            events.addAll(oldEvents);
            registrations.clear();
            registrations.addAll(oldRegistrations);
            attendances.clear();
            attendances.addAll(oldAttendances);
            throw ex;
        }
    }

    /** Matches keyword against event name, type, venue, speaker and topic (case-insensitive). */
    public List<Event> searchEvent(String keyword) {
        List<Event> result = new ArrayList<>();
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Event e : events) {
            if (k.isEmpty() || e.getEventName().toLowerCase().contains(k)
                    || e.getEventType().toLowerCase().contains(k)
                    || e.getVenue().getVenueName().toLowerCase().contains(k)
                    || e.getSpeakerNames().toLowerCase().contains(k)
                    || e.getTopicOrSubject().toLowerCase().contains(k)) {
                result.add(e);
            }
        }
        return result;
    }

    public List<Event> getAllEvents() {
        return Collections.unmodifiableList(events);
    }

    /** Events today or later, soonest first. A limit of 0 or less means "all". */
    public List<Event> getUpcomingEvents(int limit) {
        List<Event> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Event e : events) if (!e.getDate().isBefore(today)) result.add(e);
        result.sort(Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime));
        if (limit > 0 && result.size() > limit) return new ArrayList<>(result.subList(0, limit));
        return result;
    }

    public List<Event> getEventsOn(LocalDate date) {
        List<Event> result = new ArrayList<>();
        for (Event e : events) if (e.getDate().equals(date)) result.add(e);
        result.sort(Comparator.comparing(Event::getStartTime));
        return result;
    }

    // =====================================================================
    //  PARTICIPANTS / ORGANIZATIONS / ORGANIZERS
    // =====================================================================
    public Participant findParticipantById(int id) {
        for (Participant p : participants) if (p.getParticipantId() == id) return p;
        return null;
    }

    /** Finds a saved participant: Students/Faculty by their number, Others by full name. Returns null if none. */
    public Participant findParticipant(String type, String number, String name) {
        for (Participant p : participants) {
            if ("Student".equals(type) && p instanceof Student
                    && ((Student) p).getStudentNumber().equalsIgnoreCase(number)) return p;
            if ("Faculty".equals(type) && p instanceof Faculty
                    && ((Faculty) p).getFacultyNumber().equalsIgnoreCase(number)) return p;
            if ("Others".equals(type) && p instanceof Others
                    && p.getFullName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    private void checkParticipantNotDuplicate(Participant p) {
        for (Participant x : participants) {
            if (p instanceof Student && x instanceof Student
                    && ((Student) x).getStudentNumber().equalsIgnoreCase(((Student) p).getStudentNumber())) {
                throw new IllegalStateException("Student number " + ((Student) p).getStudentNumber()
                        + " is already registered in the system.");
            }
            if (p instanceof Faculty && x instanceof Faculty
                    && ((Faculty) x).getFacultyNumber().equalsIgnoreCase(((Faculty) p).getFacultyNumber())) {
                throw new IllegalStateException("Faculty ID " + ((Faculty) p).getFacultyNumber()
                        + " is already registered in the system.");
            }
        }
    }

    /** Saves a participant. Adding the same object twice is allowed and does nothing. */
    public void addParticipant(Participant p) {
        if (p == null) throw new IllegalArgumentException("Participant is required.");
        if (participants.contains(p)) return;
        checkParticipantNotDuplicate(p);
        participants.add(p);
        try {
            saveParticipants();
        } catch (StorageException ex) {
            participants.remove(p);
            throw ex;
        }
    }

    public List<Participant> getAllParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public Organization findOrganizationById(int id) {
        for (Organization o : organizations) if (o.getOrgId() == id) return o;
        return null;
    }

    public List<Organization> getOrganizations() { return Collections.unmodifiableList(organizations); }

    public Organization addOrganization(String name, String description) {
        for (Organization o : organizations)
            if (o.getOrgName().equalsIgnoreCase(name == null ? "" : name.trim()))
                throw new IllegalStateException("An organization named \"" + o.getOrgName() + "\" already exists.");
        Organization org = new Organization(name, description);
        organizations.add(org);
        try {
            saveOrganizations();
        } catch (StorageException ex) {
            organizations.remove(org);
            throw ex;
        }
        return org;
    }

    public Organizer findOrganizer(String username) {
        if (username == null) return null;
        for (Organizer o : organizers)
            if (username.trim().equalsIgnoreCase(o.getUsername())) return o;
        return null;
    }

    public List<Organizer> getOrganizers() { return Collections.unmodifiableList(organizers); }

    /** Saves an organizer (which needs a username) and its organization. */
    public void addOrganizer(Organizer o) {
        if (o == null) throw new IllegalArgumentException("Organizer is required.");
        if (o.getUsername() == null) throw new IllegalArgumentException("An organizer must be linked to a username.");
        if (findOrganizer(o.getUsername()) != null)
            throw new IllegalStateException("An organizer for " + o.getUsername() + " already exists.");
        boolean newOrg = !organizations.contains(o.getOrganization());
        if (newOrg) organizations.add(o.getOrganization());
        organizers.add(o);
        try {
            if (newOrg) saveOrganizations();
            saveOrganizers();
        } catch (StorageException ex) {
            organizers.remove(o);
            if (newOrg) organizations.remove(o.getOrganization());
            throw ex;
        }
    }

    // =====================================================================
    //  REGISTRATION
    // =====================================================================
    public Registration findRegistrationById(int id) {
        for (Registration r : registrations) if (r.getRegistrationId() == id) return r;
        return null;
    }

    /** Registers a participant for an event. A new participant is saved automatically. */
    public Registration register(Participant p, Event e) {
        if (p == null || e == null) throw new IllegalArgumentException("Select both a participant and an event.");
        if (!events.contains(e)) throw new IllegalArgumentException("That event no longer exists.");
        for (Registration r : registrations) {
            if (r.isActive() && r.getParticipant() == p && r.getEvent() == e) {
                throw new IllegalStateException(p.getFullName() + " is already registered for this event.");
            }
        }
        if (!e.getVenue().isAvailableFor(countActive(e) + 1)) {
            throw new IllegalStateException("Venue is full (capacity " + e.getVenue().getCapacity() + ").");
        }
        boolean newParticipant = !participants.contains(p);
        if (newParticipant) checkParticipantNotDuplicate(p);

        Registration r = new Registration(p, e, LocalDate.now(), Registration.REGISTERED);
        if (newParticipant) participants.add(p);
        registrations.add(r);
        try {
            if (newParticipant) saveParticipants();
            saveRegistrations();
        } catch (StorageException ex) {
            registrations.remove(r);
            if (newParticipant) participants.remove(p);
            throw ex;
        }
        return r;
    }

    public void cancelRegistration(Registration r) {
        if (r == null || !registrations.contains(r)) throw new IllegalArgumentException("Select a registration first.");
        if (!r.isActive()) throw new IllegalStateException("This registration is already cancelled.");
        Attendance a = getAttendance(r);
        boolean wasAttended = a != null && a.isAttended();
        r.cancel();
        if (a != null) a.markAbsent();
        try {
            saveRegistrations();
            saveAttendance();
        } catch (StorageException ex) {
            r.reactivate();
            if (a != null && wasAttended) a.markPresent();
            throw ex;
        }
    }

    public List<Registration> getRegistrationsFor(Event e) {
        List<Registration> out = new ArrayList<>();
        for (Registration r : registrations) {
            if (r.getEvent() == e) out.add(r);
        }
        return out;
    }

    public List<Registration> getAllRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public int countActive(Event e) {
        int n = 0;
        for (Registration r : registrations) {
            if (r.getEvent() == e && r.isActive()) n++;
        }
        return n;
    }

    /** Active registrations of one participant type: "Student", "Faculty" or "Others". */
    public int countActiveByType(Event e, String typeName) {
        int n = 0;
        for (Registration r : registrations) {
            if (r.getEvent() == e && r.isActive()
                    && r.getParticipant().getClass().getSimpleName().equals(typeName)) n++;
        }
        return n;
    }

    public int getTotalRegistrations() {
        return registrations.size();
    }

    // =====================================================================
    //  ATTENDANCE
    // =====================================================================
    public Attendance getAttendance(Registration r) {
        for (Attendance a : attendances) {
            if (a.getRegistration() == r) return a;
        }
        return null;
    }

    public List<Attendance> getAllAttendances() {
        return Collections.unmodifiableList(attendances);
    }

    public void markAttendance(Registration r, boolean present) {
        if (r == null || !registrations.contains(r)) throw new IllegalArgumentException("Select a registration first.");
        if (!r.isActive()) throw new IllegalStateException("Cancelled registrations cannot be marked.");
        Attendance a = getAttendance(r);
        boolean created = (a == null);
        boolean wasAttended = !created && a.isAttended();
        if (created) {
            a = new Attendance(r);
            attendances.add(a);
        }
        if (present) a.markPresent(); else a.markAbsent();
        try {
            saveAttendance();
        } catch (StorageException ex) {
            if (created) {
                attendances.remove(a);
            } else if (wasAttended) {
                a.markPresent();
            } else {
                a.markAbsent();
            }
            throw ex;
        }
    }

    public int countAttended(Event e) {
        int n = 0;
        for (Attendance a : attendances) {
            if (a.getRegistration().getEvent() == e && a.isAttended()) n++;
        }
        return n;
    }

    // =====================================================================
    //  POLYMORPHISM REPORT DEMONSTRATION
    // =====================================================================
    /** Runtime polymorphism: each Event runs its own displayDetails(). */
    public String generatePolymorphicEventReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CAMPUS EVENT POLYMORPHIC REPORT ===\n\n");
        for (Event e : events) {
            sb.append(e.displayDetails()).append("\n");
            sb.append("Registered: ").append(countActive(e)).append(" / ").append(e.getVenue().getCapacity()).append("\n");
            sb.append("Attended  : ").append(countAttended(e)).append("\n");
            sb.append("---------------------------------------\n");
        }
        return sb.toString();
    }
}
