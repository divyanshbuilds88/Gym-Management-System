package com.smartgym.gui.membership;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.ScrollPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.ThinProgressBar;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Membership;
import com.smartgym.model.MembershipPlan;
import com.smartgym.service.MembershipService;
import com.smartgym.service.SessionManager;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** "My Membership": what a member sees - the current plan, time left, history, and the price list. */
public class MyMembershipPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    /** Everything this screen needs, loaded in one background task. */
    private static final class Data {
        List<Membership> mine;
        List<MembershipPlan> plans;
    }

    private final transient MembershipService service = new MembershipService();

    private final JLabel planLabel = UiFactory.label("...", 26, true, Theme.TEXT);
    private final JLabel statusLabel = UiFactory.label(" ", 13, true, Theme.MUTED);
    private final JLabel validLabel = UiFactory.muted(" ");
    private final JLabel daysLabel = UiFactory.label(" ", 15, true, Theme.TEXT);
    private final ThinProgressBar progress = new ThinProgressBar();

    private final MembershipTableModel historyModel = new MembershipTableModel(false);
    private final PlanTableModel plansModel = new PlanTableModel(false);

    public MyMembershipPanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        ScrollPanel content = new ScrollPanel(new BorderLayout());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        content.add(fixed(buildCurrentCard(), 236));
        content.add(Box.createVerticalStrut(16));
        content.add(fixed(tableCard("My membership history", historyModel, true), 230));
        content.add(Box.createVerticalStrut(16));
        content.add(fixed(tableCard("Plans we offer", plansModel, false), 240));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private static JPanel fixed(JPanel panel, int height) {
        panel.setPreferredSize(new Dimension(100, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel buildCurrentCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel caption = UiFactory.fieldLabel("CURRENT MEMBERSHIP");
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        planLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        validLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        daysLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = UiFactory.muted("To renew or change your plan, please visit the front desk.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        box.add(caption);
        box.add(Box.createVerticalStrut(4));
        box.add(planLabel);
        box.add(Box.createVerticalStrut(2));
        box.add(statusLabel);
        box.add(Box.createVerticalStrut(10));
        box.add(validLabel);
        box.add(Box.createVerticalStrut(8));
        box.add(progress);
        box.add(Box.createVerticalStrut(8));
        box.add(daysLabel);
        box.add(Box.createVerticalGlue());
        box.add(hint);
        card.add(box, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel tableCard(String title, javax.swing.table.AbstractTableModel model, boolean history) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(UiFactory.label(title, 15, true, Theme.TEXT), BorderLayout.NORTH);
        JTable table = UiFactory.createTable(model);
        UiFactory.styleTable(table);
        if (history) {
            table.getColumnModel().getColumn(historyModel.statusColumn()).setCellRenderer(new StatusCellRenderer());
        }
        if (!history) {
            UiFactory.setColumnWidths(table, 130, 80, 110, 420);
        }
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refreshData() {
        final Integer memberId = SessionManager.getInstance().getMemberId();
        BackgroundTask.run(this, () -> {
            if (memberId == null) {
                throw new com.smartgym.exception.InvalidInputException("No member profile is linked to this account.");
            }
            Data d = new Data();
            d.mine = service.getMembershipsOfMember(memberId);
            d.plans = service.getActivePlans();
            return d;
        }, this::display);
    }

    private void display(Data d) {
        historyModel.setMemberships(d.mine);
        plansModel.setPlans(d.plans);

        Membership current = null;                       // the active membership that ends last
        for (Membership ms : d.mine) {
            if (Constants.ACTIVE.equals(ms.getStatus())
                    && (current == null || ms.getExpiryDate().isAfter(current.getExpiryDate()))) {
                current = ms;
            }
        }
        if (current == null) {
            planLabel.setText(d.mine.isEmpty() ? "No membership yet" : "Membership expired");
            statusLabel.setForeground(Theme.DANGER);
            statusLabel.setText(d.mine.isEmpty() ? "\u25CF  NOT ACTIVE" : "\u25CF  EXPIRED on "
                    + d.mine.get(0).getExpiryDate().format(UiFactory.DATE_FORMAT));
            validLabel.setText(" ");
            daysLabel.setText(" ");
            progress.setProgress(0, Theme.DANGER);
            return;
        }
        long total = Math.max(1, ChronoUnit.DAYS.between(current.getStartDate(), current.getExpiryDate()));
        long used = ChronoUnit.DAYS.between(current.getStartDate(), LocalDate.now());
        long left = current.getDaysLeft();

        planLabel.setText(current.getPlanName() + " plan");
        statusLabel.setForeground(Theme.SUCCESS);
        statusLabel.setText("\u25CF  ACTIVE");
        validLabel.setText("Valid " + current.getStartDate().format(UiFactory.DATE_FORMAT)
                + "  to  " + current.getExpiryDate().format(UiFactory.DATE_FORMAT));
        daysLabel.setText(left + (left == 1 ? " day left" : " days left"));
        Color barColor = left <= 7 ? Theme.AMBER : Theme.ACCENT;       // warn in the last week
        progress.setProgress((double) used / total, barColor);
    }
}
