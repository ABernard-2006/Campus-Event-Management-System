package com.system.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BrandPanel extends JPanel {

    private static final Color NAME_COLOR = UiKit.NAVY;              // deep navy, same as the headings
    private static final Color SUB_COLOR  = UiKit.MUTED;
    private static final Font  BASE       = new Font("Segoe UI", Font.PLAIN, 13);
    
    /** Used by the NetBeans Design view (drag and drop). */
public BrandPanel() {
    this(loadLogo("/com/system/gui/icon/cems_logo.jpg", 520, 160, 370, 280),
         "CEMS", "");
    setPreferredSize(new Dimension(200, 95));
}

    public BrandPanel(Image logo, String title, String subtitle) {
        setOpaque(false);
        setLayout(new GridBagLayout());                 // keeps everything vertically centred

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;

        // logo
        if (logo != null) {
            c.gridx = 0;
            c.insets = new Insets(0, 16, 0, 10);
            add(new JLabel(new ImageIcon(fit(logo, 64, 48))), c);
        }

        // name + optional subtitle
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 0));
        text.setOpaque(false);
        JLabel name = new JLabel(title);
        name.setFont(BASE.deriveFont(Font.BOLD, 22f));
        name.setForeground(NAME_COLOR);
        text.add(name);
        if (subtitle != null && !subtitle.isEmpty()) {
            for (String line : subtitle.split("\n")) {          // one small label per line
                JLabel sub = new JLabel(line);
                sub.setFont(BASE.deriveFont(11f));
                sub.setForeground(SUB_COLOR);
                text.add(sub);
            }
        }
        c.gridx = 1;
        c.insets = new Insets(0, logo == null ? 16 : 0, 0, 0);
        add(text, c);
    }

    /** Load the whole image from the classpath, e.g. "/com/system/gui/icon/cems_logo.jpg". */
    public static Image loadLogo(String resourcePath) {
        try {
            URL url = BrandPanel.class.getResource(resourcePath);
            return url == null ? null : ImageIO.read(url);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Like {@link #loadLogo(String, int, int, int, int)}, but the logo's near-white background is made
     * transparent, so the emblem sits cleanly on any page colour (white sidebar, grey page, white card).
     */
    public static Image loadLogoTransparent(String resourcePath, int x, int y, int w, int h) {
        Image img = loadLogo(resourcePath, x, y, w, h);
        if (!(img instanceof BufferedImage)) return img;
        BufferedImage src = (BufferedImage) img;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < src.getHeight(); py++) {
            for (int px = 0; px < src.getWidth(); px++) {
                int p = src.getRGB(px, py);
                int lowest = Math.min((p >> 16) & 255, Math.min((p >> 8) & 255, p & 255));
                int alpha = lowest >= 248 ? 0 : lowest <= 228 ? 255 : (int) (255 * (248 - lowest) / 20.0);
                out.setRGB(px, py, (alpha << 24) | (p & 0xFFFFFF));
            }
        }
        return out;
    }

    /** Load and crop to a region (x, y, width, height), useful to keep only the emblem. */
    public static Image loadLogo(String resourcePath, int x, int y, int w, int h) {
        Image img = loadLogo(resourcePath);
        if (!(img instanceof BufferedImage)) return img;
        BufferedImage bi = (BufferedImage) img;
        x = Math.max(0, x);
        y = Math.max(0, y);
        w = Math.min(w, bi.getWidth() - x);
        h = Math.min(h, bi.getHeight() - y);
        return bi.getSubimage(x, y, w, h);
    }

    /** High-quality scale that keeps the aspect ratio inside maxW x maxH. */
    static Image fit(Image src, int maxW, int maxH) {
        int sw = src.getWidth(null), sh = src.getHeight(null);
        double scale = Math.min((double) maxW / sw, (double) maxH / sh);
        int w = Math.max(1, (int) Math.round(sw * scale));
        int h = Math.max(1, (int) Math.round(sh * scale));

        // scale in two steps for a smoother result on big images
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Image half = src.getScaledInstance(Math.max(w * 2, 1), Math.max(h * 2, 1), Image.SCALE_SMOOTH);
        g2.drawImage(half, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }
}