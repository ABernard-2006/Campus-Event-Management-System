package classModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/** A login account. The password is never stored; only a salted SHA-256 hash is kept. */
public class UserAccount {
    public static final String ROLE_ADMIN = "Administrator";
    public static final String ROLE_ORGANIZER = "Organizer";
    public static final String ROLE_PARTICIPANT = "Participant";

    /** Shortest password a new account may use. */
    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");

    private final String username;
    private final String email;
    private final String salt;
    private final String passwordHash;
    private final String role;

    /** Rebuilds an account from saved values (the password is already hashed). */
    public UserAccount(String username, String email, String salt, String passwordHash, String role) {
        this.username = requireText(username, "Username");
        this.email = requireText(email, "Email");
        this.salt = requireText(salt, "Salt");
        this.passwordHash = requireText(passwordHash, "Password hash");
        this.role = normalizeRole(role);
    }

    /** Creates a brand-new account from a plain-text password. */
    public static UserAccount createNew(String username, String email, String plainPassword, String role) {
        if (plainPassword == null || plainPassword.isEmpty()) throw new IllegalArgumentException("Password is required.");
        String salt = newSalt();
        return new UserAccount(username, email, salt, hash(salt, plainPassword), role);
    }

    private static String requireText(String v, String field) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        return v.trim();
    }

    /** Accepts "Administrator", "Admin", "Organizer", "Participant(s)" in any letter case. */
    public static String normalizeRole(String role) {
        String r = requireText(role, "Role");
        if (r.equalsIgnoreCase("Administrator") || r.equalsIgnoreCase("Admin")) return ROLE_ADMIN;
        if (r.equalsIgnoreCase("Organizer")) return ROLE_ORGANIZER;
        if (r.equalsIgnoreCase("Participant") || r.equalsIgnoreCase("Participants")) return ROLE_PARTICIPANT;
        throw new IllegalArgumentException("Unknown role: " + r);
    }

    private static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available on this computer.", ex);
        }
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }

    // ---- what this account's role allows in principle. Whether the account is a REAL, saved one is
    //      decided by AccessControl, which is the only place that combines the two checks.
    public boolean isAdministrator() { return ROLE_ADMIN.equals(role); }
    public boolean isOrganizer() { return ROLE_ORGANIZER.equals(role); }
    /** Administrators and Organizers run events; Participants only register for them. */
    public boolean isStaff() { return isAdministrator() || isOrganizer(); }

    /**
     * Checks the text fields of a NEW account and throws IllegalArgumentException with the message shown on the
     * sign-up screen. Rules are checked in this order: username, email, password, password confirmation.
     * Accounts loaded from the data file are not re-checked (older files may hold older usernames).
     *
     * @param confirmPassword compared with the password only when it is not null
     */
    public static void validateSignUpFields(String username, String email, String password, String confirmPassword) {
        String u = username == null ? "" : username.trim();
        String e = email == null ? "" : email.trim();
        if (u.isEmpty()) throw new IllegalArgumentException("Username is required.");
        if (!USERNAME_PATTERN.matcher(u).matches())
            throw new IllegalArgumentException("Username must be 3-30 characters: letters, numbers, dot, dash or underscore.");
        if (e.isEmpty()) throw new IllegalArgumentException("Email address is required.");
        if (!EMAIL_PATTERN.matcher(e).matches())
            throw new IllegalArgumentException("Enter a valid email address, for example name@school.edu.");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Password is required.");
        if (password.length() < MIN_PASSWORD_LENGTH)
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        if (confirmPassword != null && !password.equals(confirmPassword))
            throw new IllegalArgumentException("Passwords do not match.");
    }

    /** True when the identifier is this account's username or email, the password is right and the role matches. */
    public boolean matches(String identifier, String plainPassword, String selectedRole) {
        if (identifier == null || plainPassword == null || selectedRole == null) return false;
        String id = identifier.trim();
        if (!id.equalsIgnoreCase(username) && !id.equalsIgnoreCase(email)) return false;
        try {
            if (!role.equals(normalizeRole(selectedRole))) return false;
        } catch (IllegalArgumentException ex) {
            return false;
        }
        byte[] expected = passwordHash.getBytes(StandardCharsets.UTF_8);
        byte[] actual = hash(salt, plainPassword).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
