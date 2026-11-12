package com.smartgym.gui.workout;

import com.smartgym.exception.InvalidInputException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.ScrollPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.MemberWorkout;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.service.SessionManager;
import com.smartgym.service.WorkoutService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;

/** "My Workouts" for a member: the plans their trainer gave them, and a Monday-Sunday view of the selected plan. */
public class MyWorkoutsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient WorkoutService service = new WorkoutService();
    private final AssignmentTableModel model = new AssignmentTableModel(false);
    private final JTable table = UiFactory.createTable(model);
    private final WeekPlanPanel week = new WeekPlanPanel();
    private final JLabel weekTitle = UiFactory.label("Weekly schedule", 15, true, Theme.TEXT);
    private final JLabel emptyLabel = UiFactory.muted(" ");
    private int exerciseToken = 0;

    public MyWorkoutsPanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        ScrollPanel content = new ScrollPanel(new BorderLayout());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        RoundedPanel plansCard = new RoundedPanel(new BorderLayout(0, 10));
        plansCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        plansCard.add(UiFactory.label("My workout plans", 15, true, Theme.TEXT), BorderLayout.NORTH);
        UiFactory.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(model.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 200, 160, 110, 110, 110);
        plansCard.add(UiFactory.scroll(table), BorderLayout.CENTER);
        plansCard.add(emptyLabel, BorderLayout.SOUTH);
        content.add(fixed(plansCard, 230));
        content.add(Box.createVerticalStrut(16));

        RoundedPanel weekCard = new RoundedPanel(new BorderLayout(0, 12));
        weekCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        weekCard.add(weekTitle, BorderLayout.NORTH);
        JScrollPane weekScroll = new JScrollPane(week);          // scrolls if a day has many exercises or the window is narrow
        weekScroll.setBorder(null);
        weekScroll.setOpaque(false);
        weekScroll.getViewport().setOpaque(false);
        weekCard.add(weekScroll, BorderLayout.CENTER);
        content.add(fixed(weekCard, 330));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showWeek();
            }
        });
        table.setName("my.plans");
    }

    private static JPanel fixed(JPanel panel, int height) {
        panel.setPreferredSize(new Dimension(100, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    @Override
    public void refreshData() {
        final Integer memberId = SessionManager.getInstance().getMemberId();
        BackgroundTask.run(this, () -> {
            if (memberId == null) {
                throw new InvalidInputException("No member profile is linked to this account.");
            }
            return service.getAssignmentsOfMember(memberId);
        }, list -> {
            model.setAssignments(list);
            emptyLabel.setText(list.isEmpty() ? "Your trainer has not assigned a workout plan to you yet." : " ");
            int pick = 0;                                         // prefer the first ACTIVE plan
            for (int i = 0; i < model.getRowCount(); i++) {
                if ("ACTIVE".equals(model.getAssignment(i).getStatus())) {
                    pick = i;
                    break;
                }
            }
            if (model.getRowCount() > 0) {
                table.setRowSelectionInterval(pick, pick);
            } else {
                week.clear();
                weekTitle.setText("Weekly schedule");
            }
        });
    }

    private void showWeek() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        final MemberWorkout a = model.getAssignment(table.convertRowIndexToModel(row));
        final int token = ++exerciseToken;
        weekTitle.setText("Weekly schedule - " + a.getPlanName());
        BackgroundTask.run(this, () -> service.getExercises(a.getWorkoutPlanId()), (List<WorkoutExercise> list) -> {
            if (token == exerciseToken) {
                week.setExercises(list.isEmpty() ? Collections.<WorkoutExercise>emptyList() : list);
            }
        });
    }
}
