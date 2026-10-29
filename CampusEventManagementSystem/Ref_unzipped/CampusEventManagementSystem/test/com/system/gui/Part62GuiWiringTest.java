package com.system.gui;

import classModel.Event;
import classModel.UserAccount;
import com.system.service.DataStore;
import com.system.service.EventManager;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

/**
 * Headless wiring test for the panels changed in Part 6.2: ReportPanel, EventListPanel, AttendancePanel.
 * LIMITS are the same as GuiWiringSmokeTest: no window can be shown, JFrames/JDialogs (Dashboard, LoginFrame,
 * SignInFrame, the two key dialogs) cannot be constructed, and JOptionPane dialogs throw HeadlessException,
 * which is treated as "the handler reached a dialog".
 */
public class Part62GuiWiringTest {
    static int passed = 0, failed = 0;
    static void check(String n, boolean ok) { if (ok) { passed++; System.out.println("  PASS  " + n); } else { failed++; System.out.println("  FAIL  " + n); } }
    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String name) throws Exception {
        Class<?> c = o.getClass();
        while (c != null) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return (T) f.get(o); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    static boolean click(AbstractButton b) {
        try { b.doClick(0); return false; } catch (java.awt.HeadlessException ex) { return true; }
    }
    /** Listeners on a spinner that are ours (the DateEditor registers itself too; it is not counted). */
    static int ourChangeListeners(JSpinner s) {
        int n = 0;
        for (ChangeListener l : s.getChangeListeners()) if (!(l instanceof JSpinner.DefaultEditor)) n++;
        return n;
    }
    static Date day(LocalDate d) { return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()); }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path dir = Files.createTempDirectory("cems-gui62");
        UserAccount a = UserAccount.createNew("admin", "admin@cems.local", "secret1", UserAccount.ROLE_ADMIN);
        Files.write(dir.resolve("accounts.txt"), ("# x\n" + a.getUsername() + "|" + a.getEmail() + "|" + a.getSalt() + "|"
                + a.getPasswordHash() + "|" + a.getRole() + "\n").getBytes(StandardCharsets.UTF_8));
        EventManager m = new EventManager(new DataStore(dir));
        UserAccount admin = m.authenticate("admin", "secret1", "Administrator");
        LocalDate today = LocalDate.now();
        Event w = m.createEvent(admin, "Workshop", "Java Basics", today.plusDays(2), LocalTime.of(9, 0), 60, "R1", 20, "Dr. Cruz", "Java", "Laptop");
        Event s = m.createEvent(admin, "Seminar", "AI Ethics", today.plusDays(9), LocalTime.of(9, 0), 60, "R2", 20, "Prof. Tan", "Ethics", "");
        Event w2 = m.createEvent(admin, "Workshop", "Python", today.plusDays(5), LocalTime.of(13, 0), 60, "R1", 20, "Ms. Lee", "Python", "Laptop");
        var r1 = m.registerForEvent(admin, w, "Student", "Sam", "sam@x.edu", "2024-1", "BSCS", "");
        m.registerForEvent(admin, s, "Others", "Otto", "otto@x.edu", "", "ACME", "");
        m.markAttendance(admin, r1, true);

        // ------------------------------------------------------------ ReportPanel
        System.out.println("\n[ReportPanel]");
        ReportPanel rp = new ReportPanel(m, admin);
        check("Generate button has exactly one ActionListener", ((AbstractButton) field(rp, "btnGanerateReport")).getActionListeners().length == 1);
        check("Select Event combo has exactly one ActionListener", ((JComboBox<?>) field(rp, "cmbSelectEvents")).getActionListeners().length == 1);
        check("Report Type combo has exactly one ActionListener", ((JComboBox<?>) field(rp, "cmbReportType")).getActionListeners().length == 1);
        check("each date spinner has exactly one handler of ours", ourChangeListeners(field(rp, "spinMinDateRange")) == 1 && ourChangeListeners(field(rp, "spinMaxDateRange")) == 1);

        JTable table = field(rp, "reportTable");
        JLabel reg = field(rp, "lblTotalRegNum"), att = field(rp, "lblTotalAttendNum"), rate = field(rp, "lblTotalAttendRateNum");
        check("table lists all 3 events, soonest first", table.getRowCount() == 3 && "Java Basics".equals(table.getModel().getValueAt(0, 0))
                && "Python".equals(table.getModel().getValueAt(1, 0)) && "AI Ethics".equals(table.getModel().getValueAt(2, 0)));
        check("tiles come from the service totals: 2 registered, 1 attended, 50.0%", reg.getText().equals("2") && att.getText().equals("1") && rate.getText().equals("50.0%"));
        JSpinner min = field(rp, "spinMinDateRange"), max = field(rp, "spinMaxDateRange");
        check("default date range = earliest..latest event date", min.getValue().equals(day(today.plusDays(2))) && max.getValue().equals(day(today.plusDays(9))));

        JComboBox<String> type = field(rp, "cmbReportType");
        type.setSelectedItem("Seminar");
        check("choosing Seminar filters through the service (1 row)", table.getRowCount() == 1 && "AI Ethics".equals(table.getModel().getValueAt(0, 0)));
        check("tiles follow the filter: 1 registered, 0 attended, 0.0%", reg.getText().equals("1") && att.getText().equals("0") && rate.getText().equals("0.0%"));
        type.setSelectedItem("All Types");
        JComboBox<String> which = field(rp, "cmbSelectEvents");
        which.setSelectedIndex(2);       // item 0 = all, 1 = Java Basics, 2 = Python
        check("choosing one event shows only it", table.getRowCount() == 1 && "Python".equals(table.getModel().getValueAt(0, 0)));
        which.setSelectedIndex(0);
        max.setValue(day(today.plusDays(5)));
        check("changing the end date filters by date range (2 rows)", table.getRowCount() == 2);
        // a later data change must not reset a range the user picked
        m.createEvent(admin, "Seminar", "Late Seminar", today.plusDays(30), LocalTime.of(9, 0), 60, "R3", 5, "Prof. X", "Late", "");
        rp.refresh();
        check("after the user picked dates, refresh keeps their range (still 2 rows)", table.getRowCount() == 2);
        boolean refused = false;
        try { min.setValue(day(today.plusDays(8))); } catch (java.awt.HeadlessException ex) { refused = true; }   // start after end
        check("start date after end date: the service refuses it and the panel reaches its warning dialog", refused);
        min.setValue(day(today.plusDays(2)));
        check("Generate click re-runs the report (still 2 rows)", !click(field(rp, "btnGanerateReport")) && table.getRowCount() == 2);

        // ------------------------------------------------------------ EventListPanel
        System.out.println("\n[EventListPanel]");
        final Event[][] edited = {{null}};
        EventListPanel lp = new EventListPanel(m, admin, e -> edited[0][0] = e);
        for (String b : new String[]{"btnSearch", "btnShowAll", "btnEdit", "btnDelete"})
            check(b + " has exactly one ActionListener", ((AbstractButton) field(lp, b)).getActionListeners().length == 1);
        JTextField search = field(lp, "txtSearch");
        check("search box has exactly one ActionListener", search.getActionListeners().length == 1);
        JTable lt = field(lp, "table");
        check("list shows all 4 events", lt.getRowCount() == 4);
        search.setText("python");
        click(field(lp, "btnSearch"));
        check("Search button filters through the service", lt.getRowCount() == 1);
        search.setText("workshop");
        search.postActionEvent();
        check("Enter in the search box filters the same way", lt.getRowCount() == 2);
        click(field(lp, "btnShowAll"));
        check("Show All clears the box and lists everything", search.getText().isEmpty() && lt.getRowCount() == 4);

        click(field(lp, "btnEdit"));
        check("Edit with no row selected does not call the edit handler", edited[0][0] == null);
        lt.setRowSelectionInterval(0, 0);
        click(field(lp, "btnEdit"));
        check("Edit with a row selected passes that event to the edit handler", edited[0][0] == w);
        int before = m.getAllEvents().size();
        boolean dialog = click(field(lp, "btnDelete"));
        check("Delete asks for confirmation first (dialog reached) and deletes nothing yet", dialog && m.getAllEvents().size() == before);
        lt.clearSelection();
        click(field(lp, "btnDelete"));
        check("Delete with no row selected deletes nothing", m.getAllEvents().size() == before);

        // ------------------------------------------------------------ AttendancePanel
        System.out.println("\n[AttendancePanel]");
        AttendancePanel ap = new AttendancePanel(m, admin);
        JComboBox<String> cmb = field(ap, "cmbEvent");
        check("event combo has exactly one ActionListener", cmb.getActionListeners().length == 1);
        JTable at = field(ap, "table");
        cmb.setSelectedIndex(0);
        int row0 = at.getRowCount();
        cmb.setSelectedIndex(1);
        check("changing the event reloads the registrations (Java Basics has 1, Python 0)", row0 == 1 && at.getRowCount() == 0);

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }
}
