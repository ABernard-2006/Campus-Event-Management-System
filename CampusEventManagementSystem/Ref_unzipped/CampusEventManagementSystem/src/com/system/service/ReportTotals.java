package com.system.service;

/** The three summary numbers shown above the report table. Immutable. */
public final class ReportTotals {
    private final int totalRegistrations;
    private final int totalAttended;
    private final double attendanceRatePercent;

    public ReportTotals(int totalRegistrations, int totalAttended, double attendanceRatePercent) {
        this.totalRegistrations = totalRegistrations;
        this.totalAttended = totalAttended;
        this.attendanceRatePercent = attendanceRatePercent;
    }

    public int getTotalRegistrations() { return totalRegistrations; }
    public int getTotalAttended() { return totalAttended; }
    public double getAttendanceRatePercent() { return attendanceRatePercent; }
}
