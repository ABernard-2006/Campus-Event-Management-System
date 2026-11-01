package com.system.service;

import classModel.Event;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only questions about events: find, search, list, order, and the dates they fall on.
 * Nothing here changes data or needs a permission check, so any screen may use it freely.
 * Changes to events are in {@link EventService}.
 */
public class EventQueryService {
    private static final Comparator<Event> SOONEST_FIRST =
            Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime);

    private final DataRepository data;

    EventQueryService(DataRepository data) {
        this.data = data;
    }

    public Event findEventById(int id) { return data.findEventById(id); }

    /** Matches keyword against event name, type, venue, speaker and topic (case-insensitive). */
    public List<Event> searchEvent(String keyword) {
        List<Event> result = new ArrayList<>();
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Event e : data.events()) {
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
        return Collections.unmodifiableList(data.events());
    }

    /** Events today or later, soonest first. A limit of 0 or less means "all". */
    public List<Event> getUpcomingEvents(int limit) {
        List<Event> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Event e : data.events()) if (!e.getDate().isBefore(today)) result.add(e);
        result.sort(SOONEST_FIRST);
        if (limit > 0 && result.size() > limit) return new ArrayList<>(result.subList(0, limit));
        return result;
    }

    public List<Event> getEventsOn(LocalDate date) {
        List<Event> result = new ArrayList<>();
        for (Event e : data.events()) if (e.getDate().equals(date)) result.add(e);
        result.sort(Comparator.comparing(Event::getStartTime));
        return result;
    }

    /** Every event, earliest first (date, then start time). A new list: safe to modify. */
    public List<Event> getEventsChronological() {
        List<Event> result = new ArrayList<>(data.events());
        result.sort(SOONEST_FIRST);
        return result;
    }

    /** Events matching the keyword (see searchEvent), earliest first. */
    public List<Event> searchEventsChronological(String keyword) {
        List<Event> result = searchEvent(keyword);
        result.sort(SOONEST_FIRST);
        return result;
    }

    /**
     * Orders events for the dashboard cards: upcoming events first (soonest first), then past
     * events (most recent first). Returns a new list; the input is not changed.
     */
    public static List<Event> orderForDisplay(Collection<Event> source) {
        LocalDate today = LocalDate.now();
        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();
        for (Event e : source) (e.getDate().isBefore(today) ? past : upcoming).add(e);
        upcoming.sort(SOONEST_FIRST);
        past.sort(SOONEST_FIRST.reversed());
        upcoming.addAll(past);
        return upcoming;
    }

    /** Every date that has at least one event (for the calendar dots). */
    public Set<LocalDate> getEventDates() {
        Set<LocalDate> dates = new HashSet<>();
        for (Event e : data.events()) dates.add(e.getDate());
        return dates;
    }

    /** {earliest, latest} event date, or {today, today} when there are no events. Used as the default report range. */
    public LocalDate[] getEventDateRange() {
        LocalDate min = null, max = null;
        for (Event e : data.events()) {
            if (min == null || e.getDate().isBefore(min)) min = e.getDate();
            if (max == null || e.getDate().isAfter(max)) max = e.getDate();
        }
        LocalDate today = LocalDate.now();
        return new LocalDate[] { min == null ? today : min, max == null ? today : max };
    }
}
