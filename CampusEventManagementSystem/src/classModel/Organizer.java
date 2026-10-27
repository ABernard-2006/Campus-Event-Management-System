/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;

/**
 *
 * @author bernard_07
 */

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a person responsible for planning and managing campus events.
 */
public class Organizer {
    private String username;          // the login account this organizer belongs to (may be null)
    private Organization organization;
    private String position;

    public Organizer(Organization organization, String position) {
        this.organization = requireOrganization(organization);
        this.position = require(position, "Position");
    }

    /** Organizer linked to a login account, so it can be saved and found again. */
    public Organizer(String username, Organization organization, String position) {
        this(organization, position);
        this.username = require(username, "Username");
    }

    public String getUsername() {
        return username;
    }

    private static String require(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return v.trim();
    }

    private static Organization requireOrganization(Organization org) {
        if (org == null) {
            throw new IllegalArgumentException("Organization is required.");
        }
        return org;
    }

    // Getters and Setters
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = requireOrganization(organization);
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = require(position, "Position");
    }

    // Helper methods to create specific event instances
    public Workshop createWorkshop(String eventName, LocalDate date, LocalTime startTime, 
                                   Venue venue, String topic, String materials, String speakerName) {
        return new Workshop(eventName, date, startTime, venue, topic, materials, speakerName);
    }

    public Seminar createSeminar(String eventName, LocalDate date, LocalTime startTime, 
                                 Venue venue, String speakerName, String subject) {
        return new Seminar(eventName, date, startTime, venue, speakerName, subject);
    }

    @Override
    public String toString() {
        return "Organizer (" + position + " at " + organization.getOrgName() + ")";
    }
}
