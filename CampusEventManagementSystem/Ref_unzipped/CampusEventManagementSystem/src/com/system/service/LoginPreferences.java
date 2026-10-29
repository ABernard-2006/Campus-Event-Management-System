package com.system.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Remembers the username and role for the "Remember me" checkbox.
 * The password is never saved. Problems here are ignored on purpose: remembering a name
 * is only a convenience and must never stop someone from logging in.
 */
public final class LoginPreferences {
    private static final String FILE_NAME = "login-preferences.properties";

    private LoginPreferences() { }

    /** @return {identifier, role}, or null when nothing is remembered. */
    public static String[] load(Path dataDirectory) {
        Path file = dataDirectory.resolve(FILE_NAME);
        if (!Files.exists(file)) return null;
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
        String id = p.getProperty("identifier", "").trim();
        String role = p.getProperty("role", "").trim();
        return id.isEmpty() ? null : new String[] { id, role };
    }

    public static void save(Path dataDirectory, String identifier, String role) {
        Properties p = new Properties();
        p.setProperty("identifier", identifier == null ? "" : identifier);
        p.setProperty("role", role == null ? "" : role);
        try (Writer out = Files.newBufferedWriter(dataDirectory.resolve(FILE_NAME), StandardCharsets.UTF_8)) {
            p.store(out, "CEMS remembered login (no password is stored)");
        } catch (IOException ex) {
            // ignored: see class comment
        }
    }

    public static void clear(Path dataDirectory) {
        try {
            Files.deleteIfExists(dataDirectory.resolve(FILE_NAME));
        } catch (IOException ex) {
            // ignored: see class comment
        }
    }
}
