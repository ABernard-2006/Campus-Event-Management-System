/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.system.model;

/**
 *
 * @author bernard_07
 */

public class Venue {
    private int venueId;
    private String venueName;
    private String location;
    private int capacity;

    public Venue(int venueId, String venueName, String location, int capacity) {
        this.venueId = venueId;
        this.venueName = venueName;
        this.location = location;
        this.capacity = capacity;
    }

    public int getVenueId() { return venueId; }
    public String getVenueName() { return venueName; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }

    public boolean isAvailableFor(int attendeeCount) {
        return attendeeCount <= capacity;
    }

    @Override
    public String toString() {
        return venueName + " (" + location + " - Cap: " + capacity + ")";
    }
}
