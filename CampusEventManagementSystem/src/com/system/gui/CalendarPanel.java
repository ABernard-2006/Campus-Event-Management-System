package com.system.gui;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;

public class CalendarPanel extends JPanel {

    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TEXT      = new Color(0x2B2B3C);
    private static final Color MUTED     = new Color(0xC4C4D0);
    private static final Color ACCENT    = new Color(0x7C6FF0);
    private static final Font  BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private final JLabel monthLabel = new JLabel();
    private final JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
    private final Set<LocalDate> eventDates = new HashSet<>();   // days that get a small dot
    private Consumer<LocalDate> onDateSelected;

    public CalendarPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // Header: title + arrows
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Calendar");
        title.setFont(BASE_FONT.deriveFont(Font.BOLD, 18f));
        title.setForeground(TEXT);

        JPanel arrows = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        arrows.setOpaque(false);
        arrows.add(arrowButton("<", -1));
        arrows.add(arrowButton(">", 1));
        header.add(title, BorderLayout.WEST);
        header.add(arrows, BorderLayout.EAST);

        monthLabel.setFont(BASE_FONT.deriveFont(Font.PLAIN, 13f));
        monthLabel.setForeground(TEXT);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(monthLabel, BorderLayout.SOUTH);

        grid.setOpaque(false);
        add(top, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);

        rebuild();
    }

    private JButton arrowButton(String text, int delta) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xEFEDFD) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(0xE6E6EE));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setPreferredSize(new Dimension(28, 28));
        b.setMargin(new Insets(0, 0, 0, 0));     // otherwise some look-and-feels show "..." instead of < >
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(TEXT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> { month = month.plusMonths(delta); rebuild(); });
        return b;
    }

    private void rebuild() {
        grid.removeAll();
        monthLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + month.getYear());

        // Weekday header (Mo..Su)
        for (DayOfWeek d : DayOfWeek.values()) {
            String name = d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2);
            JLabel l = new JLabel(name, SwingConstants.CENTER);
            l.setFont(BASE_FONT.deriveFont(Font.BOLD, 12f));
            l.setForeground(TEXT);
            l.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            grid.add(l);
        }

        // Start on the Monday of the week containing the 1st
        LocalDate first = month.atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1);

        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);
            grid.add(new DayCell(date, !date.getMonth().equals(month.getMonth())));
        }
        grid.revalidate();
        grid.repaint();
    }

    // One clickable day
    private class DayCell extends JComponent {
        private final LocalDate date;
        private final boolean outside;
        private boolean hover;

        DayCell(LocalDate date, boolean outside) {
            this.date = date;
            this.outside = outside;
            setPreferredSize(new Dimension(38, 38));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selected = date;
                    if (outside) month = YearMonth.from(date);
                    rebuild();
                    if (onDateSelected != null) onDateSelected.accept(date);
                }
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            boolean isSel = date.equals(selected);

            if (isSel) {
                g2.setColor(ACCENT);
                g2.fillRoundRect(x, y, size, size, 16, 16);
            } else if (hover) {
                g2.setColor(new Color(0xF1EFFD));
                g2.fillRoundRect(x, y, size, size, 16, 16);
            }

            g2.setFont(BASE_FONT.deriveFont(isSel ? Font.BOLD : Font.PLAIN, 14f));
            g2.setColor(isSel ? Color.WHITE : outside ? MUTED : TEXT);
            String s = String.valueOf(date.getDayOfMonth());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());

            if (eventDates.contains(date)) {                 // dot = at least one event that day
                g2.setColor(isSel ? Color.WHITE : ACCENT);
                g2.fillOval(getWidth() / 2 - 2, y + size - 7, 5, 5);
            }
            g2.dispose();
        }
    }

    // Rounded white card with soft shadow
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 8, h = getHeight() - 8;
        for (int i = 4; i > 0; i--) {                      // fake soft shadow
            g2.setColor(new Color(90, 80, 160, 10));
            g2.fillRoundRect(4 - i, 4 - i + 3, w + i * 2, h + i * 2, 28, 28);
        }
        g2.setColor(CARD_BG);
        g2.fillRoundRect(4, 4, w, h, 24, 24);
        g2.dispose();
        super.paintComponent(g);
    }

    public LocalDate getSelectedDate() { return selected; }

    /** Marks every date in the collection with a dot. Pass the dates of the real events. */
    public void setEventDates(Collection<LocalDate> dates) {
        eventDates.clear();
        if (dates != null) eventDates.addAll(dates);
        rebuild();
    }

    /** Called with the clicked date each time the user picks a day. */
    public void setOnDateSelected(Consumer<LocalDate> listener) { onDateSelected = listener; }
}