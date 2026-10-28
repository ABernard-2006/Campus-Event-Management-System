package classModel;

import CEMS.Main;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Common blueprint for every campus event. Subclasses (Workshop, Seminar)
 * describe themselves differently through the abstract methods (polymorphism).
 */
public abstract class Event {
    protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    protected static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    /** Shortest allowed event, in minutes. */
    public static final int MIN_DURATION_MINUTES = 1;
    /** Longest allowed event, in minutes (24 hours). Keeps every event within one midnight crossing. */
    public static final int MAX_DURATION_MINUTES = 24 * 60;
    /** Duration given to saved events that were created before durations existed (the old fixed 2 hours). */
    public static final int LEGACY_DURATION_MINUTES = 120;
    /** Duration pre-filled in the Event form when creating a new event. */
    public static final int DEFAULT_NEW_EVENT_DURATION_MINUTES = 60;

    private final int eventId;
    private String eventName;
    private LocalDate date;
    private LocalTime startTime;
    private int durationMinutes;
    private Venue venue;

    public Event(int eventId, String eventName, LocalDate date, LocalTime startTime,
                 int durationMinutes, Venue venue) {
        if (eventId <= 0) throw new IllegalArgumentException("Event ID must be a positive number.");
        this.eventId = eventId;
        Main.syncEventId(eventId);          // keeps the ID counter ahead of any loaded ID
        setEventName(eventName);
        setDate(date);
        setStartTime(startTime);
        setDurationMinutes(durationMinutes);
        setVenue(venue);
    }

    /**
     * Checks a duration and returns it. Used by the setter and by services that must reject a bad
     * value BEFORE changing anything.
     * @throws IllegalArgumentException when the value is outside MIN..MAX_DURATION_MINUTES
     */
    public static int validateDuration(int minutes) {
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES)
            throw new IllegalArgumentException("Duration must be a whole number of minutes from "
                    + MIN_DURATION_MINUTES + " to " + MAX_DURATION_MINUTES + " (24 hours).");
        return minutes;
    }

    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public Venue getVenue() { return venue; }

    /** When the event starts (date + start time). */
    public LocalDateTime getStartDateTime() { return date.atTime(startTime); }

    /** When the event ends: start + duration. May fall on the next calendar day. */
    public LocalDateTime getEndDateTime() { return getStartDateTime().plusMinutes(durationMinutes); }

    /** Time of day at which the event ends. */
    public LocalTime getEndTime() { return getEndDateTime().toLocalTime(); }

    /** True when the event ends on a later calendar day than it starts. */
    public boolean endsNextDay() { return !getEndDateTime().toLocalDate().equals(date); }

    /** "09:00 AM - 10:30 AM", with " (+1 day)" appended when the event runs past midnight. */
    public String getTimeRangeText() {
        return startTime.format(TIME_FMT) + " - " + getEndTime().format(TIME_FMT) + (endsNextDay() ? " (+1 day)" : "");
    }

    /** Human friendly length such as "90 min (1 hr 30 min)". */
    public String getDurationText() { return formatDuration(durationMinutes); }

    /** 45 -> "45 min", 120 -> "2 hr", 90 -> "1 hr 30 min". */
    public static String formatDuration(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + " hr";
        return h + " hr " + m + " min";
    }

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
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = validateDuration(durationMinutes);
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
                + "\nTime   : " + getTimeRangeText()
                + "\nLength : " + getDurationText() + " (" + durationMinutes + " minutes)"
                + "\nVenue  : " + venue.getVenueName() + " (" + venue.getLocation() + ")";
    }

    @Override
    public String toString() { return eventName; }
}
