package com.system.service;

import classModel.Attendance;
import classModel.Event;
import classModel.Organizer;
import classModel.Participant;
import classModel.Registration;
import classModel.Student;
import classModel.Venue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service Manager handling business logic for Events, Participants, Registrations, and Attendance.
 */
public class EventManager {
    private final List<Event> events = new ArrayList<>();
    private final List<Participant> participants = new ArrayList<>();
    private final List<Venue> venues = new ArrayList<>();
    private final List<Organizer> organizers = new ArrayList<>();
    private final List<Registration> registrations = new ArrayList<>();
    private final List<Attendance> attendances = new ArrayList<>();

    // ---------- EVENTS ----------
    public void addEvent(Event event) {
        if (event == null) throw new IllegalArgumentException("Event is required.");
        for (Event e : events) {
            // Compare venueId or location instead of memory reference ==
            boolean sameVenue = e.getVenue().getVenueId() == event.getVenue().getVenueId();
            boolean sameDate = e.getDate().equals(event.getDate());
            boolean sameTime = e.getStartTime().equals(event.getStartTime());

            if (sameVenue && sameDate && sameTime) {
                throw new IllegalStateException("Schedule conflict: \"" + e.getEventName()
                        + "\" already uses " + e.getVenue().getVenueName() + " at that date and time.");
            }
        }
        events.add(event);
    }

    /** Matches keyword against event name, type, and venue (case-insensitive). */
    public List<Event> searchEvent(String keyword) {
        List<Event> result = new ArrayList<>();
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Event e : events) {
            if (k.isEmpty() || e.getEventName().toLowerCase().contains(k)
                    || e.getEventType().toLowerCase().contains(k)
                    || e.getVenue().getVenueName().toLowerCase().contains(k)) {
                result.add(e);
            }
        }
        return result;
    }

    public List<Event> getAllEvents() { 
        return Collections.unmodifiableList(events); 
    }

    // ---------- PARTICIPANTS / ORGANIZERS / VENUES ----------
    public void addParticipant(Participant p) {
        if (p == null) throw new IllegalArgumentException("Participant is required.");
        if (p instanceof Student) {
            String no = ((Student) p).getStudentNumber();
            for (Participant x : participants) {
                if (x instanceof Student && ((Student) x).getStudentNumber().equalsIgnoreCase(no)) {
                    throw new IllegalStateException("Student number " + no + " is already registered in the system.");
                }
            }
        }
        participants.add(p);
    }

    public List<Participant> getAllParticipants() { 
        return Collections.unmodifiableList(participants); 
    }

    // Dedicated Organizer handling (Fixes ClassCastException)
    public void addOrganizer(Organizer o) {
        if (o == null) throw new IllegalArgumentException("Organizer is required.");
        organizers.add(o);
    }

    public List<Organizer> getOrganizers() { 
        return Collections.unmodifiableList(organizers); 
    }

    public void addVenue(Venue v) { 
        if (v == null) throw new IllegalArgumentException("Venue is required.");
        venues.add(v); 
    }

    public List<Venue> getVenues() { 
        return Collections.unmodifiableList(venues); 
    }

    // ---------- REGISTRATION ----------
    public Registration register(Participant p, Event e) {
        if (p == null || e == null) throw new IllegalArgumentException("Select both a participant and an event.");
        for (Registration r : registrations) {
            if (r.isActive() && r.getParticipant() == p && r.getEvent() == e) {
                throw new IllegalStateException(p.getFullName() + " is already registered for this event.");
            }
        }
        if (!e.getVenue().isAvailableFor(countActive(e) + 1)) {
            throw new IllegalStateException("Venue is full (capacity " + e.getVenue().getCapacity() + ").");
        }
        Registration r = new Registration(p, e, LocalDate.now(), Registration.REGISTERED);
        registrations.add(r);
        return r;
    }

    public void cancelRegistration(Registration r) {
        if (r == null) throw new IllegalArgumentException("Select a registration first.");
        r.cancel();
        Attendance a = getAttendance(r);
        if (a != null) a.markAbsent();
    }

    public List<Registration> getRegistrationsFor(Event e) {
        List<Registration> out = new ArrayList<>();
        for (Registration r : registrations) {
            if (r.getEvent() == e) out.add(r);
        }
        return out;
    }

    public int countActive(Event e) {
        int n = 0;
        for (Registration r : registrations) {
            if (r.getEvent() == e && r.isActive()) n++;
        }
        return n;
    }

    public int getTotalRegistrations() { 
        return registrations.size(); 
    }

    // ---------- ATTENDANCE ----------
    public Attendance getAttendance(Registration r) {
        for (Attendance a : attendances) {
            if (a.getRegistration() == r) return a;
        }
        return null;
    }

    public void markAttendance(Registration r, boolean present) {
        if (r == null) throw new IllegalArgumentException("Select a registration first.");
        if (!r.isActive()) throw new IllegalStateException("Cancelled registrations cannot be marked.");
        Attendance a = getAttendance(r);
        if (a == null) { 
            a = new Attendance(r); 
            attendances.add(a); 
        }
        if (present) a.markPresent(); else a.markAbsent();
    }

    public int countAttended(Event e) {
        int n = 0;
        for (Attendance a : attendances) {
            if (a.getRegistration().getEvent() == e && a.isAttended()) n++;
        }
        return n;
    }

    // ---------- POLYMORPHISM REPORT DEMONSTRATION ----------
    /**
     * Generates a string report demonstrating Runtime Polymorphism across all registered events.
     */
    public String generatePolymorphicEventReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CAMPUS EVENT POLYMORPHIC REPORT ===\n\n");
        for (Event e : events) {
            // Polymorphic call: executes Workshop or Seminar displayDetails() at runtime
            sb.append(e.displayDetails()).append("\n");
            sb.append("Registered: ").append(countActive(e)).append(" / ").append(e.getVenue().getCapacity()).append("\n");
            sb.append("Attended  : ").append(countAttended(e)).append("\n");
            sb.append("---------------------------------------\n");
        }
        return sb.toString();
    }
}