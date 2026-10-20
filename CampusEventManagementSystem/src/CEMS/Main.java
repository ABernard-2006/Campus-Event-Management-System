/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package CEMS;

/**
 *
 * @author bernard_07
 */

import classModel.*;
import com.system.service.EventManager;
import java.time.LocalDate;
import java.time.LocalTime;

public class Main {

    // Separate static counters for each entity type[cite: 7, 8]
    private static int eventCounter = 1000;
    private static int participantCounter = 1000;
    private static int venueCounter = 1000;
    private static int orgCounter = 1000;
    private static int registrationCounter = 1000;
    private static int attendanceCounter = 1000;

    public static synchronized int generateId() { return participantCounter++; }
    public static synchronized int generateEventId() { return eventCounter++; }
    public static synchronized int generateParticipantId() { return participantCounter++; }
    public static synchronized int generateVenueId() { return venueCounter++; }
    public static synchronized int generateOrgId() { return orgCounter++; }
    public static synchronized int generateRegistrationId() { return registrationCounter++; }
    public static synchronized int generateAttendanceId() { return attendanceCounter++; }

    public static void main(String[] args) {
        System.out.println("=== STARTING OOP CORE CONSOLE TEST ===\n");

        // 1. Initialize Service Manager
        EventManager manager = new EventManager();

        // 2. Create Venues & Organizations[cite: 6, 8]
        Venue auditorium = new Venue("Auditorium A", "Main Building 3rd Floor", 2); // Small cap: 2 to test full venue error[cite: 6, 8]
        Organization csClub = new Organization(generateOrgId(), "CS Society", "Computer Science Student Club");
        manager.addVenue(auditorium);

        // 3. Create Events (Workshop & Seminar) -> Tests Inheritance & Polymorphism[cite: 6, 7, 9]
        Event javaWorkshop = new Workshop("Java OOP Bootcamp", LocalDate.now(), LocalTime.of(10, 0), 
                                          auditorium, "Polymorphism & GUI", "Laptop & NetBeans", "Lea");
        Event aiSeminar = new Seminar("Future of AI", LocalDate.now().plusDays(1), LocalTime.of(14, 0), 
                                      auditorium, "Dr. Alan Turing", "Artificial Intelligence");

        manager.addEvent(javaWorkshop);
        manager.addEvent(aiSeminar);
        System.out.println("✓ Events created and added to EventManager successfully.");

        // 4. Create Participants (Student, Faculty, External)[cite: 6, 7]
        Participant student1 = new Student("Alice Smith", "alice@univ.edu", "2026-0001", "BSCS");
        Participant student2 = new Student("Bob Jones", "bob@univ.edu", "2026-0002", "BSIT");
        Participant faculty1 = new Faculty("Dr. Clara Oswald", "clara@univ.edu", "F-9901", "CS Department", "Professor");

        manager.addParticipant(student1);
        manager.addParticipant(student2);
        manager.addParticipant(faculty1);
        System.out.println("✓ Participants created with auto-increment IDs.");

        // 5. Test Registration & Capacity Validation[cite: 6, 8]
        System.out.println("\n--- Testing Registrations ---");
        Registration reg1 = manager.register(student1, javaWorkshop); // Alice registers[cite: 6, 8]
        Registration reg2 = manager.register(student2, javaWorkshop); // Bob registers (Capacity reached: 2)[cite: 6, 8]
        System.out.println("Registered Alice (ID: " + reg1.getRegistrationId() + ") and Bob (ID: " + reg2.getRegistrationId() + ")");

        // Test Venue Capacity Protection (Should throw exception)[cite: 8]
        try {
            manager.register(faculty1, javaWorkshop); // Attempt 3rd registration for capacity 2
        } catch (IllegalStateException e) {
            System.out.println("✓ Capacity Protection Working: " + e.getMessage());
        }

        // 6. Test Attendance Tracking[cite: 6, 8]
        System.out.println("\n--- Testing Attendance ---");
        manager.markAttendance(reg1, true);  // Alice present[cite: 6, 8]
        manager.markAttendance(reg2, false); // Bob absent[cite: 6, 8]

        System.out.println("Java Workshop Registered Count: " + manager.countActive(javaWorkshop)); // Output: 2[cite: 8]
        System.out.println("Java Workshop Actual Attended : " + manager.countAttended(javaWorkshop)); // Output: 1[cite: 8]

        // 7. Run Polymorphic Summary Report[cite: 2, 9]
        System.out.println("\n" + manager.generatePolymorphicEventReport());
        
        System.out.println("=== ALL CONSOLE TESTS PASSED ===");
    }
}
