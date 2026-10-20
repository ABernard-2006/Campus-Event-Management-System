/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;

import CEMS.Main;
import java.time.LocalDate;
import java.time.LocalTime;
/**
 *
 * @author ASUS2
 */
public class Seminar extends Event {
    private String speakerName;
    private String subject;

    public Seminar(String eventName, LocalDate date, LocalTime startTime, Venue venue,
                   String speakerName, String subject) {
        super(Main.generateId(), eventName, date, startTime, venue);
        this.speakerName = require(speakerName, "Speaker name");
        this.subject = require(subject, "Subject");
    }

    private static String require(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return v.trim();
    }

    public String getSpeakerName() { return speakerName; }
    public String getSubject() { return subject; }

    @Override public String getEventType() { return "Seminar"; }

    @Override
    public String displayDetails() {
        return baseDetails() + "\nSpeaker: " + speakerName + "\nSubject: " + subject
                + "\nFormat : Lecture / talk";
    }
}
