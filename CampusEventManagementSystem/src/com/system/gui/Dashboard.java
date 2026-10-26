/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.system.gui;

import com.system.service.EventManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
// @author Dhustine

public class Dashboard extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Dashboard.class.getName());

    private static final Color SIDEBAR_BG = new Color(0, 102, 153);
    private static final Color HOVER_BG   = new Color(0, 130, 190);
    private static final Color SUB_BG     = new Color(0, 85, 130);

    private EventManager manager;
    private String userRole;
    private final CardLayout cardLayout = new CardLayout();

    // Kept as fields so role permissions can hide them
    private JButton btnReports;
    private JPanel reportsSub;
    private JButton btnEventsDropdown;
    private JPanel eventsSub;

    // ---------- Constructors ----------
    public Dashboard() {
        this(new EventManager(), null);
    }

    public Dashboard(EventManager manager) {   // used by the login screen
        this(manager, null);
    }

    public Dashboard(EventManager manager, String userRole) {
        this.manager = manager;
        this.userRole = userRole;
        initComponents();
        setupUI();
        applyRolePermissions();
        setLocationRelativeTo(null);
    }

    // ---------- Role permissions ----------
    private void applyRolePermissions() {
    if ("Participant".equalsIgnoreCase(userRole)) {
        btnEventMgmt.setVisible(false);   // participants can't manage events
        btnReport.setVisible(false);      // or see reports
    } else if ("Organizer".equalsIgnoreCase(userRole)) {
        // Organizers get event management
    } else if ("Administrator".equalsIgnoreCase(userRole)) {
        // Full access
    }
    sidebarPanel.revalidate();
    sidebarPanel.repaint();
}

    // ---------- Sidebar + content setup ----------
    private void setupUI() {
        // Content area: each panel is a "card"
        contentPanel.setLayout(cardLayout);
        contentPanel.add(new DisplayEventsPanel(), "events");
        contentPanel.add(new RegistrationPanel(), "register");
        contentPanel.add(new ReportPanel(), "reports");

        // Sidebar
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setPreferredSize(new Dimension(220, 600));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Plain button
        sidebarPanel.add(createNavButton("Dashboard", e -> showCard("events")));

        // Dropdown: Events
        eventsSub = createSubMenu(
                createSubButton("View Events", e -> showCard("events")),
                createSubButton("Register", e -> showCard("register"))
        );
        btnEventsDropdown = createDropdownButton("Events", eventsSub);
        sidebarPanel.add(btnEventsDropdown);
        sidebarPanel.add(eventsSub);

        // Dropdown: Reports
        reportsSub = createSubMenu(
                createSubButton("Summary", e -> showCard("reports"))
        );
        btnReports = createDropdownButton("Reports", reportsSub);
        sidebarPanel.add(btnReports);
        sidebarPanel.add(reportsSub);

        sidebarPanel.add(Box.createVerticalGlue());
    }

    private void showCard(String name) {
        cardLayout.show(contentPanel, name);
    }

    // ---------- Helper methods ----------
    private JButton styleButton(JButton b, int leftPad, Color bg) {
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(0, leftPad, 0, 10));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(HOVER_BG); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    private JButton createNavButton(String text, ActionListener action) {
        JButton b = styleButton(new JButton(text), 20, SIDEBAR_BG);
        b.addActionListener(action);
        return b;
    }

    private JButton createSubButton(String text, ActionListener action) {
        JButton b = styleButton(new JButton(text), 40, SUB_BG);
        b.addActionListener(action);
        return b;
    }

    private JPanel createSubMenu(JButton... buttons) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SUB_BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton b : buttons) p.add(b);
        p.setVisible(false); // collapsed by default
        return p;
    }

    private JButton createDropdownButton(String text, JPanel subMenu) {
        JButton b = styleButton(new JButton(text + "  ▾"), 20, SIDEBAR_BG);
        b.addActionListener(e -> {
            boolean show = !subMenu.isVisible();
            subMenu.setVisible(show);
            b.setText(text + (show ? "  ▴" : "  ▾"));
            sidebarPanel.revalidate();
            sidebarPanel.repaint();
        });
        return b;
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
        btnSearch.setMaximumSize(new java.awt.Dimension(30, 30));
        btnSearch.setMinimumSize(new java.awt.Dimension(30, 30));
        btnSearch.setPreferredSize(new java.awt.Dimension(30, 30));

        javax.swing.GroupLayout pnlCenterLayout = new javax.swing.GroupLayout(pnlCenter);
        pnlCenter.setLayout(pnlCenterLayout);
        pnlCenterLayout.setHorizontalGroup(
            pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCenterLayout.createSequentialGroup()
                .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 680, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlCenterLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 338, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(630, Short.MAX_VALUE))
        );
        pnlCenterLayout.setVerticalGroup(
            pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCenterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCenterLayout.createSequentialGroup()
                        .addGap(0, 58, Short.MAX_VALUE)
                        .addGroup(pnlCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtSearch))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)))
        );

        pnlRoot.add(pnlCenter, java.awt.BorderLayout.CENTER);

        getContentPane().add(pnlRoot, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHomeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnHomeActionPerformed

    /**
     * @param args the command line arguments
     */
   public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new Dashboard().setVisible(true));
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
