package com.system.service;

import classModel.Attendance;
import classModel.Event;
import classModel.Registration;
import classModel.Seminar;
import classModel.UserAccount;
import classModel.Venue;
import classModel.Workshop;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Creating, editing and deleting events (staff only), together with every rule an event must satisfy:
 * field validation, duration range, venue/capacity agreement (through {@link VenueService}), venue schedule
 * conflicts, and "the venue must still hold everyone who is registered" (through {@link RegistrationService}).
 *
 * Schedule-conflict detection ({@link #checkConflict}) stays in this class on purpose: it is only ever used by
 * create/edit, it needs no state of its own, and it is short, so a separate scheduling class would only add
 * a hop between the two.
 *
 * Every change is saved immediately. If saving fails, the in-memory change is undone and a StorageException
 * is thrown. Questions about events (search, lists, ordering) are in {@link EventQueryService}.
 */
public class EventService {
    public static final int MAX_EVENT_NAME_LENGTH = 100;
    public static final int MAX_VENUE_NAME_LENGTH = 60;
    public static final String TYPE_WORKSHOP = "Workshop";
    public static final String TYPE_SEMINAR = "Seminar";

    private final DataRepository data;
    private final AccessControl access;
    private final ChangeNotifier changes;
    private final VenueService venues;
    private final RegistrationService registrations;

    EventService(DataRepository data, AccessControl access, ChangeNotifier changes,
                 VenueService venues, RegistrationService registrations) {
        this.data = data;
        this.access = access;
        this.changes = changes;
        this.venues = venues;
        this.registrations = registrations;
    }

    /**
     * Rejects an event whose time slot overlaps another event at the SAME venue. Events at different
     * venues never conflict. Each event occupies [start, start + its own durationMinutes): two events
     * conflict when startA < endB && startB < endA, so an event that begins exactly when another ends
     * does NOT conflict. Works across midnight because full date-times are compared.
     *
     * @param ignore the event being edited (so it never conflicts with itself), or null when creating
     */
    private void checkConflict(Event ignore, Venue venue, LocalDate date, LocalTime time, int durationMinutes) {
        LocalDateTime newStart = date.atTime(time);
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);
        for (Event e : data.events()) {
            if (e == ignore) continue;
            if (e.getVenue().getVenueId() != venue.getVenueId()) continue;
            LocalDateTime start = e.getStartDateTime();
            LocalDateTime end = e.getEndDateTime();
            if (newStart.isBefore(end) && start.isBefore(newEnd)) {
                throw new IllegalStateException("Schedule conflict: \"" + e.getEventName() + "\" already uses "
                        + e.getVenue().getVenueName() + " on " + e.getDate() + " from " + e.getTimeRangeText()
                        + ". Your event would run " + time.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        + " to " + newEnd.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
                        + " (" + Event.formatDuration(durationMinutes) + "), so pick another time, duration or venue.");
            }
        }
    }

    /** Creates an event. Only an Administrator or Organizer may do this. */
    public void addEvent(UserAccount actor, Event event) {
        access.requireStaff(actor, "create events");
        if (event == null) throw new IllegalArgumentException("Event is required.");
        if (data.events().contains(event)) throw new IllegalStateException("This event has already been added.");
        Venue venue = venues.resolveVenue(event.getVenue());
        Event.validateDuration(event.getDurationMinutes());
        checkConflict(null, venue, event.getDate(), event.getStartTime(), event.getDurationMinutes());

        boolean newVenue = !venues.isSaved(venue);
        event.setVenue(venue);
        if (newVenue) data.venues().add(venue);
        data.events().add(event);
        try {
            if (newVenue) data.saveVenues();
            data.saveEvents();
        } catch (StorageException ex) {
            data.events().remove(event);
            if (newVenue) data.venues().remove(venue);
            throw ex;
        }
        changes.fireChanged();
    }

    /**
     * Changes an existing event. The event type (Workshop/Seminar) cannot change.
     * 'materials' is ignored for seminars.
     */
    public void updateEvent(UserAccount actor, Event event, String name, LocalDate date, LocalTime time,
                            int durationMinutes, Venue venue,
                            String speakers, String topicOrSubject, String materials) {
        access.requireStaff(actor, "edit events");
        if (event == null || !data.events().contains(event)) throw new IllegalArgumentException("Select an event to update.");
        if (date == null) throw new IllegalArgumentException("Event date is required.");
        if (time == null) throw new IllegalArgumentException("Start time is required.");
        Event.validateDuration(durationMinutes);           // reject a bad duration before anything changes
        Venue newVenue = venues.resolveVenue(venue);
        checkConflict(event, newVenue, date, time, durationMinutes);
        int registered = registrations.countActive(event);
        if (registered > newVenue.getCapacity())
            throw new IllegalStateException("That venue holds only " + newVenue.getCapacity()
                    + " people but " + registered + " are already registered.");

        String oldName = event.getEventName();
        LocalDate oldDate = event.getDate();
        LocalTime oldTime = event.getStartTime();
        int oldDuration = event.getDurationMinutes();
        Venue oldVenue = event.getVenue();
        String oldSpeakers = event.getSpeakerNames();
        String oldTopic = event.getTopicOrSubject();
        String oldMaterials = event.getMaterials();
        boolean venueIsNew = !venues.isSaved(newVenue);
        try {
            event.setEventName(name);
            event.setDate(date);
            event.setStartTime(time);
            event.setDurationMinutes(durationMinutes);
            event.setVenue(newVenue);
            event.updateTypeDetails(speakers, topicOrSubject, materials);
            if (venueIsNew) {
                data.venues().add(newVenue);
                data.saveVenues();
            }
            data.saveEvents();
        } catch (RuntimeException ex) {          // undo everything, then report the original problem
            event.setEventName(oldName);
            event.setDate(oldDate);
            event.setStartTime(oldTime);
            event.setDurationMinutes(oldDuration);
            event.setVenue(oldVenue);
            event.updateTypeDetails(oldSpeakers, oldTopic, oldMaterials);
            if (venueIsNew) data.venues().remove(newVenue);
            throw ex;
        }
        changes.fireChanged();
    }

    // ---------------------------------------------------------------------
    //  Event create / edit from plain form values (Part 6.1)
    //  The screen only reads its fields and checks they are filled in / numeric.
    //  Every business rule below is enforced here, so it holds without the form.
    // ---------------------------------------------------------------------

    /**
     * Checks the rules every event must satisfy, before anything is created or changed.
     *
     * @param dateMustBeFuture true when the date is new (creating, or editing to a different date):
     *                         a date before today is rejected. An unchanged date is never re-checked,
     *                         so an old event can still be edited.
     */
    private void validateEventFields(String type, String name, LocalDate date, LocalTime time,
                                     String venueName, int capacity, String speakers,
                                     String topicOrSubject, String materials, boolean dateMustBeFuture) {
        String n = name == null ? "" : name.trim();
        String v = venueName == null ? "" : venueName.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Event name is required.");
        if (n.length() > MAX_EVENT_NAME_LENGTH)
            throw new IllegalArgumentException("Event name must be " + MAX_EVENT_NAME_LENGTH + " characters or fewer.");
        if (v.isEmpty()) throw new IllegalArgumentException("Venue is required.");
        if (v.length() > MAX_VENUE_NAME_LENGTH)
            throw new IllegalArgumentException("Venue name must be " + MAX_VENUE_NAME_LENGTH + " characters or fewer.");
        if (!TYPE_WORKSHOP.equals(type) && !TYPE_SEMINAR.equals(type))
            throw new IllegalArgumentException("Choose an event type (Workshop or Seminar).");
        if (date == null) throw new IllegalArgumentException("Event date is required.");
        if (time == null) throw new IllegalArgumentException("Start time is required.");
        if (dateMustBeFuture && date.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Event date cannot be in the past.");
        if (capacity <= 0) throw new IllegalArgumentException("Max capacity must be greater than zero.");
        if (speakers == null || speakers.trim().isEmpty()) throw new IllegalArgumentException("Add at least one speaker.");
        String t = topicOrSubject == null ? "" : topicOrSubject.trim();
        if (TYPE_WORKSHOP.equals(type)) {
            if (t.isEmpty()) throw new IllegalArgumentException("Workshop topic is required.");
            if (materials == null || materials.trim().isEmpty())
                throw new IllegalArgumentException("Workshop materials are required.");
        } else if (t.isEmpty()) {
            throw new IllegalArgumentException("Seminar subject is required.");
        }
    }

    /**
     * Creates a Workshop or Seminar from form values. Staff only.
     * Enforces: permission, required fields and lengths, type, date not in the past, duration range,
     * venue/capacity agreement and venue schedule conflicts. Nothing is changed (and no ID is used up)
     * when any rule fails.
     *
     * @param type            "Workshop" or "Seminar"
     * @param speakers        comma-separated speaker names
     * @param topicOrSubject  workshop topic, or seminar subject
     * @param materials       workshop materials (ignored for seminars)
     * @return the saved event
     * @throws SecurityException        when the actor is not an Administrator or Organizer
     * @throws IllegalArgumentException when a field is missing or invalid
     * @throws IllegalStateException    on a venue schedule conflict (StorageException if saving fails)
     */
    public Event createEvent(UserAccount actor, String type, String name, LocalDate date, LocalTime time,
                             int durationMinutes, String venueName, int capacity, String speakers,
                             String topicOrSubject, String materials) {
        access.requireStaff(actor, "create events");
        validateEventFields(type, name, date, time, venueName, capacity, speakers, topicOrSubject, materials, true);
        Event.validateDuration(durationMinutes);
        Venue venue = venues.venueFor(venueName, capacity);
        // Check the schedule BEFORE building the event so a rejected attempt does not consume an event ID.
        if (venues.isSaved(venue)) checkConflict(null, venue, date, time, durationMinutes);
        Event event;
        if (TYPE_WORKSHOP.equals(type)) {
            event = new Workshop(name, date, time, durationMinutes, venue,
                    topicOrSubject.trim(), materials.trim(), speakers.trim());
        } else {
            event = new Seminar(name, date, time, durationMinutes, venue, speakers.trim(), topicOrSubject.trim());
        }
        addEvent(actor, event);
        return event;
    }

    /**
     * Edits an existing event from form values. Staff only. The event type cannot change.
     * Same rules as {@link #createEvent}, except that the date is only checked against "today"
     * when it was actually changed, plus the existing rule that the new venue must still hold
     * everyone who is registered.
     */
    public void updateEvent(UserAccount actor, Event event, String name, LocalDate date, LocalTime time,
                            int durationMinutes, String venueName, int capacity, String speakers,
                            String topicOrSubject, String materials) {
        access.requireStaff(actor, "edit events");
        if (event == null || !data.events().contains(event)) throw new IllegalArgumentException("Select an event to update.");
        boolean dateChanged = date == null || !event.getDate().equals(date);
        validateEventFields(event.getEventType(), name, date, time, venueName, capacity, speakers,
                topicOrSubject, materials, dateChanged);
        Event.validateDuration(durationMinutes);
        updateEvent(actor, event, name, date, time, durationMinutes, venues.venueFor(venueName, capacity),
                speakers.trim(), topicOrSubject.trim(), materials == null ? "" : materials.trim());
    }

    /**
     * Deletes an event together with its registrations and attendance records (staff only).
     * The three files are saved together; if any save fails the three in-memory lists are restored.
     */
    public void deleteEvent(UserAccount actor, Event event) {
        access.requireStaff(actor, "delete events");
        if (event == null || !data.events().contains(event)) throw new IllegalArgumentException("Select an event to delete.");
        List<Event> oldEvents = new ArrayList<>(data.events());
        List<Registration> oldRegistrations = new ArrayList<>(data.registrations());
        List<Attendance> oldAttendances = new ArrayList<>(data.attendances());

        data.attendances().removeIf(a -> a.getRegistration().getEvent() == event);
        data.registrations().removeIf(r -> r.getEvent() == event);
        data.events().remove(event);
        try {
            data.saveAttendance();
            data.saveRegistrations();
            data.saveEvents();
        } catch (StorageException ex) {
            data.events().clear();
            data.events().addAll(oldEvents);
            data.registrations().clear();
            data.registrations().addAll(oldRegistrations);
            data.attendances().clear();
            data.attendances().addAll(oldAttendances);
            throw ex;
        }
        changes.fireChanged();
    }
}
