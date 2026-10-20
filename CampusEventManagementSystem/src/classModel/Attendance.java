/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;
import CEMS.Main;
/**
 *
 * @author ASUS2
 */
public class Attendance {
    private final int attendanceId;
    private final Registration registration;
    private boolean attended;

    public Attendance(Registration registration) {
        if (registration == null) throw new IllegalArgumentException("Registration is required.");
        this.attendanceId = Main.generateId();
        this.registration = registration;
        this.attended = false;
    }

    public int getAttendanceId() { return attendanceId; }
    public Registration getRegistration() { return registration; }
    public boolean isAttended() { return attended; }
    public void markPresent() { attended = true; }
    public void markAbsent() { attended = false; }
}
