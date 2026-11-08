package classModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * An invitation key that lets one person sign up as an Organizer.
 *
 * The secret key itself is NEVER stored: only a SHA-256 hash of it is kept. The readable key
 * exists only at the moment an administrator generates it. This class is immutable: "changing"
 * a key (marking it used or revoked) returns a new OrganizerKey, so screens can never
 * alter a key without going through EventManager.
 */
public final class OrganizerKey {
    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_REVOKED = "REVOKED";

    // 32 characters, with the easily confused I, O, 0 and 1 left out. 16 characters = 80 bits of randomness.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int KEY_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int keyId;
    private final String keyHash;
    private final String label;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final String status;
    private final String usedBy;          // "" until the key is used
    private final LocalDateTime usedAt;   // null until the key is used

    public OrganizerKey(int keyId, String keyHash, String label, String createdBy,
                        LocalDateTime createdAt, LocalDateTime expiresAt,
                        String status, String usedBy, LocalDateTime usedAt) {
        if (keyId <= 0) throw new IllegalArgumentException("Key ID must be positive.");
        if (keyHash == null || keyHash.trim().isEmpty()) throw new IllegalArgumentException("Key hash is required.");
        if (createdBy == null || createdBy.trim().isEmpty()) throw new IllegalArgumentException("Creator is required.");
        if (createdAt == null || expiresAt == null) throw new IllegalArgumentException("Creation and expiry times are required.");
        if (!STATUS_UNUSED.equals(status) && !STATUS_USED.equals(status) && !STATUS_REVOKED.equals(status))
            throw new IllegalArgumentException("Unknown key status: " + status);
        this.keyId = keyId;
        this.keyHash = keyHash.trim();
        this.label = label == null ? "" : label.trim();
        this.createdBy = createdBy.trim();
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.usedBy = usedBy == null ? "" : usedBy.trim();
        this.usedAt = usedAt;
    }

    // ---------------- creating and checking secret keys ----------------

    /** Makes a new random key such as "K7QD-3MWX-9HTA-B2FN" using a cryptographically secure generator. */
    public static String generatePlainKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH + 3);
        for (int i = 0; i < KEY_LENGTH; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Upper-cases the text and drops dashes, spaces and anything else that is not a letter or digit. */
    public static String normalize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
                sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /** The value that is stored on disk instead of the key itself. */
    public static String hashKey(String keyText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(("CEMS-ORGANIZER-KEY:" + normalize(keyText)).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available on this computer.", ex);
        }
    }

    /** True when the text a person typed is this key (dashes, spaces and letter case do not matter). */
    public boolean matches(String typedKey) {
        if (normalize(typedKey).length() != KEY_LENGTH) return false;
        return MessageDigest.isEqual(keyHash.getBytes(StandardCharsets.UTF_8),
                hashKey(typedKey).getBytes(StandardCharsets.UTF_8));
    }

    // ---------------- state ----------------

    public boolean isExpired(LocalDateTime now) { return now.isAfter(expiresAt); }

    /** A key can be used to sign up only while it is unused, not revoked and not expired. */
    public boolean isUsable(LocalDateTime now) { return STATUS_UNUSED.equals(status) && !isExpired(now); }

    /** The word shown to the administrator: Active, Used, Revoked or Expired. */
    public String getDisplayStatus(LocalDateTime now) {
        if (STATUS_USED.equals(status)) return "Used";
        if (STATUS_REVOKED.equals(status)) return "Revoked";
        return isExpired(now) ? "Expired" : "Active";
    }

    public OrganizerKey markUsed(String username, LocalDateTime when) {
        return new OrganizerKey(keyId, keyHash, label, createdBy, createdAt, expiresAt,
                STATUS_USED, username, when);
    }

    public OrganizerKey revoke() {
        return new OrganizerKey(keyId, keyHash, label, createdBy, createdAt, expiresAt,
                STATUS_REVOKED, usedBy, usedAt);
    }

    public int getKeyId() { return keyId; }
    /** Needed only for saving to the data file. It is not the key and cannot be typed in as one. */
    public String getKeyHash() { return keyHash; }
    public String getLabel() { return label; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }
    public String getUsedBy() { return usedBy; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
