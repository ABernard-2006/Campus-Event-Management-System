package com.system.service;

import classModel.Venue;
import java.util.Collections;
import java.util.List;

/**
 * Venues: look-up, adding a venue, and the rule that decides WHICH venue object an event uses.
 *
 * Two events typed as "Room 101" must share ONE Venue object, because the schedule-conflict rule
 * ({@link EventService}) compares venues by id. The same-name-and-capacity rule is therefore kept here, in one
 * place, and used by both ways of creating an event (a ready-made Event object, or plain form values).
 */
public class VenueService {
    private final DataRepository data;

    VenueService(DataRepository data) {
        this.data = data;
    }

    public Venue findVenueById(int id) { return data.findVenueById(id); }

    public Venue findVenueByName(String name) { return data.findVenueByName(name); }

    public List<Venue> getVenues() { return Collections.unmodifiableList(data.venues()); }

    public void addVenue(Venue v) {
        if (v == null) throw new IllegalArgumentException("Venue is required.");
        if (data.venues().contains(v)) return;
        if (data.findVenueByName(v.getVenueName()) != null)
            throw new IllegalStateException("A venue named \"" + v.getVenueName() + "\" already exists.");
        data.venues().add(v);
        try {
            data.saveVenues();
        } catch (StorageException ex) {
            data.venues().remove(v);
            throw ex;
        }
    }

    /** True when this exact Venue object is already saved. */
    boolean isSaved(Venue v) {
        return data.venues().contains(v);
    }

    private static String capacityMismatchMessage(Venue existing) {
        return "The venue \"" + existing.getVenueName() + "\" already exists with a capacity of "
                + existing.getCapacity() + ". Use that capacity or choose a different venue name.";
    }

    /**
     * Decides which Venue object an event should use: the same object if it is already known,
     * the existing venue with the same name, or the new venue itself. Nothing is changed here.
     * Used for events built as objects (EventService.addEvent / the object form of updateEvent).
     *
     * @throws IllegalStateException when the name exists with a different capacity
     */
    Venue resolveVenue(Venue wanted) {
        if (wanted == null) throw new IllegalArgumentException("A venue must be assigned.");
        if (data.venues().contains(wanted)) return wanted;
        Venue existing = data.findVenueByName(wanted.getVenueName());
        if (existing == null) return wanted;
        if (existing.getCapacity() != wanted.getCapacity()) throw new IllegalStateException(capacityMismatchMessage(existing));
        return existing;
    }

    /**
     * Venue to use for a typed name + capacity: the saved venue with that name (its capacity must match),
     * otherwise a NEW unsaved venue (location "TBA"). Used for events built from form values.
     *
     * @throws IllegalArgumentException when the name exists with a different capacity
     */
    Venue venueFor(String venueName, int capacity) {
        Venue known = data.findVenueByName(venueName);
        if (known == null) return new Venue(venueName.trim(), "TBA", capacity);
        if (known.getCapacity() != capacity) throw new IllegalArgumentException(capacityMismatchMessage(known));
        return known;
    }
}
