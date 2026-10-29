package classModel;

/** An external participant who is neither a student nor faculty. */
public class Others extends Participant {
    private final String orgAffiliation;

    public Others(String fullName, String contactInfo, String orgAffiliation) {
        super(fullName, contactInfo);
        this.orgAffiliation = requireText(orgAffiliation, "Affiliation");
    }

    /** Rebuilds a saved guest. */
    public Others(int participantId, String fullName, String contactInfo, String orgAffiliation) {
        super(participantId, fullName, contactInfo);
        this.orgAffiliation = requireText(orgAffiliation, "Affiliation");
    }

    public String getOrgAffiliation() { return orgAffiliation; }

    @Override
    public String displayDetails() {
        return "[GUEST] " + getFullName() + " | Affiliation: " + orgAffiliation;
    }
}
