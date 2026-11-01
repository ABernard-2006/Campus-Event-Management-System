package com.system.service;

import classModel.OrganizerKey;
import classModel.UserAccount;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Organizer invitation keys: an administrator generates, lists and revokes them, and the sign-up workflow
 * ({@link AuthenticationService}) checks and spends them through the package-level methods at the bottom.
 *
 * Only a hash of each key is stored; the readable key exists once, in the return value of
 * {@link #generateOrganizerKey}. All key rules (status, expiry, who may manage keys) live in this class.
 *
 * Methods that read or change keys lock the shared {@link DataRepository}, the same lock the sign-up workflow
 * uses, so a key can never be revoked and spent at the same moment.
 */
public class OrganizerKeyService {
    private final DataRepository data;
    private final AccessControl access;

    OrganizerKeyService(DataRepository data, AccessControl access) {
        this.data = data;
        this.access = access;
    }

    private int findKeyIndexByText(String typedKey) {
        int found = -1;
        List<OrganizerKey> keys = data.organizerKeys();
        for (int i = 0; i < keys.size(); i++)      // no early exit, so timing does not reveal a match
            if (keys.get(i).matches(typedKey)) found = i;
        return found;
    }

    private int nextKeyId() {
        int max = 0;
        for (OrganizerKey k : data.organizerKeys()) max = Math.max(max, k.getKeyId());
        return max + 1;
    }

    /**
     * Creates a new invitation key and returns its readable text. This is the ONLY time the readable key
     * exists: only a hash is stored, so it can never be shown again.
     *
     * @param label     a note such as the intended person's name (optional, up to 60 characters)
     * @param validDays how many days the key can be used, from 1 to 30
     * @throws SecurityException when 'actor' is not a logged-in administrator
     */
    public String generateOrganizerKey(UserAccount actor, String label, int validDays) {
        synchronized (data) {
            access.requireAdministrator(actor);
            if (validDays < 1 || validDays > 30)
                throw new IllegalArgumentException("A key must be valid for between 1 and 30 days.");
            String note = label == null ? "" : label.trim();
            if (note.length() > 60) throw new IllegalArgumentException("The label can be at most 60 characters.");

            List<OrganizerKey> keys = data.organizerKeys();
            String plain;
            String hash;
            do {
                plain = OrganizerKey.generatePlainKey();
                hash = OrganizerKey.hashKey(plain);
                final String candidate = hash;
                if (keys.stream().noneMatch(k -> k.getKeyHash().equals(candidate))) break;
            } while (true);

            LocalDateTime now = LocalDateTime.now().withNano(0);
            OrganizerKey key = new OrganizerKey(nextKeyId(), hash, note, actor.getUsername(),
                    now, now.plusDays(validDays), OrganizerKey.STATUS_UNUSED, "", null);
            keys.add(key);
            try {
                data.saveOrganizerKeys();
            } catch (StorageException ex) {
                keys.remove(key);
                throw ex;
            }
            return plain;
        }
    }

    /** A read-only copy of every key (newest first). The copies hold no readable key text. */
    public List<OrganizerKey> getOrganizerKeys(UserAccount actor) {
        synchronized (data) {
            access.requireAdministrator(actor);
            List<OrganizerKey> copy = new ArrayList<>(data.organizerKeys());
            Collections.reverse(copy);
            return Collections.unmodifiableList(copy);
        }
    }

    /** Cancels a key that has not been used yet. A lost key is handled by revoking it and generating a new one. */
    public void revokeOrganizerKey(UserAccount actor, int keyId) {
        synchronized (data) {
            access.requireAdministrator(actor);
            int index = data.findKeyIndex(keyId);
            if (index < 0) throw new IllegalArgumentException("That key no longer exists.");
            OrganizerKey key = data.organizerKeys().get(index);
            if (OrganizerKey.STATUS_USED.equals(key.getStatus()))
                throw new IllegalStateException("That key has already been used and cannot be revoked.");
            if (OrganizerKey.STATUS_REVOKED.equals(key.getStatus()))
                throw new IllegalStateException("That key is already revoked.");
            data.organizerKeys().set(index, key.revoke());
            try {
                data.saveOrganizerKeys();
            } catch (StorageException ex) {
                data.organizerKeys().set(index, key);
                throw ex;
            }
        }
    }

    // ---------------------------------------------------------------------
    //  Used by AuthenticationService.registerAccount (it holds the repository lock while calling these)
    // ---------------------------------------------------------------------

    /**
     * Finds the key the person typed and checks that it can still be used to sign up.
     *
     * @return the stored key (still unused)
     * @throws IllegalArgumentException with the message shown on the sign-up screen when the key is missing,
     *                                  unknown, already used, revoked or expired
     */
    OrganizerKey requireUsableKey(String typedKey, LocalDateTime now) {
        if (typedKey == null || OrganizerKey.normalize(typedKey).isEmpty())
            throw new IllegalArgumentException("An invitation key is required to register as an Organizer.");
        int keyIndex = findKeyIndexByText(typedKey);
        if (keyIndex < 0)
            throw new IllegalArgumentException("That invitation key is not valid. Check it and try again.");
        OrganizerKey key = data.organizerKeys().get(keyIndex);
        if (OrganizerKey.STATUS_USED.equals(key.getStatus()))
            throw new IllegalArgumentException("That invitation key has already been used.");
        if (OrganizerKey.STATUS_REVOKED.equals(key.getStatus()))
            throw new IllegalArgumentException("That invitation key has been revoked. Ask an administrator for a new one.");
        if (key.isExpired(now))
            throw new IllegalArgumentException("That invitation key has expired. Ask an administrator for a new one.");
        return key;
    }

    /** Marks the key as spent by 'username' and saves. If saving fails the key is put back and the error is rethrown. */
    void markUsed(OrganizerKey key, String username, LocalDateTime when) {
        int index = data.findKeyIndex(key.getKeyId());
        data.organizerKeys().set(index, key.markUsed(username, when));
        try {
            data.saveOrganizerKeys();
        } catch (StorageException ex) {
            data.organizerKeys().set(index, key);
            throw ex;
        }
    }

    /**
     * Puts a spent key back to its earlier state because the account could not be saved. The key is restored in
     * memory first; a failure to save that restoration is thrown to the caller (which reports it as suppressed).
     */
    void restoreUnused(OrganizerKey original) {
        data.organizerKeys().set(data.findKeyIndex(original.getKeyId()), original);
        data.saveOrganizerKeys();
    }
}
