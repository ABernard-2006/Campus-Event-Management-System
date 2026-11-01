package classModel;

import CEMS.Main;

/** Common information for every kind of participant. */
public abstract class Participant {
    private final int participantId;
    private String fullName;
    private String contactInfo;

    /** Creates a NEW participant with an automatically generated ID. */
    public Participant(String fullName, String contactInfo) {
        this(Main.generateParticipantId(), fullName, contactInfo);
    }

    /** Rebuilds a participant that was saved earlier (keeps its saved ID). */
    protected Participant(int participantId, String fullName, String contactInfo) {
        if (participantId <= 0) throw new IllegalArgumentException("Participant ID must be a positive number.");
        this.participantId = participantId;
        Main.syncParticipantId(participantId);
        this.fullName = requireText(fullName, "Participant name");
        this.contactInfo = requireText(contactInfo, "Contact info");
    }

    /** Shared helper for subclasses: trims the text and rejects empty values. */
    protected static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    public int getParticipantId() { return participantId; }
    public String getFullName() { return fullName; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = requireText(contactInfo, "Contact info"); }

    /** Each subclass describes itself differently (polymorphism). */
    public abstract String displayDetails();
}
