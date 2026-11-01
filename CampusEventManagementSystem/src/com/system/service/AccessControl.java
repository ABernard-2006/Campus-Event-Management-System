package com.system.service;

import classModel.Participant;
import classModel.UserAccount;

/**
 * The single place that decides who may do what. Every service asks this class instead of repeating the rule,
 * and the screens use the {@code can...} methods to show or hide buttons (the service re-checks anyway).
 *
 * Two layers of checking are combined here:
 *  - the account's ROLE (what the account is allowed in principle) comes from {@link UserAccount};
 *  - whether the account is REAL (really in the saved account list, not a forged object) comes from the repository.
 */
public class AccessControl {
    private final DataRepository data;

    AccessControl(DataRepository data) {
        this.data = data;
    }

    /** True for an account object that is in the saved account list. */
    boolean isKnownAccount(UserAccount actor) {
        return actor != null && data.accounts().contains(actor);
    }

    /** True for a real, saved Administrator or Organizer account. */
    public boolean isStaff(UserAccount actor) {
        return isKnownAccount(actor) && actor.isStaff();
    }

    /** True only for an account that is really in the saved account list and has the Administrator role. */
    public boolean isAdministrator(UserAccount actor) {
        return actor != null && actor.isAdministrator() && data.accounts().contains(actor);
    }

    /** Who may create, edit and delete events and record attendance. Same rule the services enforce. */
    public boolean canManageEvents(UserAccount actor) { return isStaff(actor); }

    /** Who may open the reports. Same rule ReportService enforces. */
    public boolean canViewReports(UserAccount actor) { return isStaff(actor); }

    /** Who may generate, list and revoke organizer invitation keys (saved administrators only). */
    public boolean canManageOrganizerKeys(UserAccount actor) { return isAdministrator(actor); }

    /** @throws SecurityException unless the actor is a real Administrator or Organizer */
    void requireStaff(UserAccount actor, String action) {
        if (!isStaff(actor))
            throw new SecurityException("Only an Administrator or Organizer can " + action + ".");
    }

    /** @throws SecurityException unless the actor is a real Administrator */
    void requireAdministrator(UserAccount actor) {
        if (!isAdministrator(actor))
            throw new SecurityException("Only a logged-in administrator can manage organizer invitation keys.");
    }

    /** A Participant may only act on a participant record that carries their own account email. */
    boolean isOwnRecord(UserAccount actor, Participant p) {
        return actor != null && p != null && p.hasContact(actor.getEmail());
    }
}
