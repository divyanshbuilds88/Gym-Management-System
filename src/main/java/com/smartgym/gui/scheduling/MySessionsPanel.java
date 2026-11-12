package com.smartgym.gui.scheduling;

import com.smartgym.exception.InvalidInputException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.service.SchedulingService;
import com.smartgym.service.SessionManager;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** "My Sessions" for a member: the next session at a glance and the full list. Booking is done by staff. */
public class MySessionsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient SchedulingService service = new SchedulingService();
    private final SessionTableModel model = new SessionTableModel(true, false);
    private final JTable table = UiFactory.createTable(model);
    private final JLabel nextLabel = UiFactory.label("...", 22, true, Theme.TEXT);
    private final JLabel nextDetail = UiFactory.muted(" ");

    public MySessionsPanel() {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        RoundedPanel next = new RoundedPanel(new BorderLayout());
        next.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel caption = UiFactory.fieldLabel("YOUR NEXT SESSION");
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        nextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nextDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = UiFactory.muted("To book or change a session, please ask your trainer or the front desk.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(caption);
        box.add(Box.createVerticalStrut(4));
        box.add(nextLabel);
        box.add(Box.createVerticalStrut(2));
        box.add(nextDetail);
        box.add(Box.createVerticalStrut(10));
        box.add(hint);
        next.add(box, BorderLayout.CENTER);
        add(next, BorderLayout.NORTH);

        RoundedPanel listCard = new RoundedPanel(new BorderLayout(0, 10));
        listCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        listCard.add(UiFactory.label("All my sessions", 15, true, Theme.TEXT), BorderLayout.NORTH);
        UiFactory.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(model.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 110, 120, 70, 150, 110, 260);
        listCard.add(UiFactory.scroll(table), BorderLayout.CENTER);
        add(listCard, BorderLayout.CENTER);

        table.setName("mysessions.table");
    }

    @Override
    public void refreshData() {
        final Integer memberId = SessionManager.getInstance().getMemberId();
        BackgroundTask.run(this, () -> {
            if (memberId == null) {
                throw new InvalidInputException("No member profile is linked to this account.");
            }
            return service.getSessionsOfMember(memberId);
        }, list -> {
            model.setSessions(list);
            TrainerSchedule next = null;                           // earliest upcoming SCHEDULED session
            LocalDateTime now = LocalDateTime.now();
            for (TrainerSchedule s : list) {
                LocalDateTime start = LocalDateTime.of(s.getSessionDate(), s.getStartTime());
                if (Constants.SCHEDULED.equals(s.getStatus()) && !start.isBefore(now)
                        && (next == null || start.isBefore(LocalDateTime.of(next.getSessionDate(), next.getStartTime())))) {
                    next = s;
                }
            }
            if (next == null) {
                nextLabel.setText("No upcoming session");
                nextDetail.setText(" ");
            } else {
                nextLabel.setText(next.getSessionDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
                        + "  at  " + next.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                nextDetail.setText("With " + next.getTrainerName() + "  |  " + next.getDurationMinutes() + " minutes"
                        + (next.getNotes() == null || next.getNotes().isEmpty() ? "" : "  |  " + next.getNotes()));
            }
        });
    }
}
