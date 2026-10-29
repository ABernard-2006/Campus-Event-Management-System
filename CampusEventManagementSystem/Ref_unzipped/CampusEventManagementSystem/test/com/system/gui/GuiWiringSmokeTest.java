package com.system.gui;

import classModel.Event;
import classModel.Registration;
import classModel.UserAccount;
import com.system.service.DataStore;
import com.system.service.EventManager;

import java.awt.HeadlessException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Date;
import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * Headless wiring smoke test for the panels refactored in Part 6.1.
 *
 * LIMITS: the JRE used for testing has no windowing toolkit, so no window can be shown and JFrames
 * (LoginFrame, SignInFrame) cannot be built at all. JPanels can. A JOptionPane dialog cannot be shown
 * either: it throws HeadlessException, which this test treats as "the handler reached a dialog".
 * What it proves: each refactored button has exactly ONE listener, and clicking it changes the
 * service/model state exactly once (or not at all when the input must be rejected).
 * What it does NOT prove: appearance, dialog text, focus behaviour, NetBeans Design-view loading.
 */
public class GuiWiringSmokeTest {
    static int passed = 0, failed = 0;
    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("  PASS  " + name); } else { failed++; System.out.println("  FAIL  " + name); }
    }

    @SuppressWarnings("unchecked")
    static <T> T field(Object o, String name) throws Exception {
        Class<?> c = o.getClass();
        while (c != null) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return (T) f.get(o); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    /** Clicks a button; a HeadlessException means a dialog was about to be shown, which is fine here. */
    static boolean click(AbstractButton b) {
        try { b.doClick(0); return false; } catch (HeadlessException ex) { return true; }
    }
    static void text(Object panel, String f, String v) throws Exception { ((JTextField) field(panel, f)).setText(v); }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path dir = Files.createTempDirectory("cems-gui");
        UserAccount a = UserAccount.createNew("admin", "admin@cems.local", "secret1", UserAccount.ROLE_ADMIN);
        UserAccount al = UserAccount.createNew("alice", "alice@school.edu", "secret1", UserAccount.ROLE_PARTICIPANT);
        Files.write(dir.resolve("accounts.txt"), ("# x\n"
                + a.getUsername() + "|" + a.getEmail() + "|" + a.getSalt() + "|" + a.getPasswordHash() + "|" + a.getRole() + "\n"
                + al.getUsername() + "|" + al.getEmail() + "|" + al.getSalt() + "|" + al.getPasswordHash() + "|" + al.getRole() + "\n")
                .getBytes(StandardCharsets.UTF_8));
        EventManager m = new EventManager(new DataStore(dir));
        UserAccount admin = m.authenticate("admin", "secret1", "Administrator");
        UserAccount alice = m.authenticate("alice", "secret1", "Participant");

        // ------------------------------------------------------------ EventPanel
        System.out.println("\n[EventPanel]");
        EventPanel ep = new EventPanel(m, admin);
        for (String b : new String[]{"btnCerateEvent", "btnEventClear", "btnAddSpeaker", "btnRemoveSpeaker"})
            check(b + " has exactly one ActionListener", ((AbstractButton) field(ep, b)).getActionListeners().length == 1);
        check("txtEventSpeaker has exactly one ActionListener", ((JTextField) field(ep, "txtEventSpeaker")).getActionListeners().length == 1);
        DefaultListModel<String> speakers = field(ep, "speakerModel");

        // Add Speaker handler
        text(ep, "txtEventSpeaker", "Dr. Cruz");
        click(field(ep, "btnAddSpeaker"));
        check("Add Speaker adds one speaker and clears the box", speakers.size() == 1
                && ((JTextField) field(ep, "txtEventSpeaker")).getText().isEmpty());
        text(ep, "txtEventSpeaker", "dr. cruz");
        boolean dlg = click(field(ep, "btnAddSpeaker"));
        check("Add Speaker rejects a case-insensitive duplicate (dialog shown, list unchanged)", dlg && speakers.size() == 1);
        text(ep, "txtEventSpeaker", "Ms. Lee");
        ((JTextField) field(ep, "txtEventSpeaker")).postActionEvent();       // Enter key in the box
        check("Enter in the speaker box adds a speaker through the same handler", speakers.size() == 2);

        fillEventForm(ep, "Java Basics", "Room 101", "30");
        check("Create Event shows its result dialog", click(field(ep, "btnCerateEvent")));
        check("Create Event created exactly one event via the service", m.getAllEvents().size() == 1
                && m.getAllEvents().get(0).getSpeakerNames().equals("Dr. Cruz, Ms. Lee")
                && m.getAllEvents().get(0).getDurationMinutes() == 90);
        check("form cleared after a successful create", speakers.isEmpty()
                && ((JTextField) field(ep, "txtEventName")).getText().isEmpty());

        fillEventForm(ep, "Overlap", "Room 101", "30");
        click(field(ep, "btnCerateEvent"));
        check("overlapping event is refused by the service (still one event)", m.getAllEvents().size() == 1);

        fillEventForm(ep, "", "Room 101", "30");
        click(field(ep, "btnCerateEvent"));
        check("empty name stopped at the GUI (no event)", m.getAllEvents().size() == 1);
        fillEventForm(ep, "NoCap", "Room 909", "abc");
        click(field(ep, "btnCerateEvent"));
        check("non-numeric capacity stopped at the GUI (no event, no venue)", m.getAllEvents().size() == 1 && m.findVenueByName("Room 909") == null);
        fillEventForm(ep, "BadDuration", "Room 909", "30");
        text(ep, "txtEventDuration", "9x");
        click(field(ep, "btnCerateEvent"));
        check("malformed duration stopped at the GUI", m.getAllEvents().size() == 1);

        // edit mode: same button saves changes
        Event first = m.getAllEvents().get(0);
        ep.loadForEdit(first);
        text(ep, "txtEventName", "Java Basics v2");
        text(ep, "txtEventDuration", "45");
        click(field(ep, "btnCerateEvent"));
        check("in edit mode the button updates the event (no new event)", m.getAllEvents().size() == 1
                && first.getEventName().equals("Java Basics v2") && first.getDurationMinutes() == 45);
        check("edit mode exited after saving", !ep.isEditing());

        // participant account: panel exists but the service refuses
        EventPanel epAlice = new EventPanel(m, alice);
        fillEventForm(epAlice, "Hacked", "Room 777", "10");
        click(field(epAlice, "btnCerateEvent"));
        check("a Participant pressing Create Event is refused by the service", m.getAllEvents().size() == 1);

        // ------------------------------------------------------------ RegistrationPanel
        System.out.println("\n[RegistrationPanel]");
        Event open = m.getAllEvents().get(0);
        RegistrationPanel rp = new RegistrationPanel(m, admin);
        for (String b : new String[]{"btnRegister", "btnRegClear", "btnSearchReg"})
            check(b + " has exactly one ActionListener", ((AbstractButton) field(rp, b)).getActionListeners().length == 1);
        check("txtSearchReg has exactly one ActionListener", ((JTextField) field(rp, "txtSearchReg")).getActionListeners().length == 1);
        check("Cancel button has exactly one ActionListener", ((AbstractButton) field(rp, "btnCancelReg")).getActionListeners().length == 1);
        JComboBox<String> type = field(rp, "cmbParticipantType");
        type.setSelectedItem("Student");
        text(rp, "txtRegName", "Ana Cruz"); text(rp, "txtRegContact", "ana@school.edu");
        text(rp, "txtRegNum", "2024-001"); text(rp, "txtRegExtra", "BSCS");
        check("Register shows its result dialog", click(field(rp, "btnRegister")));
        check("Register created exactly one registration", m.getAllRegistrations().size() == 1);
        click(field(rp, "btnRegister"));
        check("registering the same person twice is refused (still one)", m.getAllRegistrations().size() == 1);
        text(rp, "txtRegName", "");
        click(field(rp, "btnRegister"));
        check("empty name stopped at the GUI", m.getAllRegistrations().size() == 1);
        JTable table = field(rp, "regTable");
        // In the real GUI refresh() runs right after the success dialog is dismissed; headless, the dialog
        // throws first, so refresh is invoked here to exercise the service-backed table loading.
        rp.refresh();
        check("history table shows the registration (after refresh)", table.getRowCount() == 1);
        text(rp, "txtSearchReg", "zzz");
        click(field(rp, "btnSearchReg"));
        check("Search button filters through the service (0 rows)", table.getRowCount() == 0);
        text(rp, "txtSearchReg", "ana");
        ((JTextField) field(rp, "txtSearchReg")).postActionEvent();
        check("Enter in the search box filters too (1 row)", table.getRowCount() == 1);

        RegistrationPanel rpAlice = new RegistrationPanel(m, alice);
        JComboBox<String> t2 = field(rpAlice, "cmbParticipantType");
        t2.setSelectedItem("Student");
        text(rpAlice, "txtRegName", "Alice Lim"); text(rpAlice, "txtRegNum", "2024-777"); text(rpAlice, "txtRegExtra", "BSIT");
        click(field(rpAlice, "btnRegister"));
        Registration mine = m.getAllRegistrations().get(m.getAllRegistrations().size() - 1);
        check("Participant panel registers the account's own profile", m.getAllRegistrations().size() == 2
                && mine.getParticipant().getContactInfo().equals("alice@school.edu"));
        rpAlice.refresh();
        check("Participant history shows only their own row (after refresh)", ((JTable) field(rpAlice, "regTable")).getRowCount() == 1);

        // ------------------------------------------------------------ AttendancePanel
        System.out.println("\n[AttendancePanel]");
        AttendancePanel ap = new AttendancePanel(m, admin);
        AbstractButton present = field(ap, "btnPresent"), absent = field(ap, "btnAbsent");
        check("Mark Present / Mark Absent have exactly one listener each",
                present.getActionListeners().length == 1 && absent.getActionListeners().length == 1);
        JTable at = field(ap, "table");
        check("attendance table lists the event's registrations", at.getRowCount() == 2);
        at.setRowSelectionInterval(0, 1);
        click(present);
        check("Mark Present marked both selected registrations via the service", m.countAttended(open) == 2);
        at.setRowSelectionInterval(0, 0);
        boolean dialog = click(present);
        check("marking someone already present is reported (dialog) and changes nothing", dialog && m.countAttended(open) == 2);
        at.setRowSelectionInterval(0, 0);
        click(absent);
        check("Mark Absent updates attendance", m.countAttended(open) == 1);

        // ------------------------------------------------------------ EventListPanel
        System.out.println("\n[EventListPanel]");
        EventListPanel lp = new EventListPanel(m, admin, e -> { });
        JTable lt = field(lp, "table");
        check("event list built from the service's chronological search", lt.getRowCount() == 1);

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void fillEventForm(Object ep, String name, String venue, String cap) throws Exception {
        ((JComboBox<?>) field(ep, "cmbEventType")).setSelectedItem("Workshop");
        text(ep, "txtEventName", name); text(ep, "txtEventVenue", venue); text(ep, "txtMaxEventCap", cap);
        text(ep, "txtEventTopic", "Java"); text(ep, "txtEventMaterials", "Laptop"); text(ep, "txtEventDuration", "90");
        DefaultListModel<String> sp = field(ep, "speakerModel");
        if (sp.isEmpty()) { sp.addElement("Dr. Cruz"); sp.addElement("Ms. Lee"); }
        ((javax.swing.JSpinner) field(ep, "SpinEventDate")).setValue(Date.from(LocalDate.now().plusDays(3)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        ((javax.swing.JSpinner) field(ep, "SpinEventType")).setValue(Date.from(LocalDate.now().atTime(9, 0)
                .atZone(java.time.ZoneId.systemDefault()).toInstant()));
    }
}
