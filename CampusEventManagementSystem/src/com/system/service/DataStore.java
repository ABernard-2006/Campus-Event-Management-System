package com.system.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-level text-file storage. It knows nothing about events or participants:
 * it only reads and writes lines whose fields are separated by '|'.
 *
 * Special characters inside a field are escaped: '\' becomes "\\", '|' becomes "\p",
 * a line break becomes "\n" (and "\r"), so one record is always exactly one line.
 */
public class DataStore {
    public static final String ACCOUNTS = "accounts.txt";
    public static final String ORGANIZER_KEYS = "organizer_keys.txt";
    public static final String ORGANIZATIONS = "organizations.txt";
    public static final String ORGANIZERS = "organizers.txt";
    public static final String VENUES = "venues.txt";
    public static final String EVENTS = "events.txt";
    public static final String PARTICIPANTS = "participants.txt";
    public static final String REGISTRATIONS = "registrations.txt";
    public static final String ATTENDANCE = "attendance.txt";

    private final Path directory;

    /** Uses a "data" folder in the folder the program is started from. */
    public DataStore() {
        this(Paths.get("data"));
    }

    public DataStore(Path directory) {
        if (directory == null) throw new IllegalArgumentException("A data folder is required.");
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new StorageException("Could not create the data folder " + directory.toAbsolutePath()
                    + ": " + ex.getMessage(), ex);
        }
    }

    public Path getDirectory() { return directory; }

    public boolean exists(String fileName) {
        return Files.exists(directory.resolve(fileName));
    }

    // ---------------- one parsed line ----------------
    public static class Record {
        private final int lineNumber;
        private final String[] fields;

        Record(int lineNumber, String[] fields) {
            this.lineNumber = lineNumber;
            this.fields = fields;
        }

        public int getLineNumber() { return lineNumber; }
        public int size() { return fields.length; }
        public String get(int index) { return fields[index]; }

        /** Rejects lines that have the wrong number of fields. */
        public void requireFields(int count) {
            if (fields.length != count)
                throw new IllegalArgumentException("expected " + count + " fields but found " + fields.length);
        }
    }

    // ---------------- escaping ----------------
    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '|':  sb.append("\\p"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= s.length()) throw new IllegalArgumentException("bad escape sequence at end of field");
            char n = s.charAt(++i);
            switch (n) {
                case '\\': sb.append('\\'); break;
                case 'p':  sb.append('|'); break;
                case 'n':  sb.append('\n'); break;
                case 'r':  sb.append('\r'); break;
                default: throw new IllegalArgumentException("bad escape sequence \\" + n);
            }
        }
        return sb.toString();
    }

    /** Joins fields into one escaped line. Numbers, dates and times are converted with String.valueOf. */
    public static String join(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(escape(String.valueOf(fields[i])));
        }
        return sb.toString();
    }

    // ---------------- reading ----------------
    /**
     * Reads every record of a file. A missing file is simply "no records yet".
     * Blank lines and lines starting with '#' are ignored. Lines that cannot be parsed
     * are skipped and described in 'problems' instead of stopping the whole load.
     */
    public List<Record> readRecords(String fileName, List<String> problems) {
        List<Record> records = new ArrayList<>();
        Path file = directory.resolve(fileName);
        if (!Files.exists(file)) return records;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] raw = line.split("\\|", -1);
                try {
                    String[] fields = new String[raw.length];
                    for (int i = 0; i < raw.length; i++) fields[i] = unescape(raw[i]);
                    records.add(new Record(number, fields));
                } catch (IllegalArgumentException ex) {
                    problems.add("line " + number + " skipped - " + ex.getMessage());
                }
            }
        } catch (CharacterCodingException ex) {
            throw new StorageException(fileName + " is not valid UTF-8 text and could not be read.", ex);
        } catch (IOException ex) {
            throw new StorageException("Could not read " + fileName + " in " + directory.toAbsolutePath()
                    + ": " + ex.getMessage(), ex);
        }
        return records;
    }

    // ---------------- writing ----------------
    /**
     * Replaces the whole file with the given lines. The new content is written to a temporary
     * file first and then moved over the real file, so a failed save cannot leave a half-written file.
     */
    public void writeRecords(String fileName, String header, List<String> lines) {
        Path target = directory.resolve(fileName);
        Path temp = directory.resolve(fileName + ".tmp");
        try {
            Files.createDirectories(directory);
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write("# " + header);
                writer.newLine();
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(temp);          // clean up; the original error is reported below
            } catch (IOException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw new StorageException("Could not save " + fileName + " in " + directory.toAbsolutePath()
                    + ": " + ex.getMessage(), ex);
        }
    }

    /** Copies a file to "name.yyyyMMdd-HHmmss.bak" so skipped records are never lost when the file is rewritten. */
    public void backupFile(String fileName) {
        Path source = directory.resolve(fileName);
        if (!Files.exists(source)) return;
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = directory.resolve(fileName + "." + stamp + ".bak");
        try {
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new StorageException("Could not back up " + fileName + ": " + ex.getMessage(), ex);
        }
    }
}
