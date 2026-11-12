package com.smartgym.gui.dashboard;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.BarChartPanel;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.ScrollPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Member;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.service.DashboardData;
import com.smartgym.service.DashboardService;
import com.smartgym.service.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The dashboard: 6 statistic cards, 2 charts and 2 tables, all loaded from MySQL.
 * The data is loaded in the background (SwingWorker) every time the screen is shown or F5 is pressed.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient DashboardService service = new DashboardService();

    private final StatCard totalMembers = new StatCard("Total Members", Theme.INFO, "members");
    private final StatCard activeMemberships = new StatCard("Active Memberships", Theme.SUCCESS, "check");
    private final StatCard expiredMemberships = new StatCard("Expired Memberships", Theme.DANGER, "membership");
    private final StatCard totalTrainers = new StatCard("Total Trainers", Theme.PURPLE, "trainers");
    private final StatCard todayAttendance = new StatCard("Today's Attendance", Theme.TEAL, "attendance");
    private final StatCard monthlyRevenue = new StatCard("Revenue This Month", Theme.ACCENT, "payments");

    private final BarChartPanel revenueChart = new BarChartPanel("Revenue - last 6 months (Rs.)", Theme.ACCENT, true);
    private final BarChartPanel attendanceChart = new BarChartPanel("Check-ins - last 7 days", Theme.INFO, false);

    private final DefaultTableModel upcomingModel = UiFactory.readOnlyModel("Date", "Time", "Trainer", "Member");
    private final DefaultTableModel recentModel = UiFactory.readOnlyModel("Code", "Name", "Joined", "Plan");

    private final JLabel greeting = UiFactory.label("", 20, true, Theme.TEXT);
    private final JLabel updated = UiFactory.muted(" ");
    private final RoundedButton refreshButton = RoundedButton.secondaryWithIcon("Refresh", "refresh");

    public DashboardPanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        add(buildToolbar(), BorderLayout.NORTH);

        ScrollPanel content = new ScrollPanel(new BorderLayout());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        JPanel cards = new JPanel(new GridLayout(2, 3, 16, 16));
        cards.setOpaque(false);
        cards.add(totalMembers);
        cards.add(activeMemberships);
        cards.add(expiredMemberships);
        cards.add(totalTrainers);
        cards.add(todayAttendance);
        cards.add(monthlyRevenue);
        content.add(fixedHeight(cards, 216));
        content.add(Box.createVerticalStrut(16));

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.add(revenueChart);
        charts.add(attendanceChart);
        content.add(fixedHeight(charts, 290));
        content.add(Box.createVerticalStrut(16));

        JPanel tables = new JPanel(new GridLayout(1, 2, 16, 0));
        tables.setOpaque(false);
        tables.add(tableCard("Upcoming Trainer Sessions", upcomingModel));
        tables.add(tableCard("Recent Registrations", recentModel));
        content.add(fixedHeight(tables, 260));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(greeting);
        left.add(Box.createVerticalStrut(2));
        left.add(updated);
        bar.add(left, BorderLayout.WEST);
        bar.add(refreshButton, BorderLayout.EAST);
        return bar;
    }

    private static JPanel fixedHeight(JPanel panel, int height) {
        panel.setPreferredSize(new Dimension(100, height));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel tableCard(String title, DefaultTableModel model) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(UiFactory.label(title, 15, true, Theme.TEXT), BorderLayout.NORTH);
        JTable table = UiFactory.createTable(model);
        UiFactory.styleTable(table);
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        return card;
    }

    // ================================================================ loading

    @Override
    public void refreshData() {
        String firstName = SessionManager.getInstance().getCurrentUser().getFullName().split(" ")[0];
        greeting.setText(greetingFor(LocalDateTime.now().getHour()) + ", " + firstName);
        refreshButton.setEnabled(false);
        updated.setText("Loading...");

        BackgroundTask.run(this, service::load, data -> {
            display(data);
            refreshButton.setEnabled(true);
        }, ex -> {
            refreshButton.setEnabled(true);
            updated.setText("Could not load data");
            BackgroundTask.showError(this, ex);
        });
    }

    private static String greetingFor(int hour) {
        if (hour < 12) {
            return "Good morning";
        } else if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    private void display(DashboardData d) {
        totalMembers.setValue(String.valueOf(d.getTotalMembers()));
        activeMemberships.setValue(String.valueOf(d.getActiveMemberships()));
        expiredMemberships.setValue(String.valueOf(d.getExpiredMemberships()));
        totalTrainers.setValue(String.valueOf(d.getTotalTrainers()));
        todayAttendance.setValue(String.valueOf(d.getTodayAttendance()));
        monthlyRevenue.setValue(String.format("Rs. %,.0f", d.getMonthlyRevenue()));

        Map<String, Double> revenue = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : d.getRevenueSeries().entrySet()) {
            String month = YearMonth.parse(e.getKey()).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            revenue.put(month, e.getValue());
        }
        revenueChart.setData(revenue);

        Map<String, Double> visits = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Integer> e : d.getAttendanceSeries().entrySet()) {
            visits.put(e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), (double) e.getValue());
        }
        attendanceChart.setData(visits);

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        upcomingModel.setRowCount(0);
        for (TrainerSchedule s : d.getUpcomingSessions()) {
            upcomingModel.addRow(new Object[] {s.getSessionDate().format(dateFormat),
                    s.getStartTime().format(timeFormat), s.getTrainerName(), s.getMemberName()});
        }
        recentModel.setRowCount(0);
        for (Member m : d.getRecentMembers()) {
            recentModel.addRow(new Object[] {m.getMemberCode(), m.getFullName(),
                    m.getJoiningDate().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)),
                    m.getPlanName() == null ? "-" : m.getPlanName()});
        }
        updated.setText("Last updated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + "   (press F5 to reload)");
    }
}
