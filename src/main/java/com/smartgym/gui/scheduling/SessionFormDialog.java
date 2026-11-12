package com.smartgym.gui.scheduling;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.service.MemberService;
import com.smartgym.service.SchedulingService;
import com.smartgym.service.SessionManager;
import com.smartgym.service.TrainerService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Book a new training session, or change an existing one.
 * Clashes (the trainer or the member is already booked, the member's plan has expired, the session is
 * in the past ...) come back from the SchedulingService as messages and are shown inside this window.
 */
public class SessionFormDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    private static final class Lists {
        List<Member> members;
        List<Trainer> trainers;
    }

    private final transient SchedulingService schedulingService = new SchedulingService();
    private final transient MemberService memberService = new MemberService();
    private final transient TrainerService trainerService = new TrainerService();
    private final transient SessionManager session = SessionManager.getInstance();
    private final transient TrainerSchedule existing;
    private final transient Trainer preTrainer;
    private final transient Runnable onSaved;

    private final JComboBox<Trainer> trainerCombo = new JComboBox<>();
    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final DateSpinner dateSpinner;
    private final JSpinner timeSpinner = new JSpinner(new SpinnerListModel(times()));
    private final JComboBox<Integer> durationCombo = new JComboBox<>(new Integer[] {30, 45, 60, 90, 120});
    private final JTextField notesField = UiFactory.textField(20);
    private final JLabel summaryLabel = UiFactory.label(" ", 13, true, Theme.TEXT);

    /**
     * @param existing   the session to change, or null to book a new one
     * @param preTrainer trainer to pre-select (for example the column that was double-clicked), may be null
     * @param preDate    date to pre-select, may be null (then tomorrow)
     * @param preTime    start time to pre-select, may be null (then 10:00)
     */
    public SessionFormDialog(Window owner, TrainerSchedule existing, Trainer preTrainer,
                             LocalDate preDate, LocalTime preTime, Runnable onSaved) {
        super(owner,
                existing == null ? "Book Session" : "Edit Session",
                existing == null ? "Book a personal-training session" : "Change the session of " + existing.getMemberName(),
                existing == null ? "Book Session" : "Save Changes");
        this.existing = existing;
        this.preTrainer = preTrainer;
        this.onSaved = onSaved;
        this.dateSpinner = new DateSpinner(existing != null ? existing.getSessionDate()
                : preDate != null ? preDate : LocalDate.now().plusDays(1));
        build();

        LocalTime start = existing != null ? existing.getStartTime() : preTime != null ? preTime : LocalTime.of(10, 0);
        timeSpinner.setValue(String.format("%02d:%02d", start.getHour(), start.getMinute()));
        durationCombo.setSelectedItem(existing != null ? Integer.valueOf(existing.getDurationMinutes()) : Integer.valueOf(60));
        if (existing != null) {
            notesField.setText(existing.getNotes());
        }
        updateSummary();
        load();
    }

    /** 05:00, 05:30 ... 21:30 */
    private static String[] times() {
        List<String> list = new ArrayList<>();
        for (int h = 5; h <= 21; h++) {
            list.add(String.format("%02d:00", h));
            list.add(String.format("%02d:30", h));
        }
        return list.toArray(new String[0]);
    }

    @Override
    protected JComponent buildForm() {
        trainerCombo.setName("ss.trainer");
        memberCombo.setName("ss.member");
        dateSpinner.setName("ss.date");
        timeSpinner.setName("ss.time");
        durationCombo.setName("ss.duration");
        notesField.setName("ss.notes");
        UiFactory.styleCombo(trainerCombo);
        UiFactory.styleCombo(memberCombo);
        UiFactory.styleCombo(durationCombo);
        UiFactory.styleSpinner(timeSpinner);
        durationCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value + " minutes");
                return this;
            }
        });

        summaryLabel.setOpaque(true);
        summaryLabel.setBackground(new Color(0xEFF6FF));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // update the summary whenever date, time or duration changes
        dateSpinner.addChangeListener(e -> updateSummary());
        timeSpinner.addChangeListener(e -> updateSummary());
        durationCombo.addActionListener(e -> updateSummary());

        FormPanel form = new FormPanel();
        form.addField("TRAINER", trainerCombo);
        form.addField("MEMBER", memberCombo);
        form.addFieldPair("DATE", dateSpinner, "START TIME", timeSpinner);
        form.addField("DURATION", durationCombo);
        form.addField("NOTES (optional)", notesField);
        form.addComponent(summaryLabel);
        return form;
    }

    private LocalTime selectedStart() {
        return LocalTime.parse((String) timeSpinner.getValue());
    }

    private void updateSummary() {
        Integer minutes = (Integer) durationCombo.getSelectedItem();
        if (minutes == null) {
            return;
        }
        LocalTime start = selectedStart();
        LocalTime end = start.plusMinutes(minutes);
        boolean pastMidnight = !end.isAfter(start);
        summaryLabel.setText(pastMidnight ? "The session would run past midnight."
                : start + " - " + end + "  on  " + dateSpinner.getDate().format(UiFactory.DATE_FORMAT)
                + (end.isAfter(LocalTime.of(22, 0)) ? "   (the gym closes at 22:00)" : ""));
    }

    private void load() {
        BackgroundTask.run(this, () -> {
            Lists lists = new Lists();
            lists.members = memberService.getAllMembers();
            lists.trainers = trainerService.getAllTrainers();
            return lists;
        }, lists -> {
            for (Trainer t : lists.trainers) {
                boolean mine = session.getRole() != Role.TRAINER
                        || (session.getTrainerId() != null && session.getTrainerId() == t.getTrainerId());
                if (mine) {
                    trainerCombo.addItem(t);
                }
            }
            trainerCombo.setEnabled(session.getRole() != Role.TRAINER);   // a trainer books only for themself
            for (Member m : lists.members) {
                memberCombo.addItem(m);
            }
            // pre-select the trainer / member we were given
            int wantedTrainer = existing != null ? existing.getTrainerId() : preTrainer != null ? preTrainer.getTrainerId() : -1;
            for (int i = 0; i < trainerCombo.getItemCount(); i++) {
                if (trainerCombo.getItemAt(i).getTrainerId() == wantedTrainer) {
                    trainerCombo.setSelectedIndex(i);
                }
            }
            if (existing != null) {
                for (int i = 0; i < memberCombo.getItemCount(); i++) {
                    if (memberCombo.getItemAt(i).getMemberId() == existing.getMemberId()) {
                        memberCombo.setSelectedIndex(i);
                    }
                }
            }
        }, ex -> showError("Could not load the lists: " + ex.getMessage()));
    }

    @Override
    protected void onSave() {
        Trainer trainer = (Trainer) trainerCombo.getSelectedItem();
        Member member = (Member) memberCombo.getSelectedItem();
        if (trainer == null || member == null) {
            showError("Please choose a trainer and a member.");
            return;
        }
        final TrainerSchedule s = new TrainerSchedule();
        if (existing != null) {
            s.setScheduleId(existing.getScheduleId());
        }
        s.setTrainerId(trainer.getTrainerId());
        s.setMemberId(member.getMemberId());
        s.setSessionDate(dateSpinner.getDate());
        s.setStartTime(selectedStart());
        s.setDurationMinutes((Integer) durationCombo.getSelectedItem());
        s.setNotes(notesField.getText().trim());

        runSave(() -> {
            if (existing == null) {
                schedulingService.scheduleSession(s);
            } else {
                schedulingService.updateSession(s);
            }
            return Boolean.TRUE;
        }, ok -> {
            dispose();
            onSaved.run();
        });
    }
}
