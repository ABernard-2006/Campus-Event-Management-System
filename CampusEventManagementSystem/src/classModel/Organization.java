package classModel;

import CEMS.Main;

public class Organization {
    private final int orgId;
    private final String orgName;
    private String description;

    /** Creates a NEW organization with an automatically generated ID. */
    public Organization(String orgName, String description) {
        this(Main.generateOrgId(), orgName, description);
    }

    /** Rebuilds a saved organization. */
    public Organization(int orgId, String orgName, String description) {
        if (orgId <= 0) throw new IllegalArgumentException("Organization ID must be a positive number.");
        if (orgName == null || orgName.trim().isEmpty()) throw new IllegalArgumentException("Organization name is required.");
        this.orgId = orgId;
        Main.syncOrgId(orgId);
        this.orgName = orgName.trim();
        this.description = description == null ? "" : description.trim();
    }

    public int getOrgId() { return orgId; }
    public String getOrgName() { return orgName; }
    public String getOrganizationName() { return orgName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description.trim(); }
}
