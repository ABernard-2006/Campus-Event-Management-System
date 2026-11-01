package com.system.service;

import classModel.OrganizerKey;
import classModel.UserAccount;
import java.time.LocalDateTime;

/**
 * Login and sign-up. Coordinates the objects involved (accounts, and invitation keys through
 * {@link OrganizerKeyService}); the rules that belong to ONE account (role names, field formats, password
 * hashing and matching) are in {@link UserAccount}.
 *
 * Also keeps the "Remember me" convenience (username and role only, never the password).
 *
 * authenticate and registerAccount lock the shared {@link DataRepository}, the same lock the key service uses.
 */
public class AuthenticationService {
    private final DataRepository data;
    private final OrganizerKeyService keys;

    AuthenticationService(DataRepository data, OrganizerKeyService keys) {
        this.data = data;
        this.keys = keys;
    }

    /**
     * First run only: when accounts.txt never existed, creates the built-in administrator. Organizers and
     * participants must sign up, and organizers need an invitation key that this administrator generates.
     * Called once by {@link EventManager} right after loading.
     *
     * @throws StorageException when the new account cannot be saved
     */
    void createDefaultAdministratorIfFirstRun() {
        if (data.isFirstRun() && data.accounts().isEmpty()) {
            data.accounts().add(UserAccount.createNew("admin", "admin@cems.local", "admin123", UserAccount.ROLE_ADMIN));
            data.saveAccounts();
        }
    }

    public boolean hasAccounts() { return !data.accounts().isEmpty(); }
    public boolean usernameExists(String username) { return data.findAccount(username) != null; }
    public boolean emailExists(String email) { return data.findAccountByEmail(email) != null; }

    /**
     * Checks the username-or-email, password and selected role against the saved accounts.
     *
     * @return the matching account, or null when the credentials do not match
     * @throws IllegalArgumentException when an input is empty
     * @throws IllegalStateException    when no accounts exist at all
     */
    public UserAccount authenticate(String identifier, String password, String role) {
        synchronized (data) {
            if (identifier == null || identifier.trim().isEmpty())
                throw new IllegalArgumentException("Enter your username or email.");
            if (password == null || password.isEmpty())
                throw new IllegalArgumentException("Enter your password.");
            if (role == null || role.trim().isEmpty())
                throw new IllegalArgumentException("Select the role you are logging in as.");
            if (data.accounts().isEmpty())
                throw new IllegalStateException("No accounts exist yet. Please register first.");
            for (UserAccount account : data.accounts()) {
                if (account.matches(identifier, password, role)) return account;
            }
            return null;
        }
    }

    /**
     * Public sign-up. Creates a Participant account, or an Organizer account when a valid invitation key is given.
     *
     * Rules enforced here (not only on the screen):
     *  - Administrator accounts can never be created by sign-up.
     *  - An Organizer needs an invitation key that is unused, not revoked and not expired.
     *  - Using a key and creating the account happen together while holding the repository lock,
     *    so the same key cannot be spent by two sign-ups.
     *  - The key is only spent when every other field is valid.
     *
     * @param invitationKey ignored for Participants
     * @throws SecurityException        when the role is Administrator
     * @throws IllegalArgumentException when a field is missing or invalid, or the key is not accepted
     * @throws IllegalStateException    when the username or email is already taken (StorageException if saving fails)
     */
    public UserAccount registerAccount(String username, String email, String password,
                                       String confirmPassword, String role, String invitationKey) {
        synchronized (data) {
            String normalizedRole;
            try {
                normalizedRole = UserAccount.normalizeRole(role);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Select what you are registering as.");
            }
            if (UserAccount.ROLE_ADMIN.equals(normalizedRole))
                throw new SecurityException("Administrator accounts cannot be created through sign up.");

            UserAccount.validateSignUpFields(username, email, password, confirmPassword);
            String u = username.trim();
            String e = email.trim();
            if (data.findAccount(u) != null) throw new IllegalStateException("The username \"" + u + "\" is already taken.");
            if (data.findAccountByEmail(e) != null) throw new IllegalStateException("That email address is already registered.");

            boolean organizer = UserAccount.ROLE_ORGANIZER.equals(normalizedRole);
            LocalDateTime now = LocalDateTime.now();
            OrganizerKey spentKey = organizer ? keys.requireUsableKey(invitationKey, now) : null;

            UserAccount account = UserAccount.createNew(u, e, password, normalizedRole);

            if (organizer) {
                // Spend the key first and save it. If the account cannot be saved afterwards the key is
                // put back; in the worst case a key is lost and the administrator simply issues another.
                keys.markUsed(spentKey, u, now.withNano(0));
            }
            data.accounts().add(account);
            try {
                data.saveAccounts();
            } catch (StorageException ex) {
                data.accounts().remove(account);
                if (organizer) {
                    try {
                        keys.restoreUnused(spentKey);
                    } catch (StorageException restoreFailure) {
                        ex.addSuppressed(restoreFailure);
                    }
                }
                throw ex;
            }
            return account;
        }
    }

    // =====================================================================
    //  REMEMBER ME (username + role only, never the password)
    // =====================================================================
    /** {identifier, role} remembered from the last login, or null. */
    public String[] getRememberedLogin() {
        return LoginPreferences.load(data.directory());
    }

    /** Remembers (remember = true) or forgets the account's username and role. Failures are ignored on purpose. */
    public void rememberLogin(UserAccount account, boolean remember) {
        if (remember && account != null) LoginPreferences.save(data.directory(), account.getUsername(), account.getRole());
        else LoginPreferences.clear(data.directory());
    }
}
