package CEMS;

import com.system.gui.LoginFrame;
import com.system.service.EventManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Application entry point and ID generator for every entity type. */
public class Main {

    // Every counter starts at 1. Loading saved records moves the counter past the highest saved ID.
    private static int eventCounter = 1;
    private static int participantCounter = 1;
    private static int venueCounter = 1;
    private static int orgCounter = 1;
    private static int registrationCounter = 1;
    private static int attendanceCounter = 1;

    public static synchronized int generateEventId() { return eventCounter++; }
    public static synchronized int generateParticipantId() { return participantCounter++; }
    public static synchronized int generateVenueId() { return venueCounter++; }
    public static synchronized int generateOrgId() { return orgCounter++; }
    public static synchronized int generateRegistrationId() { return registrationCounter++; }
    public static synchronized int generateAttendanceId() { return attendanceCounter++; }

    // Called by the model constructors so a loaded ID can never be handed out again.
    public static synchronized void syncEventId(int usedId) { eventCounter = Math.max(eventCounter, usedId + 1); }
    public static synchronized void syncParticipantId(int usedId) { participantCounter = Math.max(participantCounter, usedId + 1); }
    public static synchronized void syncVenueId(int usedId) { venueCounter = Math.max(venueCounter, usedId + 1); }
    public static synchronized void syncOrgId(int usedId) { orgCounter = Math.max(orgCounter, usedId + 1); }
    public static synchronized void syncRegistrationId(int usedId) { registrationCounter = Math.max(registrationCounter, usedId + 1); }
    public static synchronized void syncAttendanceId(int usedId) { attendanceCounter = Math.max(attendanceCounter, usedId + 1); }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            e.printStackTrace();   // the default look and feel is still fine
        }

        SwingUtilities.invokeLater(() -> {
            // 1. Create the shared service manager. This loads every saved text file.
            EventManager manager;
            try {
                manager = new EventManager();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null,
                        "The application could not load its saved data:\n" + ex.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!manager.getLoadWarnings().isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "Some saved records could not be read and were skipped.\n"
                        + "A backup copy of each affected file was kept in:\n" + manager.getDataDirectory()
                        + "\n\n" + String.join("\n", manager.getLoadWarnings()),
                        "Data warning", JOptionPane.WARNING_MESSAGE);
            }

            // 2. Launch LoginFrame first
            LoginFrame loginFrame = new LoginFrame(manager);
            loginFrame.setLocationRelativeTo(null);
            loginFrame.setVisible(true);
        });
    }
}
