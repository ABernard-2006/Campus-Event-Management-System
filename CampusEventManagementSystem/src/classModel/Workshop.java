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
public class Workshop extends Event {
    private String topic;
    private String materials;
    private String speakerName;

    public Workshop(String eventName, LocalDate date, LocalTime startTime, Venue venue,
                    String topic, String materials, String speakerName) {
        super(Main.generateId(), eventName, date, startTime, venue);
        this.topic = require(topic, "Topic");
        this.materials = require(materials, "Materials");
        this.speakerName = require(speakerName, "Speaker");
    }

    private static String require(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return v.trim();
    }

    public String getTopic() { return topic; }
    public String getMaterials() { return materials; }
    public String getSpeaker(){ return speakerName; }
    
    public void setSpeakerName(String speakerName){
        this.speakerName = require(speakerName, "Speaker");
    }

    @Override public String getEventType() { return "Workshop"; }

    @Override
    public String displayDetails() {
        return baseDetails() + "\nTopic  : " + topic + "\nSpeaker: " + speakerName + "\nBring  : " + materials
                + "\nFormat : Hands-on activity";
    }
}
