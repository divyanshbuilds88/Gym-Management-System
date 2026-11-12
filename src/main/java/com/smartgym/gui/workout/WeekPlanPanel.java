package com.smartgym.gui.workout;

import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;

/**
 * A Monday-to-Sunday overview of a workout plan: one column per day, listing that day's exercises.
 * Days without exercises show "Rest day". Built with GridLayout (7 equal columns).
 */
public class WeekPlanPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel[] dayBodies = new JPanel[Constants.DAYS.length];

    public WeekPlanPanel() {
        super(new GridLayout(1, Constants.DAYS.length, 10, 0));
        setOpaque(false);
        for (int i = 0; i < Constants.DAYS.length; i++) {
            RoundedPanel column = new RoundedPanel(new BorderLayout(0, 6), new Color(0xF9FAFB), 12);
            column.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel header = UiFactory.label(Constants.DAYS[i], 13, true, Theme.ACCENT_DARK);
            column.add(header, BorderLayout.NORTH);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            dayBodies[i] = body;
            column.add(body, BorderLayout.CENTER);
            add(column);
        }
        clear();
    }

    /** Empties all seven columns (shows "Rest day"). */
    public final void clear() {
        setExercises(java.util.Collections.<WorkoutExercise>emptyList());
    }

    public void setExercises(List<WorkoutExercise> exercises) {
        for (int i = 0; i < Constants.DAYS.length; i++) {
            JPanel body = dayBodies[i];
            body.removeAll();
            boolean any = false;
            for (WorkoutExercise e : exercises) {
                if (Constants.DAYS[i].equals(e.getScheduleDay())) {
                    any = true;
                    JLabel l = new JLabel("<html><div style='width:100px'><b>" + escape(e.getExerciseName()) + "</b><br>"
                            + e.getSets() + " x " + e.getRepetitions() + "  |  " + e.getDurationMinutes() + " min<br>"
                            + "<font color='#6B7280'>" + escape(e.getTargetMuscleGroup()) + "</font></div></html>");
                    l.setFont(Theme.regular(12));
                    l.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
                    l.setAlignmentX(LEFT_ALIGNMENT);
                    body.add(l);
                }
            }
            if (!any) {
                JLabel rest = UiFactory.label("Rest day", 12, false, new Color(0x9CA3AF));
                rest.setAlignmentX(LEFT_ALIGNMENT);
                body.add(rest);
            }
            body.revalidate();
            body.repaint();
        }
    }

    /** Stops exercise names from being read as HTML tags. */
    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
