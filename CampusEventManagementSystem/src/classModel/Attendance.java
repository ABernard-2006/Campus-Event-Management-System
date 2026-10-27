package classModel;

import CEMS.Main;

/** Records whether a registered participant actually attended. */
public class Attendance {
    private final int attendanceId;
    private final Registration registration;
    private boolean attended;

    /** Creates a NEW attendance record (starts as absent). */
    public Attendance(Registration registration) {
        this(Main.generateAttendanceId(), registration, false);
    }

    /** Rebuilds a saved attendance record. */
    public Attendance(int attendanceId, Registration registration, boolean attended) {
        if (attendanceId <= 0) throw new IllegalArgumentException("Attendance ID must be a positive number.");
        if (registration == null) throw new IllegalArgumentException("Registration is required.");
        this.attendanceId = attendanceId;
        Main.syncAttendanceId(attendanceId);
        this.registration = registration;
        this.attended = attended;
    }

    public int getAttendanceId() { return attendanceId; }
    public Registration getRegistration() { return registration; }
    public boolean isAttended() { return attended; }
    public void markPresent() { attended = true; }
    public void markAbsent() { attended = false; }
}
