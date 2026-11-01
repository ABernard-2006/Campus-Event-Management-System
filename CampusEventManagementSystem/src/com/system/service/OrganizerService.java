package com.system.service;

import classModel.Organization;
import classModel.Organizer;
import java.util.Collections;
import java.util.List;

/**
 * Organizations and the organizer profiles that belong to them (organizations.txt and organizers.txt).
 * An organizer profile is linked to a login account by username. Kept apart from the account classes because
 * profiles have their own data, files and uniqueness rules and are not part of login or sign-up.
 */
public class OrganizerService {
    private final DataRepository data;

    OrganizerService(DataRepository data) {
        this.data = data;
    }

    public Organization findOrganizationById(int id) { return data.findOrganizationById(id); }

    public List<Organization> getOrganizations() { return Collections.unmodifiableList(data.organizations()); }

    public Organization addOrganization(String name, String description) {
        for (Organization o : data.organizations())
            if (o.getOrgName().equalsIgnoreCase(name == null ? "" : name.trim()))
                throw new IllegalStateException("An organization named \"" + o.getOrgName() + "\" already exists.");
        Organization org = new Organization(name, description);
        data.organizations().add(org);
        try {
            data.saveOrganizations();
        } catch (StorageException ex) {
            data.organizations().remove(org);
            throw ex;
        }
        return org;
    }

    public Organizer findOrganizer(String username) { return data.findOrganizer(username); }

    public List<Organizer> getOrganizers() { return Collections.unmodifiableList(data.organizers()); }

    /** Saves an organizer (which needs a username) and its organization. */
    public void addOrganizer(Organizer o) {
        if (o == null) throw new IllegalArgumentException("Organizer is required.");
        if (o.getUsername() == null) throw new IllegalArgumentException("An organizer must be linked to a username.");
        if (data.findOrganizer(o.getUsername()) != null)
            throw new IllegalStateException("An organizer for " + o.getUsername() + " already exists.");
        boolean newOrg = !data.organizations().contains(o.getOrganization());
        if (newOrg) data.organizations().add(o.getOrganization());
        data.organizers().add(o);
        try {
            if (newOrg) data.saveOrganizations();
            data.saveOrganizers();
        } catch (StorageException ex) {
            data.organizers().remove(o);
            if (newOrg) data.organizations().remove(o.getOrganization());
            throw ex;
        }
    }
}
