package com.smartgym.gui.scheduling;

import com.smartgym.gui.common.Theme;
import com.smartgym.gui.trainers.AvailabilityPicker;
import com.smartgym.model.Trainer;
import com.smartgym.model.TrainerSchedule;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A one-day timetable drawn with Java2D: one column per trainer, hours down the left side.
 *   - sessions are coloured blocks (blue = scheduled, green = completed, grey = cancelled)
 *   - hours outside the trainer's working time are shaded grey
 *   - a red line shows the current time (when the date is today)
 *   - double-click an empty slot = book there, double-click a block = edit that session
 */
public class TimetablePanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    /** Layout constants (public so tests and other classes can calculate positions). */
    public static final int OPEN_HOUR = 5;
    public static final int CLOSE_HOUR = 22;
    public static final int HOUR_HEIGHT = 46;
    public static final int LEFT_MARGIN = 62;
    public static final int HEADER_HEIGHT = 58;
    private static final int MIN_COLUMN_WIDTH = 150;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Who is told when the user double-clicks the timetable. */
    public interface Listener {
        void emptySlotOpened(Trainer trainer, LocalTime time);

        void sessionOpened(TrainerSchedule session);
    }

    private LocalDate date = LocalDate.now();
    private transient List<Trainer> trainers = new ArrayList<>();
    private transient List<TrainerSchedule> sessions = new ArrayList<>();
    private transient Listener listener;
    private transient TrainerSchedule hovered;

    public TimetablePanel() {
        setBackground(Color.WHITE);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                TrainerSchedule s = sessionAt(e.getX(), e.getY());
                if (s != hovered) {
                    hovered = s;
                    setCursor(s == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    repaint();
                }
                setToolTipText(s == null ? null : "<html><b>" + s.getMemberName() + "</b><br>"
                        + s.getStartTime().format(TIME) + " - " + s.getEndTime().format(TIME) + "  (" + s.getDurationMinutes() + " min)<br>"
                        + "Trainer: " + s.getTrainerName() + "<br>Status: " + s.getStatus()
                        + (s.getNotes() == null || s.getNotes().isEmpty() ? "" : "<br>" + s.getNotes()) + "</html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hovered != null) {
                    hovered = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || listener == null) {
                    return;
                }
                TrainerSchedule s = sessionAt(e.getX(), e.getY());
                if (s != null) {
                    listener.sessionOpened(s);
                    return;
                }
                int column = columnAt(e.getX());
                if (column >= 0 && e.getY() >= HEADER_HEIGHT) {
                    listener.emptySlotOpened(trainers.get(column), timeAt(e.getY()));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        setToolTipText("");                                       // switches tooltips on for this component
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Shows one day: the trainers (columns) and that day's sessions. */
    public void setData(LocalDate date, List<Trainer> trainers, List<TrainerSchedule> sessions) {
        this.date = date;
        this.trainers = new ArrayList<>(trainers);
        this.sessions = new ArrayList<>(sessions);
        this.hovered = null;
        revalidate();
        repaint();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    // ================================================================ geometry

    private int columnWidth() {
        int n = Math.max(1, trainers.size());
        return Math.max(MIN_COLUMN_WIDTH, (getWidth() - LEFT_MARGIN - 8) / n);
    }

    private int columnAt(int x) {
        if (x < LEFT_MARGIN) {
            return -1;
        }
        int c = (x - LEFT_MARGIN) / columnWidth();
        return c < trainers.size() ? c : -1;
    }

    /** Time that belongs to a y position, rounded down to 30 minutes. */
    private LocalTime timeAt(int y) {
        int minutes = OPEN_HOUR * 60 + (y - HEADER_HEIGHT) * 60 / HOUR_HEIGHT;
        minutes = Math.max(OPEN_HOUR * 60, Math.min(CLOSE_HOUR * 60 - 30, minutes / 30 * 30));
        return LocalTime.of(minutes / 60, minutes % 60);
    }

    private int yFor(LocalTime time) {
        return HEADER_HEIGHT + (time.getHour() * 60 + time.getMinute() - OPEN_HOUR * 60) * HOUR_HEIGHT / 60;
    }

    /** The rectangle of a session block, or null if its trainer has no column. */
    private Rectangle rectFor(TrainerSchedule s) {
        for (int i = 0; i < trainers.size(); i++) {
            if (trainers.get(i).getTrainerId() == s.getTrainerId()) {
                int w = columnWidth();
                int y = yFor(s.getStartTime());
                int h = Math.max(18, s.getDurationMinutes() * HOUR_HEIGHT / 60);
                return new Rectangle(LEFT_MARGIN + i * w + 4, y + 1, w - 8, h - 2);
            }
        }
        return null;
    }

    private TrainerSchedule sessionAt(int x, int y) {
        for (int i = sessions.size() - 1; i >= 0; i--) {           // later (top) blocks first
            Rectangle r = rectFor(sessions.get(i));
            if (r != null && r.contains(x, y)) {
                return sessions.get(i);
            }
        }
        return null;
    }

    // ================================================================ painting

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int colW = columnWidth();
        int gridBottom = HEADER_HEIGHT + (CLOSE_HOUR - OPEN_HOUR) * HOUR_HEIGHT;
        int gridRight = LEFT_MARGIN + colW * Math.max(1, trainers.size());

        if (trainers.isEmpty()) {
            g2.setFont(Theme.regular(14));
            g2.setColor(Theme.MUTED);
            g2.drawString("No trainers to show.", LEFT_MARGIN + 20, HEADER_HEIGHT + 40);
            g2.dispose();
            return;
        }

        // shade the hours when a trainer is not working
        for (int i = 0; i < trainers.size(); i++) {
            for (int minutes = OPEN_HOUR * 60; minutes < CLOSE_HOUR * 60; minutes += 30) {
                LocalTime t = LocalTime.of(minutes / 60, minutes % 60);
                if (!AvailabilityPicker.isAvailable(trainers.get(i).getAvailability(), date, t)) {
                    g2.setColor(new Color(0xF3F4F6));
                    g2.fillRect(LEFT_MARGIN + i * colW, yFor(t), colW, HOUR_HEIGHT / 2);
                }
            }
        }

        // hour lines and labels
        g2.setFont(Theme.regular(11));
        FontMetrics fm = g2.getFontMetrics();
        for (int h = OPEN_HOUR; h <= CLOSE_HOUR; h++) {
            int y = HEADER_HEIGHT + (h - OPEN_HOUR) * HOUR_HEIGHT;
            g2.setColor(new Color(0xE5E7EB));
            g2.drawLine(LEFT_MARGIN, y, gridRight, y);
            g2.setColor(Theme.MUTED);
            String label = String.format("%02d:00", h);
            g2.drawString(label, LEFT_MARGIN - 10 - fm.stringWidth(label), y + 4);
            if (h < CLOSE_HOUR) {
                g2.setColor(new Color(0xF1F2F4));
                g2.drawLine(LEFT_MARGIN, y + HOUR_HEIGHT / 2, gridRight, y + HOUR_HEIGHT / 2);
            }
        }

        // column headers and separators
        for (int i = 0; i < trainers.size(); i++) {
            int x = LEFT_MARGIN + i * colW;
            g2.setColor(new Color(0xE5E7EB));
            g2.drawLine(x, 6, x, gridBottom);
            Trainer t = trainers.get(i);
            g2.setFont(Theme.bold(13));
            g2.setColor(Theme.TEXT);
            g2.drawString(clip(t.getFullName(), g2.getFontMetrics(), colW - 14), x + 8, 26);
            g2.setFont(Theme.regular(11));
            g2.setColor(Theme.MUTED);
            g2.drawString(clip(t.getAvailability(), g2.getFontMetrics(), colW - 14), x + 8, 44);
        }
        g2.setColor(new Color(0xE5E7EB));
        g2.drawLine(gridRight, 6, gridRight, gridBottom);
        g2.drawLine(LEFT_MARGIN, HEADER_HEIGHT, gridRight, HEADER_HEIGHT);

        // session blocks
        for (TrainerSchedule s : sessions) {
            Rectangle r = rectFor(s);
            if (r == null) {
                continue;
            }
            Color fill;
            Color line;
            switch (s.getStatus()) {
                case "COMPLETED":
                    fill = new Color(0xDCFCE7);
                    line = Theme.SUCCESS;
                    break;
                case "CANCELLED":
                    fill = new Color(0xF3F4F6);
                    line = new Color(0x9CA3AF);
                    break;
                default:
                    fill = new Color(0xDBEAFE);
                    line = Theme.INFO;
                    break;
            }
            if (s == hovered) {
                fill = fill.darker();
            }
            g2.setColor(fill);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(line);
            g2.fillRoundRect(r.x, r.y, 4, r.height, 4, 4);
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

            g2.setClip(r);
            g2.setColor("CANCELLED".equals(s.getStatus()) ? Theme.MUTED : Theme.TEXT);
            g2.setFont(Theme.bold(12));
            g2.drawString(clip(s.getMemberName(), g2.getFontMetrics(), r.width - 16), r.x + 10, r.y + 16);
            if (r.height >= 36) {
                g2.setFont(Theme.regular(11));
                g2.setColor(Theme.MUTED);
                g2.drawString(s.getStartTime().format(TIME) + " - " + s.getEndTime().format(TIME)
                        + ("CANCELLED".equals(s.getStatus()) ? "  (cancelled)" : ""), r.x + 10, r.y + 31);
            }
            g2.setClip(null);
        }

        // "now" line
        LocalDateTime now = LocalDateTime.now();
        if (date.equals(now.toLocalDate()) && now.getHour() >= OPEN_HOUR && now.getHour() < CLOSE_HOUR) {
            int y = yFor(now.toLocalTime());
            g2.setColor(Theme.DANGER);
            g2.fillOval(LEFT_MARGIN - 4, y - 4, 8, 8);
            g2.drawLine(LEFT_MARGIN, y, gridRight, y);
        }
        g2.dispose();
    }

    /** Cuts text with "..." so it fits into a width. */
    private static String clip(String text, FontMetrics fm, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && fm.stringWidth(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    // ================================================================ size and scrolling

    @Override
    public Dimension getPreferredSize() {
        int n = Math.max(1, trainers.size());
        return new Dimension(LEFT_MARGIN + n * MIN_COLUMN_WIDTH + 8,
                HEADER_HEIGHT + (CLOSE_HOUR - OPEN_HOUR) * HOUR_HEIGHT + 12);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return HOUR_HEIGHT * 3; }

    /** Fill the window when there is room; scroll sideways only when many trainers do not fit. */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() == null || getPreferredSize().width <= getParent().getWidth();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() { return false; }

    /** Title text such as "Monday, 9 November 2026" for the screen that owns this panel. */
    public static String dayTitle(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));
    }
}
