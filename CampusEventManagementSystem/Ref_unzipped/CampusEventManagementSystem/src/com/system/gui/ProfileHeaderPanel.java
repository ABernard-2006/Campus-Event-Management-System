package com.system.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class ProfileHeaderPanel extends JPanel {

    private static final Color TEXT   = new Color(0x2B2B3C);
    private static final Color SUB    = new Color(0x8A8A9C);
    private static final Color BADGE  = new Color(0xE8506A);
    private static final Font  BASE   = new Font("Segoe UI", Font.PLAIN, 13);

    // ---------- allowed roles ----------
    public enum Role {
        PARTICIPANT("Participant"),
        STUDENT("Student"),
        ADMIN("Administrator"),
        ORGANIZER("Organizer");

        private final String label;
        Role(String label) { this.label = label; }
        public String getLabel() { return label; }

        /** Converts text from your database/login (e.g. "admin", "Student") into a Role. */
        public static Role fromString(String s) {
            if (s == null) return PARTICIPANT;
            if (s.trim().equalsIgnoreCase("Admin")) return ADMIN;   // older spelling
            for (Role r : values()) {
                if (r.name().equalsIgnoreCase(s.trim()) || r.label.equalsIgnoreCase(s.trim())) return r;
            }
            return PARTICIPANT;   // default if the text doesn't match
        }
    }

    private final JLabel nameLabel = new JLabel("Alex Smith");
    private final JLabel roleLabel = new JLabel(Role.PARTICIPANT.getLabel());
    private final AvatarIcon avatar = new AvatarIcon();
    private final BellIcon bell = new BellIcon();
    private final JPopupMenu menu = new JPopupMenu();

    public ProfileHeaderPanel() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 16, 8));

        nameLabel.setFont(BASE.deriveFont(Font.BOLD, 14f));
        nameLabel.setForeground(TEXT);
        roleLabel.setFont(BASE.deriveFont(12f));
        roleLabel.setForeground(SUB);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
        text.setOpaque(false);
        text.add(nameLabel);
        text.add(roleLabel);

        add(bell);
        add(new Divider());
        add(avatar);
        add(text);
        add(new Chevron());

        // dropdown menu on click (avatar, name or arrow)
        MouseAdapter open = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                menu.show(ProfileHeaderPanel.this, getWidth() - 160, getHeight());
            }
        };
        for (Component c : new Component[]{avatar, text, nameLabel, roleLabel, getComponent(4)}) {
            c.addMouseListener(open);
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    // ---------- public API ----------
    public void setUser(String name, Role role) {
        nameLabel.setText(name);
        setRole(role);
    }

    /** Convenience: pass the role as text (e.g. straight from the database). */
    public void setUser(String name, String roleText) {
        setUser(name, Role.fromString(roleText));
    }

    public void setRole(Role role) {
        roleLabel.setText(role.getLabel());
    }

    public void setAvatar(Image img)            { avatar.image = img; avatar.repaint(); }
    public void setNotificationCount(int count) { bell.count = count; bell.repaint(); }
    public void addMenuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }
    public void setOnBellClick(Runnable r) {
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { r.run(); }
        });
    }

    // ---------- bell with red count badge ----------
    private static class BellIcon extends JComponent {
        int count = 0;
        BellIcon() { setPreferredSize(new Dimension(38, 40)); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(new Color(0x7A7A8C));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D bellShape = new Path2D.Double();
            bellShape.moveTo(8, 27);
            bellShape.curveTo(10, 25, 11, 22, 11, 17);
            bellShape.curveTo(11, 11, 15, 8, 18, 8);
            bellShape.curveTo(21, 8, 25, 11, 25, 17);
            bellShape.curveTo(25, 22, 26, 25, 28, 27);
            bellShape.closePath();
            g2.translate(0, 4);
            g2.draw(bellShape);
            g2.drawArc(14, 26, 8, 6, 180, 180);        // clapper
            g2.translate(0, -4);

            if (count > 0) {
                String s = count > 99 ? "99+" : String.valueOf(count);
                g2.setFont(BASE.deriveFont(Font.BOLD, 10f));
                FontMetrics fm = g2.getFontMetrics();
                int w = Math.max(18, fm.stringWidth(s) + 10), h = 18;
                int x = getWidth() - w, y = 0;
                g2.setColor(BADGE);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(Color.WHITE);
                g2.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    // ---------- thin vertical divider ----------
    private static class Divider extends JComponent {
        Divider() { setPreferredSize(new Dimension(1, 34)); }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(0xE6E6EE));
            g.fillRect(0, 0, 1, getHeight());
        }
    }

    // ---------- round avatar with soft ring ----------
    private static class AvatarIcon extends JComponent {
        Image image;
        AvatarIcon() { setPreferredSize(new Dimension(52, 52)); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int s = 48, x = (getWidth() - s) / 2, y = (getHeight() - s) / 2;

            g2.setColor(new Color(0xB9CBFA));                // light blue background
            g2.fillOval(x, y, s, s);
            if (image != null) {
                g2.setClip(new Ellipse2D.Double(x, y, s, s));
                g2.drawImage(image, x, y, s, s, null);
                g2.setClip(null);
            } else {
                g2.setColor(Color.WHITE);                    // placeholder silhouette
                g2.fillOval(x + 16, y + 9, 16, 16);
                g2.fillArc(x + 8, y + 28, 32, 30, 0, 180);
            }
            g2.setColor(Color.WHITE);                        // white ring
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(x, y, s, s);
            g2.dispose();
        }
    }

    // ---------- small down arrow ----------
    private static class Chevron extends JComponent {
        Chevron() { setPreferredSize(new Dimension(18, 18)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x4A4A5C));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() / 2, cy = getHeight() / 2;
            g2.drawLine(cx - 5, cy - 2, cx, cy + 3);
            g2.drawLine(cx, cy + 3, cx + 5, cy - 2);
            g2.dispose();
        }
    }
}