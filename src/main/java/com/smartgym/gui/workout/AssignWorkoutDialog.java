package com.smartgym.gui.workout;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.WorkoutPlan;
import com.smartgym.service.MemberService;
import com.smartgym.service.SessionManager;
import com.smartgym.service.TrainerService;
import com.smartgym.service.WorkoutService;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.util.List;

/** Give a workout plan to a member, supervised by a trainer. */
public class AssignWorkoutDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    /** The three drop-down lists, loaded together in the background. */
    private static final class Lists {
        List<Member> members;
        List<WorkoutPlan> plans;
        List<Trainer> trainers;
    }

    private final transient MemberService memberService = new MemberService();
    private final transient WorkoutService workoutService = new WorkoutService();
    private final transient TrainerService trainerService = new TrainerService();
    private final transient SessionManager session = SessionManager.getInstance();
    private final transient WorkoutPlan preselectedPlan;
    private final transient Runnable onDone;

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JComboBox<WorkoutPlan> planCombo = new JComboBox<>();
    private final JComboBox<Trainer> trainerCombo = new JComboBox<>();
    private final JCheckBox endCheck = new JCheckBox("Set an end date");
    private final DateSpinner endSpinner = new DateSpinner(LocalDate.now().plusDays(30));

    public AssignWorkoutDialog(Window owner, WorkoutPlan preselectedPlan, Runnable onDone) {
        super(owner, "Assign Workout Plan", "Choose the member, the plan and the supervising trainer", "Assign Plan");
        this.preselectedPlan = preselectedPlan;
        this.onDone = onDone;
        build();
        load();
    }

    @Override
    protected JComponent buildForm() {
        memberCombo.setName("aw.member");
        planCombo.setName("aw.plan");
        trainerCombo.setName("aw.trainer");
        endCheck.setName("aw.endCheck");
        UiFactory.styleCombo(memberCombo);
        UiFactory.styleCombo(planCombo);
        UiFactory.styleCombo(trainerCombo);
        endCheck.setOpaque(false);
        endSpinner.setEnabled(false);
        endCheck.addItemListener(e -> endSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED));

        FormPanel form = new FormPanel();
        form.addField("MEMBER", memberCombo);
        form.addField("WORKOUT PLAN", planCombo);
        form.addField("SUPERVISING TRAINER", trainerCombo);
        form.addComponent(endCheck);
        form.addField("END DATE", endSpinner);
        return form;
    }

    private void load() {
        BackgroundTask.run(this, () -> {
            Lists lists = new Lists();
            lists.members = memberService.getAllMembers();
            lists.plans = workoutService.getAllPlans();
            lists.trainers = trainerService.getAllTrainers();
            return lists;
        }, lists -> {
            for (Member m : lists.members) {
                memberCombo.addItem(m);
            }
            for (WorkoutPlan p : lists.plans) {
                planCombo.addItem(p);
                if (preselectedPlan != null && p.getWorkoutPlanId() == preselectedPlan.getWorkoutPlanId()) {
                    planCombo.setSelectedItem(p);
                }
            }
            for (Trainer t : lists.trainers) {
                // a trainer can only assign under their own name
                boolean mine = session.getRole() != Role.TRAINER
                        || (session.getTrainerId() != null && session.getTrainerId() == t.getTrainerId());
                if (mine) {
                    trainerCombo.addItem(t);
                }
            }
            trainerCombo.setEnabled(session.getRole() != Role.TRAINER);
        }, ex -> showError("Could not load the lists: " + ex.getMessage()));
    }

    @Override
    protected void onSave() {
        final Member member = (Member) memberCombo.getSelectedItem();
        final WorkoutPlan plan = (WorkoutPlan) planCombo.getSelectedItem();
        final Trainer trainer = (Trainer) trainerCombo.getSelectedItem();
        if (member == null || plan == null || trainer == null) {
            showError("Please choose a member, a plan and a trainer.");
            return;
        }
        final LocalDate end = endCheck.isSelected() ? endSpinner.getDate() : null;
        runSave(() -> workoutService.assignToMember(member.getMemberId(), plan.getWorkoutPlanId(),
                trainer.getTrainerId(), end), id -> {
            dispose();
            onDone.run();
        });
    }
}
