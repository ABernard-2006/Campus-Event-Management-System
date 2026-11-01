package classModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** A login account. The password is never stored; only a salted SHA-256 hash is kept. */
public class UserAccount {
    public static final String ROLE_ADMIN = "Administrator";
    public static final String ROLE_ORGANIZER = "Organizer";
    public static final String ROLE_PARTICIPANT = "Participant";

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
