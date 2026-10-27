package classModel;

import CEMS.Main;

public class Venue {
    private final int venueId;
    private final String venueName;
    private final String location;
    private final int capacity;

    /** Creates a NEW venue with an automatically generated ID. */
    public Venue(String venueName, String location, int capacity) {
        this(Main.generateVenueId(), venueName, location, capacity);
    }

    /** Rebuilds a saved venue (keeps its saved ID). */
    public Venue(int venueId, String venueName, String location, int capacity) {
        if (venueId <= 0) throw new IllegalArgumentException("Venue ID must be a positive number.");
        if (venueName == null || venueName.trim().isEmpty()) throw new IllegalArgumentException("Venue name is required.");
        if (location == null || location.trim().isEmpty()) throw new IllegalArgumentException("Venue location is required.");
        if (capacity <= 0) throw new IllegalArgumentException("Venue capacity must be greater than zero.");
        this.venueId = venueId;
        Main.syncVenueId(venueId);
        this.venueName = venueName.trim();
        this.location = location.trim();
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
