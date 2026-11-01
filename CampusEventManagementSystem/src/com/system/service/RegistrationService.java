package com.system.service;

import classModel.Attendance;
import classModel.Event;
import classModel.Faculty;
import classModel.Others;
import classModel.Participant;
import classModel.Registration;
import classModel.Student;
import classModel.UserAccount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Participants and their registrations for events: registering (from objects or from form values),
 * cancelling, who may see which registration, searching, and the registration counts.
 *
 * Participant records are managed here because the only way a participant comes into existence in the
 * application is by registering. The rule "what counts as the same student/faculty ID" belongs to the
 * participant classes themselves.
 *
 * Authorization: staff may register or cancel for anyone; a Participant account only for the participant
 * record that carries its own email ({@link AccessControl#isOwnRecord}).
 */
public class RegistrationService {
    public static final String PARTICIPANT_STUDENT = "Student";
    public static final String PARTICIPANT_FACULTY = "Faculty";
    public static final String PARTICIPANT_OTHERS = "Others";

    private final DataRepository data;
    private final AccessControl access;
    private final ChangeNotifier changes;

    RegistrationService(DataRepository data, AccessControl access, ChangeNotifier changes) {
        this.data = data;
        this.access = access;
        this.changes = changes;
    }

    // =====================================================================
    //  PARTICIPANTS
    // =====================================================================
    public Participant findParticipantById(int id) { return data.findParticipantById(id); }

    /** Finds a saved participant: Students/Faculty by their number, Others by full name. Returns null if none. */
    public Participant findParticipant(String type, String number, String name) {
        for (Participant p : data.participants()) {
            if ("Student".equals(type) && p instanceof Student
                    && ((Student) p).getStudentNumber().equalsIgnoreCase(number)) return p;
            if ("Faculty".equals(type) && p instanceof Faculty
                    && ((Faculty) p).getFacultyNumber().equalsIgnoreCase(number)) return p;
            if ("Others".equals(type) && p instanceof Others
                    && p.getFullName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    /** Finds the saved participant whose contact (email) matches, or null. Used to link an account to its profile. */
    public Participant findParticipantByContact(String email) {
        if (email == null) return null;
        for (Participant p : data.participants())
            if (p.hasContact(email)) return p;
        return null;
    }

    /** Saves a participant. Adding the same object twice is allowed and does nothing. */
    public void addParticipant(Participant p) {
        if (p == null) throw new IllegalArgumentException("Participant is required.");
        if (data.participants().contains(p)) return;
        data.checkParticipantNotDuplicate(p);
        data.participants().add(p);
        try {
            data.saveParticipants();
        } catch (StorageException ex) {
            data.participants().remove(p);
            throw ex;
        }
    }

    public List<Participant> getAllParticipants() {
        return Collections.unmodifiableList(data.participants());
    }

    // =====================================================================
    //  REGISTERING
    // =====================================================================
    public Registration findRegistrationById(int id) { return data.findRegistrationById(id); }

    /**
     * Registers a participant for an event. A new participant is saved automatically.
     *
     * Permissions: Administrators and Organizers may register anyone (for example a walk-in guest).
     * A Participant account may only register the participant record that carries its own email.
     *
     * @throws SecurityException     when the actor is not allowed to register this participant
     * @throws IllegalStateException when the event is over, full, or the participant is already registered
     */
    public Registration register(UserAccount actor, Participant p, Event e) {
        if (!access.isKnownAccount(actor)) throw new SecurityException("Log in to register for an event.");
        if (p == null || e == null) throw new IllegalArgumentException("Select both a participant and an event.");
        boolean staff = access.isStaff(actor);
        if (!staff) {
            if (!access.isOwnRecord(actor, p))
                throw new SecurityException("You can only register yourself. Use the email address of your own account.");
            Participant mine = findParticipantByContact(actor.getEmail());
            if (mine != null && mine != p)
                throw new IllegalStateException("Your participant profile already exists. Register using that profile.");
        }
        if (!data.events().contains(e)) throw new IllegalArgumentException("That event no longer exists.");
        if (e.getDate().isBefore(LocalDate.now()))
            throw new IllegalStateException("Registration is closed: \"" + e.getEventName() + "\" has already taken place.");
        for (Registration r : data.registrations()) {
            if (r.isActive() && r.getParticipant() == p && r.getEvent() == e) {
                throw new IllegalStateException(p.getFullName() + " is already registered for this event.");
            }
        }
        if (!e.getVenue().isAvailableFor(countActive(e) + 1)) {
            throw new IllegalStateException("Venue is full (capacity " + e.getVenue().getCapacity() + ").");
        }
        boolean newParticipant = !data.participants().contains(p);
        if (newParticipant) {
            data.checkParticipantNotDuplicate(p);
            Participant sameEmail = findParticipantByContact(p.getContactInfo());
            if (sameEmail != null)
                throw new IllegalStateException("The email " + p.getContactInfo() + " already belongs to "
                        + sameEmail.getFullName() + ". Each participant needs a different email.");
        }

        Registration r = new Registration(p, e, LocalDate.now(), Registration.REGISTERED);
        if (newParticipant) data.participants().add(p);
        data.registrations().add(r);
        try {
            if (newParticipant) data.saveParticipants();
            data.saveRegistrations();
        } catch (StorageException ex) {
            data.registrations().remove(r);
            if (newParticipant) data.participants().remove(p);
            throw ex;
        }
        changes.fireChanged();
        return r;
    }

    /**
     * Registers someone for an event from form values (Part 6.1). Decides WHO is being registered,
     * validates the type-specific fields, builds a new participant when needed and then applies
     * {@link #register}.
     *
     * Who is registered:
     *  - Staff: the saved participant matching the Student/Faculty number (or the Others name),
     *    otherwise a new one built from the values.
     *  - A Participant account: ALWAYS its own profile. {@code contact} is ignored and replaced by the
     *    account's email. If that profile already exists the other fields are ignored (the profile is locked);
     *    otherwise the profile is created from the values.
     *
     * @param type   "Student", "Faculty" or "Others"
     * @param number Student number or Faculty ID (ignored for Others)
     * @param extra  Program (Student), Department (Faculty) or Affiliation (Others)
     * @param extra2 Position (Faculty only)
     * @throws SecurityException        when the actor is not logged in with a saved account
     * @throws IllegalArgumentException when a required field is missing or the contact is malformed
     * @throws IllegalStateException    when the event is over/full or the person is already registered
     */
    public Registration registerForEvent(UserAccount actor, Event event, String type, String name,
                                         String contact, String number, String extra, String extra2) {
        if (!access.isKnownAccount(actor)) throw new SecurityException("Log in to register for an event.");
        if (event == null) throw new IllegalArgumentException("No event selected. There may be no upcoming events yet.");
        boolean staff = access.isStaff(actor);
        Participant p = staff ? null : findParticipantByContact(actor.getEmail());

        if (p == null) {                                   // need to identify / create the participant from the form
            String t = type == null ? "" : type.trim();
            String n = name == null ? "" : name.trim();
            String c = staff ? (contact == null ? "" : contact.trim()) : actor.getEmail();
            String num = number == null ? "" : number.trim();
            String x = extra == null ? "" : extra.trim();
            String x2 = extra2 == null ? "" : extra2.trim();
            if (!PARTICIPANT_STUDENT.equals(t) && !PARTICIPANT_FACULTY.equals(t) && !PARTICIPANT_OTHERS.equals(t))
                throw new IllegalArgumentException("Choose a participant type.");
            if (n.isEmpty()) throw new IllegalArgumentException("Participant name is required.");
            if (c.isEmpty()) throw new IllegalArgumentException("BU/email address is required.");
            if (c.matches(".*\\s+.*")) throw new IllegalArgumentException("BU/email address must not contain spaces.");
            if (PARTICIPANT_STUDENT.equals(t)) {
                if (num.isEmpty()) throw new IllegalArgumentException("Student number is required.");
                if (x.isEmpty()) throw new IllegalArgumentException("Program is required.");
            } else if (PARTICIPANT_FACULTY.equals(t)) {
                if (num.isEmpty()) throw new IllegalArgumentException("Faculty ID is required.");
                if (x.isEmpty()) throw new IllegalArgumentException("Department is required.");
                if (x2.isEmpty()) throw new IllegalArgumentException("Position is required.");
            } else if (x.isEmpty()) {
                throw new IllegalArgumentException("Affiliation / company is required.");
            }
            if (staff) p = findParticipant(t, num, n);
            if (p == null) {
                switch (t) {
                    case PARTICIPANT_STUDENT: p = new Student(n, c, num, x); break;
                    case PARTICIPANT_FACULTY: p = new Faculty(n, c, num, x, x2); break;
                    default:                  p = new Others(n, c, x); break;
                }
            }
        }
        return register(actor, p, event);
    }

    // =====================================================================
    //  CANCELLING
    // =====================================================================
    /**
     * Cancels a registration (the record is kept with the status "Cancelled", so history stays complete).
     * Staff may cancel any registration; a Participant only their own. A cancelled registration can no longer
     * be marked present, so any attendance record it has is reset to absent in the same save.
     */
    public void cancelRegistration(UserAccount actor, Registration r) {
        if (!access.isKnownAccount(actor)) throw new SecurityException("Log in to cancel a registration.");
        if (r == null || !data.registrations().contains(r)) throw new IllegalArgumentException("Select a registration first.");
        if (!access.isStaff(actor) && !access.isOwnRecord(actor, r.getParticipant()))
            throw new SecurityException("You can only cancel your own registrations.");
        if (!r.isActive()) throw new IllegalStateException("This registration is already cancelled.");
        Attendance a = data.getAttendance(r);
        boolean wasAttended = a != null && a.isAttended();
        r.cancel();
        if (a != null) a.markAbsent();
        try {
            data.saveRegistrations();
            data.saveAttendance();
        } catch (StorageException ex) {
            r.reactivate();
            if (a != null && wasAttended) a.markPresent();
            throw ex;
        }
        changes.fireChanged();
    }

    // =====================================================================
    //  LOOKING AT REGISTRATIONS
    // =====================================================================
    /** Registrations for one event, including cancelled ones. Staff only. */
    public List<Registration> getRegistrationsFor(UserAccount actor, Event e) {
        access.requireStaff(actor, "view the registrations of an event");
        return getRegistrationsFor(e);
    }

    /**
     * The registrations this account is allowed to see: everything for staff,
     * only their own for a Participant. Newest registration first.
     */
    public List<Registration> getVisibleRegistrations(UserAccount actor) {
        List<Registration> out = new ArrayList<>();
        if (!access.isKnownAccount(actor)) return out;
        boolean staff = access.isStaff(actor);
        for (Registration r : data.registrations())
            if (staff || access.isOwnRecord(actor, r.getParticipant())) out.add(r);
        Collections.reverse(out);
        return out;
    }

    /**
     * The registrations this account may see (see getVisibleRegistrations), narrowed to those whose
     * participant name, event name, participant type or status contains the keyword.
     * An empty keyword returns everything visible.
     */
    public List<Registration> searchVisibleRegistrations(UserAccount actor, String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Registration> out = new ArrayList<>();
        for (Registration r : getVisibleRegistrations(actor)) {
            if (k.isEmpty()
                    || r.getParticipant().getFullName().toLowerCase().contains(k)
                    || r.getEvent().getEventName().toLowerCase().contains(k)
                    || r.getParticipant().getClass().getSimpleName().toLowerCase().contains(k)
                    || r.getStatus().toLowerCase().contains(k)) {
                out.add(r);
            }
        }
        return out;
    }

    public List<Registration> getRegistrationsFor(Event e) {
        List<Registration> out = new ArrayList<>();
        for (Registration r : data.registrations()) {
            if (r.getEvent() == e) out.add(r);
        }
        return out;
    }

    public List<Registration> getAllRegistrations() {
        return Collections.unmodifiableList(data.registrations());
    }

    // =====================================================================
    //  COUNTS
    // =====================================================================
    /** Registrations of this event that are still valid (not cancelled). */
    public int countActive(Event e) {
        int n = 0;
        for (Registration r : data.registrations()) {
            if (r.getEvent() == e && r.isActive()) n++;
        }
        return n;
    }

    /** Active registrations of one participant type: "Student", "Faculty" or "Others". */
    public int countActiveByType(Event e, String typeName) {
        int n = 0;
        for (Registration r : data.registrations()) {
            if (r.getEvent() == e && r.isActive()
                    && r.getParticipant().getClass().getSimpleName().equals(typeName)) n++;
        }
        return n;
    }

    public int getTotalRegistrations() {
        return data.registrations().size();
    }
}
