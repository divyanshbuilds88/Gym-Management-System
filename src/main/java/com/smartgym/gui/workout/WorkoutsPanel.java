package com.smartgym.gui.workout;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.model.MemberWorkout;
import com.smartgym.model.Role;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.model.WorkoutPlan;
import com.smartgym.service.SessionManager;
import com.smartgym.service.WorkoutService;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Admin / trainer screen for workouts.
 * Tab 1 "Workout Plans": a master-detail view - the plans on top, the exercises of the selected plan below.
 * Tab 2 "Assignments":   which member follows which plan (a trainer sees only their own members).
 */
public class WorkoutsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient WorkoutService service = new WorkoutService();
    private final transient SessionManager session = SessionManager.getInstance();

    // ---- plans tab
    private final WorkoutPlanTableModel planModel = new WorkoutPlanTableModel();
    private final JTable planTable = UiFactory.createTable(planModel);
    private final ExerciseTableModel exerciseModel = new ExerciseTableModel();
    private final JTable exerciseTable = UiFactory.createTable(exerciseModel);
    private final JLabel exerciseTitle = UiFactory.label("Exercises", 15, true, Theme.TEXT);
    private final RoundedButton newPlanButton = new RoundedButton("+  New Plan", Theme.ACCENT);
    private final RoundedButton editPlanButton = RoundedButton.secondary("Edit");
    private final RoundedButton deletePlanButton = new RoundedButton("Delete", Theme.DANGER);
    private final RoundedButton assignPlanButton = RoundedButton.secondary("Assign to Member");
    private final RoundedButton addExButton = new RoundedButton("+  Add Exercise", Theme.ACCENT);
    private final RoundedButton editExButton = RoundedButton.secondary("Edit");
    private final RoundedButton deleteExButton = new RoundedButton("Delete", Theme.DANGER);
    private int exerciseToken = 0;

    // ---- assignments tab
    private final AssignmentTableModel assignModel = new AssignmentTableModel(true);
    private final JTable assignTable = UiFactory.createTable(assignModel);
    private final transient TableRowSorter<AssignmentTableModel> sorter = new TableRowSorter<>(assignModel);
    private final JTextField filterField = UiFactory.searchField("Filter by member, plan or trainer", 24);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {"All", "Active", "Completed", "Cancelled"});
    private final JLabel assignCount = UiFactory.muted(" ");
    private final RoundedButton assignNewButton = new RoundedButton("+  Assign Plan", Theme.ACCENT);
    private final RoundedButton completeButton = RoundedButton.secondary("Mark Completed");
    private final RoundedButton cancelButton = RoundedButton.secondary("Cancel");
    private final RoundedButton removeButton = new RoundedButton("Remove", Theme.DANGER);

    public WorkoutsPanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bold(13));
        tabs.addTab("Workout Plans", buildPlansTab());
        tabs.addTab(session.getRole() == Role.TRAINER ? "My Members' Plans" : "Assignments", buildAssignmentsTab());
        tabs.setName("wk.tabs");
        add(tabs, BorderLayout.CENTER);

        wireEvents();
        updateButtons();
    }

    // ================================================================ layout

    private JPanel toolbar(java.awt.Component... items) {
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        for (java.awt.Component c : items) {
            bar.add(c);
        }
        return bar;
    }

    private JPanel buildPlansTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // top half: plans
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);
        topHeader.add(UiFactory.label("Workout plans", 15, true, Theme.TEXT), BorderLayout.NORTH);
        topHeader.add(toolbar(newPlanButton, editPlanButton, deletePlanButton, assignPlanButton), BorderLayout.CENTER);
        top.add(topHeader, BorderLayout.NORTH);
        UiFactory.styleTable(planTable);
        planTable.setAutoCreateRowSorter(true);
        planTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.setColumnWidths(planTable, 160, 130, 100, 130, 300);
        top.add(UiFactory.scroll(planTable), BorderLayout.CENTER);

        // bottom half: exercises of the selected plan
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JPanel bottomHeader = new JPanel(new BorderLayout());
        bottomHeader.setOpaque(false);
        bottomHeader.add(exerciseTitle, BorderLayout.NORTH);
        bottomHeader.add(toolbar(addExButton, editExButton, deleteExButton), BorderLayout.CENTER);
        bottom.add(bottomHeader, BorderLayout.NORTH);
        UiFactory.styleTable(exerciseTable);
        exerciseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.setColumnWidths(exerciseTable, 60, 200, 60, 60, 80, 140);
        bottom.add(UiFactory.scroll(exerciseTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.5);                          // both halves grow equally
        split.setBorder(null);
        split.setDividerSize(8);
        split.setOpaque(false);
        tab.add(split, BorderLayout.CENTER);

        planTable.setName("wk.plans");
        exerciseTable.setName("wk.exercises");
        newPlanButton.setName("wk.newPlan");
        editPlanButton.setName("wk.editPlan");
        deletePlanButton.setName("wk.deletePlan");
        assignPlanButton.setName("wk.assignPlan");
        addExButton.setName("wk.addEx");
        editExButton.setName("wk.editEx");
        deleteExButton.setName("wk.deleteEx");
        return tab;
    }

    private JPanel buildAssignmentsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));

        UiFactory.styleCombo(statusCombo);
        tab.add(toolbar(filterField, statusCombo, assignNewButton, completeButton, cancelButton, removeButton),
                BorderLayout.NORTH);

        UiFactory.styleTable(assignTable);
        assignTable.setRowSorter(sorter);
        assignTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assignTable.getColumnModel().getColumn(assignModel.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(assignTable, 150, 170, 140, 110, 110, 110);
        tab.add(UiFactory.scroll(assignTable), BorderLayout.CENTER);
        tab.add(assignCount, BorderLayout.SOUTH);

        assignTable.setName("wk.assignments");
        filterField.setName("wk.filter");
        statusCombo.setName("wk.status");
        assignNewButton.setName("wk.assignNew");
        completeButton.setName("wk.complete");
        cancelButton.setName("wk.cancel");
        removeButton.setName("wk.remove");
        return tab;
    }

    // ================================================================ events

    private void wireEvents() {
        newPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new WorkoutPlanFormDialog(window(), null, WorkoutsPanel.this::refreshData).setVisible(true);
            }
        });
        editPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkoutPlan p = selectedPlan();
                if (p != null) {
                    new WorkoutPlanFormDialog(window(), p, WorkoutsPanel.this::refreshData).setVisible(true);
                }
            }
        });
        deletePlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deletePlan();
            }
        });
        assignPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AssignWorkoutDialog(window(), selectedPlan(), WorkoutsPanel.this::refreshData).setVisible(true);
            }
        });
        addExButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addExercise();
            }
        });
        editExButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editExercise();
            }
        });
        deleteExButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteExercise();
            }
        });

        planTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadExercises();
                updateButtons();
            }
        });
        exerciseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        planTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selectedPlan() != null) {
                    new WorkoutPlanFormDialog(window(), selectedPlan(), WorkoutsPanel.this::refreshData).setVisible(true);
                }
            }
        });

        // assignments tab
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        statusCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyFilter();
            }
        });
        assignNewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AssignWorkoutDialog(window(), null, WorkoutsPanel.this::refreshData).setVisible(true);
            }
        });
        completeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeStatus("COMPLETED");
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeStatus("CANCELLED");
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeAssignment();
            }
        });
        assignTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
    }

    private void updateButtons() {
        boolean plan = selectedPlan() != null;
        editPlanButton.setEnabled(plan);
        deletePlanButton.setEnabled(plan);
        addExButton.setEnabled(plan);
        boolean exercise = selectedExercise() != null;
        editExButton.setEnabled(exercise);
        deleteExButton.setEnabled(exercise);

        MemberWorkout a = selectedAssignment();
        boolean active = a != null && "ACTIVE".equals(a.getStatus());
        completeButton.setEnabled(active);
        cancelButton.setEnabled(active);
        removeButton.setEnabled(a != null);
    }

    private void applyFilter() {
        final String status = ((String) statusCombo.getSelectedItem()).toUpperCase();
        final String text = filterField.getText().trim().toLowerCase();
        sorter.setRowFilter(new RowFilter<AssignmentTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends AssignmentTableModel, ? extends Integer> entry) {
                if (!status.equals("ALL") && !status.equals(entry.getStringValue(5))) {
                    return false;
                }
                return text.isEmpty()
                        || entry.getStringValue(0).toLowerCase().contains(text)
                        || entry.getStringValue(1).toLowerCase().contains(text)
                        || entry.getStringValue(2).toLowerCase().contains(text);
            }
        });
        assignCount.setText("Showing " + assignTable.getRowCount() + " of " + assignModel.getRowCount() + " assignments");
        updateButtons();
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        final int keepPlanId = selectedPlan() == null ? -1 : selectedPlan().getWorkoutPlanId();
        BackgroundTask.run(this, service::getAllPlans, plans -> {
            planModel.setPlans(plans);
            for (int i = 0; i < planModel.getRowCount(); i++) {                 // keep the same plan selected
                if (planModel.getPlan(i).getWorkoutPlanId() == keepPlanId) {
                    int viewRow = planTable.convertRowIndexToView(i);
                    planTable.setRowSelectionInterval(viewRow, viewRow);
                    break;
                }
            }
            if (planTable.getSelectedRow() < 0 && planModel.getRowCount() > 0) {
                planTable.setRowSelectionInterval(0, 0);                       // select the first plan
            }
            loadExercises();
            updateButtons();
        });
        BackgroundTask.run(this, this::loadAssignments, list -> {
            assignModel.setAssignments(list);
            applyFilter();
        });
    }

    /** A trainer sees only their own members' plans; the admin sees everything. */
    private List<MemberWorkout> loadAssignments() throws Exception {
        if (session.getRole() == Role.TRAINER && session.getTrainerId() != null) {
            return service.getAssignmentsOfTrainer(session.getTrainerId());
        }
        return service.getAllAssignments();
    }

    private void loadExercises() {
        final WorkoutPlan plan = selectedPlan();
        final int token = ++exerciseToken;
        if (plan == null) {
            exerciseModel.setExercises(java.util.Collections.<WorkoutExercise>emptyList());
            exerciseTitle.setText("Exercises");
            return;
        }
        exerciseTitle.setText("Exercises in \"" + plan.getPlanName() + "\"");
        BackgroundTask.run(this, () -> service.getExercises(plan.getWorkoutPlanId()), list -> {
            if (token == exerciseToken) {                                          // ignore old answers
                exerciseModel.setExercises(list);
                updateButtons();
            }
        });
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private WorkoutPlan selectedPlan() {
        int row = planTable.getSelectedRow();
        return row < 0 ? null : planModel.getPlan(planTable.convertRowIndexToModel(row));
    }

    private WorkoutExercise selectedExercise() {
        int row = exerciseTable.getSelectedRow();
        return row < 0 ? null : exerciseModel.getExercise(exerciseTable.convertRowIndexToModel(row));
    }

    private MemberWorkout selectedAssignment() {
        int row = assignTable.getSelectedRow();
        return row < 0 ? null : assignModel.getAssignment(assignTable.convertRowIndexToModel(row));
    }

    // ================================================================ actions

    private void deletePlan() {
        final WorkoutPlan plan = selectedPlan();
        if (plan == null) {
            return;
        }
        Object[] options = {"Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Delete the plan \"" + plan.getPlanName() + "\"?\n\nIts exercises are deleted and it is removed from every member who follows it.",
                "Delete workout plan", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice == 0) {
            BackgroundTask.run(this, () -> {
                service.deletePlan(plan.getWorkoutPlanId());
                return Boolean.TRUE;
            }, ok -> refreshData());
        }
    }

    private void addExercise() {
        final WorkoutPlan plan = selectedPlan();
        if (plan == null) {
            return;
        }
        new ExerciseFormDialog(window(), null, ex -> {
            ex.setWorkoutPlanId(plan.getWorkoutPlanId());
            service.addExercise(ex);
        }, saved -> loadExercises()).setVisible(true);
    }

    private void editExercise() {
        WorkoutExercise ex = selectedExercise();
        if (ex != null) {
            new ExerciseFormDialog(window(), ex, service::updateExercise, saved -> loadExercises()).setVisible(true);
        }
    }

    private void deleteExercise() {
        final WorkoutExercise ex = selectedExercise();
        if (ex == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Delete the exercise \"" + ex.getExerciseName() + "\"?",
                "Delete exercise", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            BackgroundTask.run(this, () -> {
                service.deleteExercise(ex.getExerciseId());
                return Boolean.TRUE;
            }, ok -> loadExercises());
        }
    }

    private void changeStatus(final String status) {
        final MemberWorkout a = selectedAssignment();
        if (a == null) {
            return;
        }
        BackgroundTask.run(this, () -> {
            service.updateAssignmentStatus(a.getAssignmentId(), status);
            return Boolean.TRUE;
        }, ok -> refreshData());
    }

    private void removeAssignment() {
        final MemberWorkout a = selectedAssignment();
        if (a == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove \"" + a.getPlanName() + "\" from " + a.getMemberName() + "?",
                "Remove assignment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            BackgroundTask.run(this, () -> {
                service.removeAssignment(a.getAssignmentId());
                return Boolean.TRUE;
            }, ok -> refreshData());
        }
    }
}
