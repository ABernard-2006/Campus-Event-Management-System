package classModel;

import CEMS.Main;
import java.time.LocalDate;
import java.time.LocalTime;

/** A hands-on learning event. */
public class Workshop extends Event {
    private String topic;
    private String materials;
    private String speakerName;

    /** Creates a NEW workshop with an automatically generated ID. */
    public Workshop(String eventName, LocalDate date, LocalTime startTime, Venue venue,
                    String topic, String materials, String speakerName) {
        this(Main.generateEventId(), eventName, date, startTime, venue, topic, materials, speakerName);
    }

    /** Rebuilds a workshop that was saved earlier (keeps its saved ID). */
    public Workshop(int eventId, String eventName, LocalDate date, LocalTime startTime, Venue venue,
                    String topic, String materials, String speakerName) {
        super(eventId, eventName, date, startTime, venue);
        this.topic = require(topic, "Topic");
        this.materials = require(materials, "Materials");
        this.speakerName = require(speakerName, "Speaker");
    }

    private static String require(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return v.trim();
    }

    public String getTopic() { return topic; }
    public String getSpeaker() { return speakerName; }

    public void setSpeakerName(String speakerName) {
        this.speakerName = require(speakerName, "Speaker");
    }

    @Override public String getMaterials() { return materials; }
    @Override public String getSpeakerNames() { return speakerName; }
    @Override public String getTopicOrSubject() { return topic; }
    @Override public String getEventType() { return "Workshop"; }

    @Override
    public void updateTypeDetails(String speakers, String topicOrSubject, String materials) {
        String s = require(speakers, "Speaker");
        String t = require(topicOrSubject, "Topic");
        String m = require(materials, "Materials");
        this.speakerName = s;
        this.topic = t;
        this.materials = m;
    }

    @Override
    public String displayDetails() {
        return baseDetails() + "\nTopic  : " + topic + "\nSpeaker: " + speakerName + "\nBring  : " + materials
                + "\nFormat : Hands-on activity";
    }
}
