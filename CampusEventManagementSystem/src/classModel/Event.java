package classModel;

import CEMS.Main;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Common blueprint for every campus event. Subclasses (Workshop, Seminar)
 * describe themselves differently through the abstract methods (polymorphism).
 */
public abstract class Event {
    protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    protected static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final int eventId;
    private String eventName;
    private LocalDate date;
    private LocalTime startTime;
    private Venue venue;

    public Event(int eventId, String eventName, LocalDate date, LocalTime startTime, Venue venue) {
        if (eventId <= 0) throw new IllegalArgumentException("Event ID must be a positive number.");
        this.eventId = eventId;
        Main.syncEventId(eventId);          // keeps the ID counter ahead of any loaded ID
        setEventName(eventName);
        setDate(date);
        setStartTime(startTime);
        setVenue(venue);
    }

    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public Venue getVenue() { return venue; }

    public void setEventName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Event name is required.");
        this.eventName = name.trim();
    }
    public void setDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Event date is required.");
        this.date = date;
    }
    public void setStartTime(LocalTime startTime) {
        if (startTime == null) throw new IllegalArgumentException("Start time is required.");
        this.startTime = startTime;
    }
    public void setVenue(Venue venue) {
        if (venue == null) throw new IllegalArgumentException("A venue must be assigned.");
        this.venue = venue;
    }

    /** Subclass label such as "Workshop" or "Seminar". */
    public abstract String getEventType();

    /** Each subclass describes itself differently (polymorphism). */
    public abstract String displayDetails();

    /** Speaker(s) of the event, comma separated. */
    public abstract String getSpeakerNames();

    /** Workshop topic or Seminar subject. */
    public abstract String getTopicOrSubject();

    /** Materials needed. Only workshops have materials, so the default is empty. */
    public String getMaterials() { return ""; }

    /**
     * Changes the type-specific details. Validates everything first so a bad
     * value never leaves the event half-updated. Seminars ignore 'materials'.
     */
    public abstract void updateTypeDetails(String speakers, String topicOrSubject, String materials);

    /** Common header shared by every event type. */
    protected String baseDetails() {
        return "[" + getEventType().toUpperCase() + " #" + eventId + "] " + eventName
                + "\nDate   : " + date.format(DATE_FMT)
                + "\nTime   : " + startTime.format(TIME_FMT)
                + "\nVenue  : " + venue.getVenueName() + " (" + venue.getLocation() + ")";
    }

    @Override
    public String toString() { return eventName; }
}
