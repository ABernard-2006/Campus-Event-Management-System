package com.system.gui;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.swing.*;

public class UpcomingEventsPanel extends JPanel {

    private static final Color TEXT  = new Color(0x2B2B3C);
    private static final Color SUB   = new Color(0x7A7A8C);
    private static final Color ACCENT = new Color(0x7C6FF0);
    private static final Font  BASE  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final JPanel list = new JPanel();
    private Runnable onViewAll;

    public UpcomingEventsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));

        JLabel title = new JLabel("Upcoming Events");
        title.setFont(BASE.deriveFont(Font.BOLD, 17f));
        title.setForeground(TEXT);

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        // Footer: "View all events  →"
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JLabel viewAll = new JLabel("View all events");
        viewAll.setFont(BASE.deriveFont(Font.BOLD, 13f));
        viewAll.setForeground(ACCENT);
        JLabel arrow = new JLabel("\u2192");
        arrow.setFont(BASE.deriveFont(Font.BOLD, 16f));
        arrow.setForeground(ACCENT);
        footer.add(viewAll, BorderLayout.WEST);
        footer.add(arrow, BorderLayout.EAST);
        footer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footer.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (onViewAll != null) onViewAll.run(); }
        });

        add(title, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void setOnViewAll(Runnable r) { onViewAll = r; }

    /** Call this with your real events. */
    public void setEvents(List<String> names, List<LocalDate> dates) {
        list.removeAll();
        if (names.isEmpty()) {
            JLabel none = new JLabel("No upcoming events.");
            none.setFont(BASE.deriveFont(12f));
            none.setForeground(SUB);
            none.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            list.add(none);
        }
        for (int i = 0; i < names.size(); i++) {
            list.add(new EventRow(names.get(i), dates.get(i), i < names.size() - 1));
        }
        list.revalidate();
        list.repaint();
    }

    // ---------- one row ----------
    private static class EventRow extends JPanel {
        private final boolean divider;

        EventRow(String name, LocalDate date, boolean divider) {
            this.divider = divider;
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

            long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
            Color[] c = colorsFor(days);              // {background, foreground}

            // text block
            JLabel n = new JLabel(name);
            n.setFont(BASE.deriveFont(Font.BOLD, 13f));
            n.setForeground(TEXT);
            JLabel d = new JLabel(date.format(FMT));
            d.setFont(BASE.deriveFont(12f));
            d.setForeground(SUB);
            JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
            text.setOpaque(false);
            text.add(n);
            text.add(d);

            add(new IconBox(c[0], c[1]), BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(new Badge(labelFor(days), c[0], c[1]), BorderLayout.EAST);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (divider) {
                g.setColor(new Color(0xEEEEF4));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        }

        static String labelFor(long days) {
            if (days < 0)  return "Ended";
            if (days == 0) return "Today";
            if (days == 1) return "1 day left";
            return days + " days left";
        }

        static Color[] colorsFor(long days) {
            if (days < 0)  return new Color[]{new Color(0xEEEEF2), new Color(0x8A8A99)};
            if (days <= 2) return new Color[]{new Color(0xFDE6EA), new Color(0xE0556E)};
            if (days <= 7) return new Color[]{new Color(0xFFF1D0), new Color(0xC99A1B)};
            return new Color[]{new Color(0xE0F5E6), new Color(0x3DA35D)};
        }
    }

    // ---------- rounded icon square with a tiny calendar glyph ----------
    private static class IconBox extends JComponent {
        private final Color bg, fg;
        IconBox(Color bg, Color fg) {
            this.bg = bg; this.fg = fg;
            setPreferredSize(new Dimension(40, 40));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s = 38, y = (getHeight() - s) / 2;
            g2.setColor(bg);
            g2.fillRoundRect(0, y, s, s, 12, 12);
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(10, y + 11, 18, 17, 5, 5);       // calendar body
            g2.drawLine(10, y + 17, 28, y + 17);              // header line
            g2.drawLine(15, y + 8, 15, y + 13);               // rings
            g2.drawLine(23, y + 8, 23, y + 13);
            g2.dispose();
        }
    }

    // ---------- pill badge ("3 days left") ----------
    private static class Badge extends JComponent {
        private final String text; private final Color bg, fg;
        Badge(String text, Color bg, Color fg) {
            this.text = text; this.bg = bg; this.fg = fg;
            setFont(BASE.deriveFont(Font.BOLD, 11.5f));
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(fm.stringWidth(text) + 26, 26));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int h = 26, y = (getHeight() - h) / 2;
            g2.setColor(bg);
            g2.fillRoundRect(0, y, getWidth(), h, h, h);
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ---------- rounded white card with soft shadow ----------
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 8, h = getHeight() - 8;
        for (int i = 4; i > 0; i--) {
            g2.setColor(new Color(90, 80, 160, 10));
            g2.fillRoundRect(4 - i, 4 - i + 3, w + i * 2, h + i * 2, 28, 28);
        }
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(4, 4, w, h, 24, 24);
        g2.dispose();
        super.paintComponent(g);
    }
}