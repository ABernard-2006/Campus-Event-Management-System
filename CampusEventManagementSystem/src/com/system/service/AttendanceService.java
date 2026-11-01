package com.system.service;

import classModel.Attendance;
import classModel.Event;
import classModel.Registration;
import classModel.UserAccount;
import java.util.Collections;
import java.util.List;

/**
 * Who actually attended: marking a registered participant present or absent (staff only) and reading the result.
 * Cancelling a registration resets its attendance as part of the cancellation itself, in
 * {@link RegistrationService#cancelRegistration}, because that is a change to the registration.
 */
public class AttendanceService {
    private final DataRepository data;
    private final AccessControl access;
    private final ChangeNotifier changes;

    AttendanceService(DataRepository data, AccessControl access, ChangeNotifier changes) {
        this.data = data;
        this.access = access;
        this.changes = changes;
    }

    public Attendance getAttendance(Registration r) { return data.getAttendance(r); }

    public List<Attendance> getAllAttendances() {
        return Collections.unmodifiableList(data.attendances());
    }

    /**
     * Marks a registered participant present or absent. Staff only.
     * A registration can only ever have ONE attendance record: marking again changes that record,
     * and asking for the status it already has is rejected instead of silently repeated.
     */
    public void markAttendance(UserAccount actor, Registration r, boolean present) {
        access.requireStaff(actor, "mark attendance");
        if (r == null || !data.registrations().contains(r)) throw new IllegalArgumentException("Select a registration first.");
        if (!r.isActive()) throw new IllegalStateException("Cancelled registrations cannot be marked.");
        Attendance a = data.getAttendance(r);
        boolean created = (a == null);
        boolean wasAttended = !created && a.isAttended();
        if (!created && present && wasAttended)
            throw new IllegalStateException(r.getParticipant().getFullName() + " is already marked present.");
        if (!created && !present && !wasAttended)
            throw new IllegalStateException(r.getParticipant().getFullName() + " is already marked absent.");
        if (created) {
            a = new Attendance(r);
            data.attendances().add(a);
        }
        if (present) a.markPresent(); else a.markAbsent();
        try {
            data.saveAttendance();
        } catch (StorageException ex) {
            if (created) {
                data.attendances().remove(a);
            } else if (wasAttended) {
                a.markPresent();
            } else {
                a.markAbsent();
            }
            throw ex;
        }
        changes.fireChanged();
    }

    /** Participants of this event who were marked present AND still hold a valid (not cancelled) registration. */
    public int countAttended(Event e) {
        int n = 0;
        for (Attendance a : data.attendances()) {
            if (a.getRegistration().getEvent() == e && a.isAttended() && a.getRegistration().isActive()) n++;
        }
        return n;
    }
}
