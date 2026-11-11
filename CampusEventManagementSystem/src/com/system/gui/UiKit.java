package com.system.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 * The look of the whole application in one place: the colour palette, button styles, table style and
 * a few layout helpers, so every screen is built the same way and a colour is changed once.
 * Handwritten (no .form file).
 *
 * The palette follows the iBU (Bicol University) student app: sky blue as the brand colour, deep navy
 * for headings, white cards on a light grey page, and green / orange / red for status.
 *
 * Buttons are painted by {@link FlatButtonUI}, not by the operating system's look and feel. The Windows
 * look and feel ignores a button's background colour, so a button set up with setBackground() alone shows
 * as a plain grey button; the style methods below always paint the chosen colour.
 */
final class UiKit {
    // ---------------- palette ----------------
    static final Color PRIMARY       = new Color(0x477CC2);   // brand blue: main buttons, selected items, links
    static final Color PRIMARY_DARK  = new Color(0x3A68A6);   // hover colour, text on light backgrounds
    static final Color PRIMARY_PRESS = new Color(0x315A92);
    static final Color ACCENT        = new Color(0x6DAEED);   // light sky blue: highlights and icons
    static final Color NAVY          = new Color(0x0E1B2B);   // headings and body text
    static final Color PAGE          = new Color(0xF6F6F6);   // page background
    static final Color CARD          = Color.WHITE;           // cards and panels
    static final Color MUTED         = new Color(0x6B7787);   // secondary text
    static final Color LINE          = new Color(0xE1E6EE);   // thin card outline
    static final Color LINE_STRONG   = new Color(0xC3D1E4);   // outline of inputs and secondary buttons
    static final Color SOFT_BLUE     = new Color(0xE8F1FB);   // hover background
    static final Color SELECT_BLUE   = new Color(0xD6E6F8);   // selected table row
    static final Color SUCCESS       = new Color(0x6AB66C);
    static final Color SUCCESS_DARK  = new Color(0x3E8E41);   // success text and filled success buttons
    static final Color WARNING       = new Color(0xE07B34);
    static final Color WARNING_DARK  = new Color(0xC25E1C);
    static final Color DANGER        = new Color(0xD2445B);
    static final Color DANGER_DARK   = new Color(0xB3354B);

    /** Page background (kept under its old name: other screens already use UiKit.BG). */
    static final Color BG = PAGE;

    private static final String BASE_ICON = "cems.baseIcon";

    private UiKit() { }

    // =====================================================================
    //  BUTTONS
    // =====================================================================

    /** Filled brand-blue button: the main action of a screen (Login, Register, Create, Save). */
    static void primaryButton(AbstractButton b) {
        flat(b, PRIMARY, PRIMARY_DARK, PRIMARY_PRESS, null, Color.WHITE, 6, 14, 10, true);
    }

    /** Filled button in a colour of your choice (white bold text). */
    static void primaryButton(AbstractButton b, Color fill) {
        flat(b, fill, darker(fill, 0.88f), darker(fill, 0.76f), null, Color.WHITE, 6, 14, 10, true);
    }

    /** White button with a blue outline: a supporting action (Search, Show All, Clear, Cancel). */
    static void secondaryButton(AbstractButton b) {
        flat(b, CARD, SOFT_BLUE, SELECT_BLUE, LINE_STRONG, PRIMARY_DARK, 6, 14, 10, true);
    }

    /** Smaller side padding, for buttons in forms that give them a fixed width. */
    static void compact(AbstractButton b) {
        b.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
    }

    /** Filled green button: a positive action (Mark Present). */
    static void successButton(AbstractButton b) {
        flat(b, SUCCESS_DARK, new Color(0x337A36), new Color(0x2B692D), null, Color.WHITE, 6, 14, 10, true);
    }

    /** Filled red button: a destructive action (Delete, Revoke, Mark Absent, Cancel registration). */
    static void dangerButton(AbstractButton b) {
        flat(b, DANGER, DANGER_DARK, new Color(0x982B3F), null, Color.WHITE, 6, 14, 10, true);
    }

