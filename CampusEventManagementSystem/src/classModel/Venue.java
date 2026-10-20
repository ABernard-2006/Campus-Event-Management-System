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

public class Venue {
    private int venueId;
    private String venueName;
    private String location;
    private int capacity;

    public Venue(String venueName, String location, int capacity) {
        this.venueId = Main.generateVenueId();
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
