package classModel;

import CEMS.Main;
import java.time.LocalDate;
import java.time.LocalTime;

/** A presentation-based event. */
public class Seminar extends Event {
    private String speakerName;
    private String subject;

    /** Creates a NEW seminar with an automatically generated ID. */
    public Seminar(String eventName, LocalDate date, LocalTime startTime, Venue venue,
                   String speakerName, String subject) {
        this(Main.generateEventId(), eventName, date, startTime, venue, speakerName, subject);
    }

    /** Rebuilds a seminar that was saved earlier (keeps its saved ID). */
    public Seminar(int eventId, String eventName, LocalDate date, LocalTime startTime, Venue venue,
                   String speakerName, String subject) {
        super(eventId, eventName, date, startTime, venue);
        this.speakerName = require(speakerName, "Speaker name");
        this.subject = require(subject, "Subject");
    }

    private static String require(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return v.trim();
    }

    public String getSpeakerName() { return speakerName; }
    public String getSubject() { return subject; }

    @Override public String getSpeakerNames() { return speakerName; }
    @Override public String getTopicOrSubject() { return subject; }
    @Override public String getEventType() { return "Seminar"; }

    @Override
    public void updateTypeDetails(String speakers, String topicOrSubject, String materials) {
        String s = require(speakers, "Speaker name");
        String t = require(topicOrSubject, "Subject");
        this.speakerName = s;      // materials are not used by seminars
        this.subject = t;
    }

    @Override
    public String displayDetails() {
        return baseDetails() + "\nSpeaker: " + speakerName + "\nSubject: " + subject
                + "\nFormat : Lecture / talk";
    }
}