    /**
     * Sidebar navigation button. The selected one is filled blue with a white icon and text; the others
     * are transparent with a blue icon and show a light blue background under the mouse. The icon given
     * in the Design view is remembered, so the style can be switched back and forth.
     */
    static void navButton(AbstractButton b, boolean active) {
        Icon base = baseIcon(b);
        flat(b, active ? PRIMARY : null, active ? PRIMARY_DARK : SOFT_BLUE, active ? PRIMARY_PRESS : SELECT_BLUE,
                null, active ? Color.WHITE : NAVY, 0, 16, 12, active);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIconTextGap(12);
        if (base != null) b.setIcon(tint(base, active ? Color.WHITE : PRIMARY));
    }

    /** Sidebar button for Logout: same shape as the others, in red. */
    static void navButtonDanger(AbstractButton b) {
        Icon base = baseIcon(b);
        flat(b, null, new Color(0xFCE8EC), new Color(0xF8D3DA), null, DANGER_DARK, 0, 16, 12, false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIconTextGap(12);
        if (base != null) b.setIcon(tint(base, DANGER));
    }

    private static Icon baseIcon(AbstractButton b) {
        Icon base = (Icon) b.getClientProperty(BASE_ICON);
        if (base == null && b.getIcon() != null) {
            base = b.getIcon();
            b.putClientProperty(BASE_ICON, base);
        }
        return base;
    }

    private static void flat(AbstractButton b, Color fill, Color hover, Color press, Color line, Color fg,
                             int padV, int padH, int arc, boolean bold) {
        b.setUI(new FlatButtonUI(fill, hover, press, line, arc));   // first, so the settings below are kept
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setForeground(fg);
        b.setBackground(fill != null ? fill : CARD);
        Font f = b.getFont();
        b.setFont(f.deriveFont(bold ? Font.BOLD : Font.PLAIN));
        b.setBorder(BorderFactory.createEmptyBorder(padV, padH, padV, padH));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Paints a rounded button itself: fill, hover/pressed colour and optional outline, then the icon and text. */
    private static final class FlatButtonUI extends BasicButtonUI {
        private final Color fill, hover, press, line;
        private final int arc;

        FlatButtonUI(Color fill, Color hover, Color press, Color line, int arc) {
            this.fill = fill;
            this.hover = hover;
            this.press = press;
            this.line = line;
            this.arc = arc;
        }

        @Override
        public void update(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Color bg = fill;
            if (!b.isEnabled()) {
                bg = fill == null ? null : LINE;
            } else if (m.isPressed() && m.isArmed()) {
                bg = press;
            } else if (m.isRollover()) {
                bg = hover;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (bg != null) {
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);
            }
            if (line != null) {
                g2.setColor(b.isEnabled() ? line : LINE);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, arc, arc);
            }
            g2.dispose();
            paint(g, c);
        }

        @Override
        protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
            AbstractButton b = (AbstractButton) c;
            FontMetrics fm = c.getFontMetrics(c.getFont());
            g.setColor(b.isEnabled() ? b.getForeground() : MUTED);
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, b.getDisplayedMnemonicIndex(),
                    textRect.x + getTextShiftOffset(), textRect.y + fm.getAscent() + getTextShiftOffset());
        }
    }

    // =====================================================================
    //  ICONS AND COLOURS
    // =====================================================================

    /** A copy of the icon painted in one colour (the shape and transparency are kept). Used to recolour black icons. */
    static Icon tint(Icon icon, Color color) {
        if (icon == null) return null;
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        int rgb = color.getRGB() & 0xFFFFFF;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int alpha = img.getRGB(x, y) >>> 24;
                img.setRGB(x, y, (alpha << 24) | rgb);
            }
        return new ImageIcon(img);
    }

    private static Color darker(Color c, float factor) {
        return new Color(Math.max(0, (int) (c.getRed() * factor)), Math.max(0, (int) (c.getGreen() * factor)),
                Math.max(0, (int) (c.getBlue() * factor)));
    }

    // =====================================================================
    //  TABLES
    // =====================================================================

    /** One look for every table: navy header, thin row lines, light stripes and a light blue selection. */
    static void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(LINE);
        t.setBackground(CARD);
        t.setForeground(NAVY);
        t.setSelectionBackground(SELECT_BLUE);
        t.setSelectionForeground(NAVY);
        t.setFillsViewportHeight(true);

        DefaultTableCellRenderer body = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!selected) {
                    setBackground(row % 2 == 0 ? CARD : new Color(0xF9FBFE));
                    setForeground(NAVY);
                }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, body);
        t.setDefaultRenderer(Number.class, body);
        t.setDefaultRenderer(Integer.class, body);

        JTableHeader header = t.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, false, false, row, col);
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }

    /**
     * Colours the words in one column by meaning: green for Present / Registered / Active, red for
     * Absent / Cancelled / Revoked / Expired, grey for Not marked / Used. Call after the table model is set.
     */
    static void colorStatusColumn(JTable t, int column) {
        TableColumn tc = t.getColumnModel().getColumn(column);
        tc.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                String s = value == null ? "" : value.toString().trim().toLowerCase();
                Color text = NAVY;
                if (s.equals("present") || s.equals("registered") || s.equals("active")) text = SUCCESS_DARK;
                else if (s.equals("absent") || s.equals("cancelled") || s.equals("revoked") || s.equals("expired")) text = DANGER_DARK;
                else if (s.equals("not marked") || s.equals("used") || s.equals("-")) text = MUTED;
                if (!selected) setBackground(row % 2 == 0 ? CARD : new Color(0xF9FBFE));
                setForeground(text);
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        });
    }

    // =====================================================================
    //  LAYOUT HELPERS
    // =====================================================================

    /** Sets only the preferred width of a component and keeps its natural height. */
    static void width(JComponent c, int w) {
        c.setPreferredSize(null);
        c.setPreferredSize(new Dimension(w, c.getPreferredSize().height));
    }

    /** Sets the preferred width and height of a component. */
    static void size(JComponent c, int w, int h) {
        c.setPreferredSize(new Dimension(w, h));
    }

    /** Button at least minWidth wide, wider if its text needs it, and a little taller than default. */
    static void button(AbstractButton b, int minWidth) {
        b.setPreferredSize(null);
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(Math.max(minWidth, d.width + 8), d.height + 4));
    }

    /** A see-through panel (used to group controls without changing the background). */
    static JPanel transparent(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    /** A white panel with a thin outline and inner padding, like the cards on the dashboard. */
    static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(CARD);
        p.setBorder(cardBorder(20, 28));
        return p;
    }

    static Border cardBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    /** Outline and padding for a text box (used by the dashboard search box). */
    static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(LINE_STRONG, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    /**
     * Adds one "label : field" row to a panel that uses GridBagLayout. Labels sit in the left
     * column, fields stretch across the right column, so every row lines up.
     */
    static void row(JPanel p, int y, JComponent label, JComponent field, boolean topAlignLabel) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridx = 0;
        c.anchor = topAlignLabel ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
        c.insets = new Insets(topAlignLabel ? 9 : 6, 0, 6, 18);
        p.add(label, c);

        c = new GridBagConstraints();
        c.gridy = y;
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        p.add(field, c);
    }

    static void row(JPanel p, int y, JComponent label, JComponent field) {
        row(p, y, label, field, false);
    }

    /** Places content at the top and horizontally centred, at its natural size. */
    static JPanel topCentered(JComponent content) {
        JPanel holder = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 1;
        c.weighty = 1;
        holder.add(content, c);
        return holder;
    }

    /**
     * Goes inside a JScrollPane around a screen. Unlike a bare panel, it stretches the screen to
     * fill the window when the window is bigger than the screen needs, and only shows scroll bars
     * when the window is smaller than the screen needs.
     */
    static final class ScrollFit extends JPanel implements Scrollable, Refreshable {
        private final JComponent view;

        ScrollFit(JComponent view) {
            super(new BorderLayout());
            this.view = view;
            setBackground(BG);
            add(view, BorderLayout.CENTER);
        }

        @Override public void refresh() {
            if (view instanceof Refreshable) ((Refreshable) view).refresh();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 16);
        }

        @Override public boolean getScrollableTracksViewportWidth() {
            Container p = getParent();
            return p instanceof JViewport && p.getWidth() >= getPreferredSize().width;
        }

        @Override public boolean getScrollableTracksViewportHeight() {
            Container p = getParent();
            return p instanceof JViewport && p.getHeight() >= getPreferredSize().height;
        }
    }
}
