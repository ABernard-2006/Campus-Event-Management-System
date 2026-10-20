/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.system.model;

/**
 *
 * @author bernard_07
 */
public abstract class Participant
{
    private int participantId;
    private String fullName;
    private String contactInfo;

    public Participant(int participantId, String fullName, String contactInfo) 
    {
        this.participantId = participantId;
        this.fullName = fullName;
        this.contactInfo = contactInfo;
    }

    public int getParticipantId() { return participantId; }
    public String getFullName() { return fullName; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    // Abstract method to be overridden by Student, Faculty, and Others
    public abstract String displayDetails();
}
