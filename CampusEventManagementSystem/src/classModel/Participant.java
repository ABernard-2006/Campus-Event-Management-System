/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;

/**
 *
 * @author bernard_07
 */

import CEMS.Main;

public abstract class Participant {
    private int participantId;
    private String fullName;
    private String contactInfo;

    // Constructor automatically generates participantId
    public Participant(String fullName, String contactInfo) {
        this.participantId = Main.generateParticipantId(); // Auto-generated ID[cite: 8]
        this.fullName = fullName;
        this.contactInfo = contactInfo;
    }

    public int getParticipantId() { return participantId; }
    public String getFullName() { return fullName; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    // Abstract method to be overridden by subclasses[cite: 7, 9]
    public abstract String displayDetails();
}
