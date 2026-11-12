package com.smartgym.gui.trainers;

import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets the user choose working days and hours, e.g.  [Mon] to [Sat]   [06:00] to [14:00].
 * getText() turns the choice into the text stored in the database: "Mon-Sat 06:00-14:00".
 */
public class AvailabilityPicker extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final Pattern FORMAT = Pattern.compile("^(\\w{3})-(\\w{3}) (\\d{2}:\\d{2})-(\\d{2}:\\d{2})$");

    private final JComboBox<String> fromDay = new JComboBox<>(DAYS);
    private final JComboBox<String> toDay = new JComboBox<>(DAYS);
    private final JSpinner fromTime = new JSpinner(new SpinnerListModel(times()));
    private final JSpinner toTime = new JSpinner(new SpinnerListModel(times()));

    public AvailabilityPicker() {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        UiFactory.styleCombo(fromDay);
        UiFactory.styleCombo(toDay);
        fromDay.setPreferredSize(new java.awt.Dimension(80, 38));
        toDay.setPreferredSize(new java.awt.Dimension(80, 38));
        UiFactory.styleSpinner(fromTime);
        UiFactory.styleSpinner(toTime);
        fromTime.setPreferredSize(new java.awt.Dimension(88, 38));
        toTime.setPreferredSize(new java.awt.Dimension(88, 38));
        fromDay.setName("avail.fromDay");
        toDay.setName("avail.toDay");
        fromTime.setName("avail.fromTime");
        toTime.setName("avail.toTime");

        add(fromDay);
        add(label("to"));
        add(toDay);
        add(label("   from"));
        add(fromTime);
        add(label("to"));
        add(toTime);
        setText("Mon-Sat 06:00-14:00");                         // sensible default
    }

    private static JLabel label(String text) {
        return UiFactory.label(text, 13, false, Theme.MUTED);
    }

    /** 04:00, 04:30, ... 23:30 - built with a loop. */
    private static String[] times() {
        List<String> list = new ArrayList<>();
        for (int h = 4; h <= 23; h++) {
            list.add(String.format("%02d:00", h));
            list.add(String.format("%02d:30", h));
        }
        return list.toArray(new String[0]);
    }

    /**
     * Is a trainer with this stored availability text ("Mon-Sat 06:00-14:00") working on that date at that time?
     * Unknown text formats count as "always available", so old or hand-typed data never breaks the screen.
     */
    public static boolean isAvailable(String text, LocalDate date, LocalTime time) {
        if (text == null) {
            return true;
        }
        Matcher m = FORMAT.matcher(text.trim());
        if (!m.matches()) {
            return true;
        }
        int from = dayIndex(m.group(1));
        int to = dayIndex(m.group(2));
        if (from < 0 || to < 0) {
            return true;
        }
        int day = date.getDayOfWeek().getValue() - 1;                 // Monday = 0
        boolean dayOk = from <= to ? (day >= from && day <= to) : (day >= from || day <= to);   // "Sat-Mon" wraps around
        if (!dayOk) {
            return false;
        }
        LocalTime start = LocalTime.parse(m.group(3));
        LocalTime end = LocalTime.parse(m.group(4));
        return !time.isBefore(start) && time.isBefore(end);
    }

    private static int dayIndex(String name) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public String getText() {
        return fromDay.getSelectedItem() + "-" + toDay.getSelectedItem() + " "
                + fromTime.getValue() + "-" + toTime.getValue();
    }

    /** The closing time must be later than the opening time (zero-padded times compare correctly as text). */
    public boolean isValidRange() {
        return ((String) toTime.getValue()).compareTo((String) fromTime.getValue()) > 0;
    }

    /** Fills the pickers from stored text such as "Mon-Sat 06:00-14:00"; unknown text keeps the defaults. */
    public final void setText(String text) {
        if (text == null) {
            return;
        }
        Matcher m = FORMAT.matcher(text.trim());
        if (m.matches()) {
            fromDay.setSelectedItem(m.group(1));
            toDay.setSelectedItem(m.group(2));
            try {
                fromTime.setValue(m.group(3));
                toTime.setValue(m.group(4));
            } catch (IllegalArgumentException e) {
                // a time that is not in the list (for example 03:15) - keep the defaults
            }
        }
    }
}
