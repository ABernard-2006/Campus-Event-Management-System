package classModel;

import CEMS.Main;

public class Venue {
    private final int venueId;
    private final String venueName;
    private String location;            // optional: "" when nobody entered one
    private final int capacity;

    /** Creates a NEW venue with an automatically generated ID. */
    public Venue(String venueName, String location, int capacity) {
        this(Main.generateVenueId(), venueName, location, capacity);
    }

    /** Rebuilds a saved venue (keeps its saved ID). */
    public Venue(int venueId, String venueName, String location, int capacity) {
        if (venueId <= 0) throw new IllegalArgumentException("Venue ID must be a positive number.");
        if (venueName == null || venueName.trim().isEmpty()) throw new IllegalArgumentException("Venue name is required.");
        if (capacity <= 0) throw new IllegalArgumentException("Venue capacity must be greater than zero.");
        this.venueId = venueId;
        Main.syncVenueId(venueId);
        this.venueName = venueName.trim();
        this.location = location == null ? "" : location.trim();
        this.capacity = capacity;
    }

    public int getVenueId() { return venueId; }
    public String getVenueName() { return venueName; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }

    /** Older saved data used the placeholder "TBA" when no location was known; it counts as "no location". */
    public static boolean isRealLocation(String text) {
        return text != null && !text.trim().isEmpty() && !text.trim().equalsIgnoreCase("TBA");
    }

    /** True when a real location was entered (blank and the old "TBA" placeholder do not count). */
    public boolean hasLocation() { return isRealLocation(location); }

    /** Changes the location. Blank is allowed (it means "no location entered"). */
    public void setLocation(String location) { this.location = location == null ? "" : location.trim(); }

    /** "Room 101 - Science Building", or just "Room 101" when no location is known. Never shows "TBA". */
    public String getDisplayName() { return hasLocation() ? venueName + " - " + location : venueName; }

    public boolean isAvailableFor(int attendeeCount) {
        return attendeeCount <= capacity;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (Cap: " + capacity + ")";
    }
}
