/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.system.gui;

import classModel.Event;
import classModel.UserAccount;
import com.system.service.EventManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
// @author Dhustine

/**
 * Main window shown after a successful login.
 *
 * The NetBeans form (initComponents) is left exactly as the GUI Builder made it. Everything
 * else (navigation, roles, widgets, logout) is handwritten below, outside the generated blocks.
 */
public class Dashboard extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Dashboard.class.getName());

    // Names of the screens (cards) that can be shown in the content area
    private static final String CARD_HOME = "home";
    private static final String CARD_MANAGE = "manage";
    private static final String CARD_REGISTER = "register";
    private static final String CARD_REPORTS = "reports";

    private static final DateTimeFormatter TITLE_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final EventManager manager;     // the one shared service object for the whole app
    private UserAccount account;            // the logged-in user; set to null again on logout

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);
    private final Map<String, JComponent> cards = new HashMap<>();
    private final ProfileHeaderPanel profileHeader = new ProfileHeaderPanel();
    private JButton btnOrganizerKeys;       // only created for administrators

    // What the Home/Events card list is currently showing, so it can be redrawn after any data change
    private Supplier<List<Event>> homeSource = null;
    private String homeEmptyMessage = "";
    // Runs after every saved change (event, registration, attendance) and refreshes every screen
    private final Runnable dataListener = this::onDataChanged;

    // ---------- Constructor ----------
    /**
     * @param manager the shared EventManager (the same one the login screen used)
     * @param account the authenticated account returned by EventManager.authenticate(...)
     */
    public Dashboard(EventManager manager, UserAccount account) {
        if (manager == null) throw new IllegalArgumentException("A Dashboard needs an EventManager.");
        if (account == null) throw new IllegalArgumentException("A Dashboard needs a logged-in account.");
        this.manager = manager;
        this.account = account;
        initComponents();       // NetBeans-generated form: do not edit
        buildContentArea();     // header + switchable screens around the existing form
        buildSidebar();         // button handlers, role rules, organizer-key button
        buildProfileHeader();
        buildHomeWidgets();
        refreshHome();
        manager.addChangeListener(dataListener);
        showCard(CARD_HOME);
        pack();                 // the profile header added extra height, so size the window again
        setLocationRelativeTo(null);
    }

    // ---------- Role rules (one place to change them) ----------
    private boolean isAdministrator() { return account != null && UserAccount.ROLE_ADMIN.equals(account.getRole()); }
    private boolean isOrganizer()     { return account != null && UserAccount.ROLE_ORGANIZER.equals(account.getRole()); }

    /** Administrators and Organizers manage events; Participants only browse and register. */
    private boolean canManageEvents() { return isAdministrator() || isOrganizer(); }
    private boolean canViewReports()  { return isAdministrator() || isOrganizer(); }
    private boolean canManageKeys()   { return isAdministrator(); }

    // ---------- Layout: wrap the form's centre panel so other screens can replace it ----------
    private void buildContentArea() {
        pnlRoot.remove(pnlCenter);                        // the form's "home" screen
        contentCards.add(pnlCenter, CARD_HOME);
        cards.put(CARD_HOME, pnlCenter);

        JPanel contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(pnlCenter.getBackground());
        contentArea.add(profileHeader, BorderLayout.NORTH);
        contentArea.add(contentCards, BorderLayout.CENTER);
        pnlRoot.add(contentArea, BorderLayout.CENTER);
    }

    // ---------- Sidebar buttons, role visibility, logout ----------
    private void buildSidebar() {
        // btnHome is already wired by the form (btnHomeActionPerformed)
        btnEvents.addActionListener(e -> showAllEvents(true));
        btnRegister.addActionListener(e -> showCard(CARD_REGISTER));
        btnManagement.addActionListener(e -> showCard(CARD_MANAGE));
        btnStats.addActionListener(e -> showCard(CARD_REPORTS));
        btnLogout.addActionListener(e -> logout());
        btnSearch.addActionListener(e -> runSearch());
        txtSearch.addActionListener(e -> runSearch());

        // Hide what this role cannot use. showCard() checks again, so this is not the only guard.
        btnManagement.setVisible(canManageEvents());
        btnStats.setVisible(canViewReports());

        int spacer = jPanel3.getPreferredSize().height;
        if (!btnManagement.isVisible()) spacer += 45;
        if (!btnStats.isVisible()) spacer += 45;

        if (canManageKeys()) {
            btnOrganizerKeys = new JButton("Organizer Keys");
            btnOrganizerKeys.setIcon(btnManagement.getIcon());
            btnOrganizerKeys.setPreferredSize(new Dimension(190, 40));
            btnOrganizerKeys.addActionListener(e -> openOrganizerKeys());
            pnlSide.add(btnOrganizerKeys, pnlSide.getComponentZOrder(jPanel3));   // just above the spacer
            spacer -= 45;
        }
        jPanel3.setPreferredSize(new Dimension(200, Math.max(0, spacer)));   // keeps Logout at the bottom
        pnlSide.revalidate();
        pnlSide.repaint();
    }

    private void buildProfileHeader() {
        profileHeader.setUser(account.getUsername(), account.getRole());
        if (canManageKeys()) profileHeader.addMenuItem("Organizer Keys", this::openOrganizerKeys);
        profileHeader.addMenuItem("Logout", this::logout);
    }

    // ---------- Navigation ----------
    private void showCard(String name) {
        if (account == null) return;
        if (CARD_HOME.equals(name)) {
            showAllEvents(false);
            return;
        }
        if (CARD_MANAGE.equals(name) && !canManageEvents()
                || CARD_REPORTS.equals(name) && !canViewReports()) {
            JOptionPane.showMessageDialog(this,
                    "Your role (" + account.getRole() + ") cannot open this section.",
                    "Access denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        switch (name) {
            case CARD_MANAGE:
                if (!cards.containsKey(name)) addCard(name, new ManageEventsPanel(manager, account));
                break;
            case CARD_REGISTER:
                // A fresh panel each time, so its event list always matches the saved events.
                addCard(name, new RegistrationPanel(manager, account));
                break;
            case CARD_REPORTS:
                if (!cards.containsKey(name)) addCard(name, new ReportPanel(manager, account));
                break;
            default:
                break;
        }
        refreshCard(name);
        cardLayout.show(contentCards, name);
    }

    /** Asks the panel inside a card (if it supports it) to re-read the shared data. */
    private void refreshCard(String name) {
        JComponent holder = cards.get(name);
        if (holder instanceof JScrollPane) {
            java.awt.Component view = ((JScrollPane) holder).getViewport().getView();
            if (view instanceof Refreshable) ((Refreshable) view).refresh();
        }
    }

    /**
     * Called by EventManager after any saved change. Redraws everything that shows event data:
     * the calendar, upcoming list, event cards, and every management/registration/report screen.
     */
    private void onDataChanged() {
        if (account == null) return;            // already logged out
        refreshHome();
        rebuildHomeCards();
        for (String name : new ArrayList<>(cards.keySet())) refreshCard(name);
    }

    private void addCard(String name, JComponent panel) {
        JComponent old = cards.remove(name);
        if (old != null) contentCards.remove(old);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        contentCards.add(scroll, name);
        cards.put(name, scroll);
        contentCards.revalidate();
    }

    // Generated handler for the Home button (kept because initComponents refers to it)
    private void btnHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHomeActionPerformed
        showCard(CARD_HOME);
    }//GEN-LAST:event_btnHomeActionPerformed

    // ---------- Home screen: calendar, upcoming events, event cards, search ----------
    private void buildHomeWidgets() {
        calendarPanel1.setOnDateSelected(this::showEventsOn);
        upcomingEventsPanel1.setOnViewAll(() -> showAllEvents(true));
    }

    /** Re-reads the events from the manager and updates the calendar and the upcoming list. */
    private void refreshHome() {
        List<Event> upcoming = manager.getUpcomingEvents(3);
        List<String> names = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        for (Event e : upcoming) {
            names.add(e.getEventName());
            dates.add(e.getDate());
        }
        upcomingEventsPanel1.setEvents(names, dates);

        Set<LocalDate> eventDays = new HashSet<>();
        for (Event e : manager.getAllEvents()) eventDays.add(e.getDate());
        calendarPanel1.setEventDates(eventDays);
    }

    /** Home / Events / View all: every event, no filter. */
    private void showAllEvents(boolean asEventsView) {
        txtSearch.setText("");
        refreshHome();
        showEventCards(() -> manager.getAllEvents(), asEventsView ? "All Events" : "Dashboard",
                "No events have been created yet.");
        cardLayout.show(contentCards, CARD_HOME);
    }

    private void runSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            showAllEvents(false);
            return;
        }
        showEventCards(() -> manager.searchEvent(keyword), "Search: " + keyword,
                "No events match \"" + keyword + "\".");
    }

    private void showEventsOn(LocalDate date) {
        txtSearch.setText("");
        showEventCards(() -> manager.getEventsOn(date), "Events on " + date.format(TITLE_DATE),
                "No events on " + date.format(TITLE_DATE) + ".");
    }

    /** Rebuilds the scrolling list of event cards. Upcoming events come first, then past ones. */
    private void showEventCards(Supplier<List<Event>> source, String title, String emptyMessage) {
        homeSource = source;
        homeEmptyMessage = emptyMessage;
        jLabel1.setText(title);
        rebuildHomeCards();
        jScrollPane1.getVerticalScrollBar().setValue(0);
    }

    /** Redraws the card list from the current source (keeps the title and the scroll position). */
    private void rebuildHomeCards() {
        if (homeSource == null) return;
        List<Event> events = homeSource.get();
        int scroll = jScrollPane1.getVerticalScrollBar().getValue();
        eventsContainer.removeAll();
        if (events.isEmpty()) {
            JLabel none = new JLabel(homeEmptyMessage);
            none.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventsContainer.add(none);
        }
        for (Event e : sortForDisplay(events)) {
            EventCardPanel card = new EventCardPanel();
            card.setEvent(e);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            eventsContainer.add(card);
            eventsContainer.add(Box.createVerticalStrut(8));
        }
        eventsContainer.revalidate();
        eventsContainer.repaint();
        final int keep = scroll;
        javax.swing.SwingUtilities.invokeLater(() -> jScrollPane1.getVerticalScrollBar().setValue(keep));
    }

    private static List<Event> sortForDisplay(List<Event> events) {
        LocalDate today = LocalDate.now();
        Comparator<Event> soonestFirst = Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime);
        Comparator<Event> latestFirst = soonestFirst.reversed();
        List<Event> upcoming = new ArrayList<>();
        List<Event> past = new ArrayList<>();
        for (Event e : events) (e.getDate().isBefore(today) ? past : upcoming).add(e);
        upcoming.sort(soonestFirst);
        past.sort(latestFirst);
        upcoming.addAll(past);
        return upcoming;
    }

    // ---------- Organizer keys (administrators only) ----------
    private void openOrganizerKeys() {
        // The dialog and EventManager both re-check the role, so a non-admin only ever gets a message.
        OrganizerKeysDialog.open(this, manager, account);
    }

    // ---------- Logout ----------
    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of CEMS?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        account = null;                         // forget who was logged in
        manager.removeChangeListener(dataListener);
        LoginFrame login = new LoginFrame(manager);
        login.setLocationRelativeTo(null);
        login.setVisible(true);
        dispose();
    }

    /**
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlRoot = new javax.swing.JPanel();
        pnlSide = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        btnHome = new javax.swing.JButton();
        btnStats = new javax.swing.JButton();
        btnRegister = new javax.swing.JButton();
        btnManagement = new javax.swing.JButton();
        btnEvents = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        btnLogout = new javax.swing.JButton();
        pnlCenter = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        eventsContainer = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        calendarPanel1 = new com.system.gui.CalendarPanel();
        upcomingEventsPanel1 = new com.system.gui.UpcomingEventsPanel();
        btnSearch = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1024, 550));
        setMinimumSize(new java.awt.Dimension(1024, 550));
        setResizable(false);
        setSize(new java.awt.Dimension(1024, 550));

        pnlRoot.setLayout(new java.awt.BorderLayout());

        pnlSide.setBackground(new java.awt.Color(255, 255, 255));
        pnlSide.setPreferredSize(new java.awt.Dimension(200, 0));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(200, 90));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 90, Short.MAX_VALUE)
        );

        pnlSide.add(jPanel1);

        btnHome.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-home-24.png"))); // NOI18N
        btnHome.setText("Home");
        btnHome.setPreferredSize(new java.awt.Dimension(190, 40));
        btnHome.addActionListener(this::btnHomeActionPerformed);
        pnlSide.add(btnHome);

        btnStats.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-combo-chart-24.png"))); // NOI18N
        btnStats.setText("Reports");
        btnStats.setPreferredSize(new java.awt.Dimension(190, 40));
        pnlSide.add(btnStats);

        btnRegister.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-register-24.png"))); // NOI18N
        btnRegister.setText("Register");
        btnRegister.setOpaque(true);
        btnRegister.setPreferredSize(new java.awt.Dimension(190, 40));
        pnlSide.add(btnRegister);

        btnManagement.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-management-24.png"))); // NOI18N
        btnManagement.setText("Manage Event");
        btnManagement.setPreferredSize(new java.awt.Dimension(190, 40));
        pnlSide.add(btnManagement);

        btnEvents.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-event-24.png"))); // NOI18N
        btnEvents.setText("Events");
        btnEvents.setPreferredSize(new java.awt.Dimension(190, 40));
        pnlSide.add(btnEvents);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setPreferredSize(new java.awt.Dimension(200, 325));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 325, Short.MAX_VALUE)
        );

        pnlSide.add(jPanel3);

        btnLogout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-logout-24.png"))); // NOI18N
        btnLogout.setText("Logout");
        btnLogout.setPreferredSize(new java.awt.Dimension(190, 40));
        pnlSide.add(btnLogout);

        pnlRoot.add(pnlSide, java.awt.BorderLayout.WEST);

        pnlCenter.setBackground(new java.awt.Color(247, 247, 252));
        pnlCenter.setMaximumSize(new java.awt.Dimension(1200, 700));
        pnlCenter.setMinimumSize(new java.awt.Dimension(1200, 700));
        pnlCenter.setPreferredSize(new java.awt.Dimension(1200, 700));

        jScrollPane1.setPreferredSize(new java.awt.Dimension(600, 500));

        eventsContainer.setBackground(new java.awt.Color(255, 255, 255));
        eventsContainer.setLayout(new javax.swing.BoxLayout(eventsContainer, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setViewportView(eventsContainer);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setMaximumSize(new java.awt.Dimension(320, 430));
        jPanel2.setMinimumSize(new java.awt.Dimension(320, 430));
        jPanel2.setPreferredSize(new java.awt.Dimension(320, 430));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(upcomingEventsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(calendarPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(calendarPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(upcomingEventsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/system/gui/icon/icons8-search-24.png"))); // NOI18N

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Dashboard");

        javax.swing.GroupLayout pnlCenterLayout = new javax.swing.GroupLayout(pnlCenter);
        pnlCenter.setLayout(pnlCenterLayout);
        pnlCenterLayout.setHorizontalGroup(
            pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCenterLayout.createSequentialGroup()
                .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(pnlCenterLayout.createSequentialGroup()
                            .addComponent(btnSearch)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 680, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlCenterLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 338, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(653, Short.MAX_VALUE))
        );
        pnlCenterLayout.setVerticalGroup(
            pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCenterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCenterLayout.createSequentialGroup()
                        .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlCenterLayout.createSequentialGroup()
                                .addGap(28, 58, Short.MAX_VALUE)
                                .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(txtSearch)))
                            .addGroup(pnlCenterLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)))
        );

        pnlRoot.add(pnlCenter, java.awt.BorderLayout.CENTER);

        getContentPane().add(pnlRoot, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * A Dashboard needs a logged-in account, so running this file directly starts the normal
     * application (login screen first).
     */
    public static void main(String args[]) {
        CEMS.Main.main(args);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEvents;
    private javax.swing.JButton btnHome;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnManagement;
    private javax.swing.JButton btnRegister;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnStats;
    private com.system.gui.CalendarPanel calendarPanel1;
    private javax.swing.JPanel eventsContainer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel pnlCenter;
    private javax.swing.JPanel pnlRoot;
    private javax.swing.JPanel pnlSide;
    private javax.swing.JTextField txtSearch;
    private com.system.gui.UpcomingEventsPanel upcomingEventsPanel1;
    // End of variables declaration//GEN-END:variables
}
