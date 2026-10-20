/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
/**
 *
 * @author ASUS2
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
        this.eventId = eventId;
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
