/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package CEMS;

/**
 *
 * @author bernard_07
 */


import com.system.gui.Dashboard;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    // Static counter variables starting at 1000 for each entity type[cite: 7, 8]
    private static int eventCounter = 0;
    private static int participantCounter = 0;
    private static int venueCounter = 0;
    private static int orgCounter = 0;
    private static int registrationCounter = 0;
    private static int attendanceCounter = 0;

    // Default fallback generator matching Week 2 table[cite: 8]
    public static synchronized int generateId() {
        return participantCounter++;
    }

    // Entity-specific ID generators[cite: 7, 8]
    public static synchronized int generateEventId() {
        return eventCounter++;
    }

    public static synchronized int generateParticipantId() {
        return participantCounter++;
    }

    public static synchronized int generateVenueId() {
        return venueCounter++;
    }

    public static synchronized int generateOrgId() {
        return orgCounter++;
    }

    public static synchronized int generateRegistrationId() {
        return registrationCounter++;
    }

    public static synchronized int generateAttendanceId() {
        return attendanceCounter++;
    }

    // Main entry point to launch the NetBeans Swing Application
    public static void main(String[] args) {
        try 
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        
    SwingUtilities.invokeLater(() -> {
        // 1. Create the shared service manager
        com.system.service.EventManager manager = new com.system.service.EventManager();
        
        // 2. Launch LoginFrame first
        com.system.gui.LoginFrame loginFrame = new com.system.gui.LoginFrame(manager);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    });
    }
}
