package com.smartgym.gui.attendance;

import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.dashboard.StatCard;
import com.smartgym.model.Attendance;
import com.smartgym.service.AttendanceService;
import com.smartgym.service.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * "Attendance" for a member: check yourself in and out, see how long you have been inside, and
 * look at your own visit history. A timer refreshes the "time so far" text every 30 seconds.
 */
public class MyAttendancePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private final transient AttendanceService service = new AttendanceService();
    private final AttendanceTableModel model = new AttendanceTableModel(true, false);
    private final JTable table = UiFactory.createTable(model);

    private final JLabel statusTitle = UiFactory.label("...", 22, true, Theme.TEXT);
    private final JLabel statusDetail = UiFactory.muted(" ");
    private final JLabel message = new JLabel(" ");
    private final RoundedButton checkInButton = new RoundedButton("Check In", Theme.ACCENT);
    private final RoundedButton checkOutButton = RoundedButton.secondary("Check Out");

    private final StatCard monthCard = new StatCard("Visits this month", Theme.INFO, "attendance");
    private final StatCard totalCard = new StatCard("Total visits", Theme.PURPLE, "check");
    private final StatCard averageCard = new StatCard("Average stay (min)", Theme.TEAL, "schedule");

    private transient LocalDateTime insideSince;               // null when the member is not inside
    private final Timer clock;

    public MyAttendancePanel() {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(buildStatusCard(), BorderLayout.NORTH);
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.add(monthCard);
        stats.add(totalCard);
        stats.add(averageCard);
        top.add(stats, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        RoundedPanel history = new RoundedPanel(new BorderLayout(0, 10));
        history.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        history.add(UiFactory.label("My visits", 15, true, Theme.TEXT), BorderLayout.NORTH);
        UiFactory.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(model.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 120, 100, 110, 110, 100);
        history.add(UiFactory.scroll(table), BorderLayout.CENTER);
        add(history, BorderLayout.CENTER);

        clock = new Timer(30_000, e -> updateStatusText());        // fires on the Swing thread every 30 s
        clock.start();

        checkInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                act(true);
            }
        });
        checkOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                act(false);
            }
        });
        checkInButton.setName("mya.checkin");
        checkOutButton.setName("mya.checkout");
        statusTitle.setName("mya.status");
        message.setName("mya.message");
        table.setName("mya.table");
    }

    private JPanel buildStatusCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(20, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel caption = UiFactory.fieldLabel("TODAY AT THE GYM");
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setFont(Theme.bold(13));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(caption);
        text.add(Box.createVerticalStrut(4));
        text.add(statusTitle);
        text.add(Box.createVerticalStrut(2));
        text.add(statusDetail);
        text.add(Box.createVerticalStrut(8));
        text.add(message);
        card.add(text, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(checkInButton);
        buttons.add(checkOutButton);
        card.add(buttons, BorderLayout.EAST);
        return card;
    }

    @Override
    public void refreshData() {
        final Integer memberId = SessionManager.getInstance().getMemberId();
        BackgroundTask.run(this, () -> {
            if (memberId == null) {
                throw new InvalidInputException("No member profile is linked to this account.");
            }
            return service.getHistory(memberId);
        }, this::display);
    }

    private void display(List<Attendance> visits) {
        model.setVisits(visits);

        insideSince = null;
        int thisMonth = 0;
        long totalMinutes = 0;
        int finished = 0;
        LocalDate today = LocalDate.now();
        for (Attendance a : visits) {
            if (a.getAttendanceDate().getYear() == today.getYear() && a.getAttendanceDate().getMonth() == today.getMonth()) {
                thisMonth++;
            }
            if (a.isCheckedOut()) {
                totalMinutes += a.getDurationMinutes();
                finished++;
            } else {
                insideSince = a.getCheckInTime();
            }
        }
        monthCard.setValue(String.valueOf(thisMonth));
        totalCard.setValue(String.valueOf(visits.size()));
        averageCard.setValue(finished == 0 ? "-" : String.valueOf(totalMinutes / finished));

        checkInButton.setEnabled(insideSince == null);
        checkOutButton.setEnabled(insideSince != null);
        updateStatusText();
    }

    private void updateStatusText() {
        if (insideSince == null) {
            statusTitle.setText("You are not checked in");
            statusDetail.setText("Press Check In when you arrive at the gym.");
        } else {
            long minutes = Duration.between(insideSince, LocalDateTime.now()).toMinutes();
            statusTitle.setText("You are checked in");
            statusDetail.setText("Since " + insideSince.format(HOUR_MINUTE) + "  |  "
                    + (minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + " minutes") + " so far");
        }
    }

    private void act(final boolean checkIn) {
        final Integer memberId = SessionManager.getInstance().getMemberId();
        checkInButton.setEnabled(false);
        checkOutButton.setEnabled(false);
        message.setText(" ");
        BackgroundTask.run(this, () -> {
            if (memberId == null) {
                throw new InvalidInputException("No member profile is linked to this account.");
            }
            return checkIn ? service.checkIn(memberId) : service.checkOut(memberId);
        }, visit -> {
            message.setForeground(new Color(0x166534));
            message.setText(checkIn ? "\u2714  Checked in at " + visit.getCheckInTime().format(HOUR_MINUTE)
                    : "\u2714  Checked out at " + visit.getCheckOutTime().format(HOUR_MINUTE)
                    + "  -  stayed " + AttendanceTableModel.formatDuration(visit));
            refreshData();
        }, ex -> {
            message.setForeground(new Color(0x991B1B));
            message.setText("<html>\u2716  " + (ex instanceof SmartGymException ? ex.getMessage()
                    : "Unexpected error: " + ex.getMessage()) + "</html>");
            refreshData();                                           // restores the correct button states
        });
    }
}
