package com.smartgym.gui.attendance;

import com.smartgym.exception.InvalidMemberException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.LiveSearch;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.model.Attendance;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.service.AttendanceService;
import com.smartgym.service.MemberService;
import com.smartgym.service.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Attendance for admins and trainers.
 *   - "Check-in desk" (admin only): type a member code or name, press Enter to check in.
 *     The result appears in a coloured banner so the front desk never has to close dialogs.
 *   - "Daily attendance": who came on a chosen day, and who is still inside.
 *   - "History": search a member across all dates (empty search = the last 30 days).
 */
public class AttendancePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private final transient AttendanceService attendance = new AttendanceService();
    private final transient MemberService memberService = new MemberService();
    private final boolean isAdmin = SessionManager.getInstance().getRole() == Role.ADMIN;

    // ---- check-in desk
    private final JTextField deskField = UiFactory.searchField("Member code (e.g. GYM0004) or name", 26);
    private final RoundedButton checkInButton = new RoundedButton("Check In", Theme.ACCENT);
    private final RoundedButton checkOutButton = RoundedButton.secondary("Check Out");
    private final JLabel banner = new JLabel(" ");
    private final Timer bannerTimer;

    // ---- daily tab
    private final AttendanceTableModel dailyModel = new AttendanceTableModel(false, true);
    private final JTable dailyTable = UiFactory.createTable(dailyModel);
    private final DateSpinner daySpinner = new DateSpinner(LocalDate.now());
    private final JLabel dailyCount = UiFactory.label(" ", 14, true, Theme.TEXT);
    private final RoundedButton prevDay = RoundedButton.secondary("\u2039");
    private final RoundedButton nextDay = RoundedButton.secondary("\u203A");
    private final RoundedButton todayButton = RoundedButton.secondary("Today");
    private int dailyToken = 0;

    // ---- history tab
    private final AttendanceTableModel historyModel = new AttendanceTableModel(true, true);
    private final JTable historyTable = UiFactory.createTable(historyModel);
    private final JTextField historySearch = UiFactory.searchField("Search by member name or code", 28);
    private final JLabel historyCount = UiFactory.muted(" ");
    private final transient LiveSearch liveSearch;

    public AttendancePanel() {
        super(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        if (isAdmin) {
            add(buildDeskCard(), BorderLayout.NORTH);
        }
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bold(13));
        tabs.addTab("Daily attendance", buildDailyTab());
        tabs.addTab("History", buildHistoryTab());
        tabs.setName("att.tabs");
        add(tabs, BorderLayout.CENTER);

        bannerTimer = new Timer(9000, e -> clearBanner());         // the banner fades after 9 seconds
        bannerTimer.setRepeats(false);
        liveSearch = new LiveSearch(historySearch, 300, this::loadHistory);
        wireEvents();
    }

    // ================================================================ layout

    private JPanel buildDeskCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiFactory.label("Check-in desk", 16, true, Theme.TEXT), BorderLayout.NORTH);
        head.add(UiFactory.muted("Type a member code or name and press Enter to check in. Use Check Out when the member leaves."),
                BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(deskField);
        row.add(checkInButton);
        row.add(checkOutButton);
        card.add(row, BorderLayout.CENTER);

        banner.setFont(Theme.bold(14));
        banner.setOpaque(true);
        banner.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        banner.setVisible(false);
        card.add(banner, BorderLayout.SOUTH);

        deskField.setName("att.desk");
        checkInButton.setName("att.checkin");
        checkOutButton.setName("att.checkout");
        banner.setName("att.banner");
        return card;
    }

    private JPanel buildDailyTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));

        prevDay.setPreferredSize(new java.awt.Dimension(44, 38));
        nextDay.setPreferredSize(new java.awt.Dimension(44, 38));
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(prevDay);
        bar.add(daySpinner);
        bar.add(nextDay);
        bar.add(todayButton);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(bar, BorderLayout.NORTH);
        dailyCount.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 0));
        top.add(dailyCount, BorderLayout.CENTER);
        tab.add(top, BorderLayout.NORTH);

        UiFactory.styleTable(dailyTable);
        dailyTable.setAutoCreateRowSorter(true);
        dailyTable.getColumnModel().getColumn(dailyModel.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(dailyTable, 90, 190, 90, 110, 100, 110);
        tab.add(UiFactory.scroll(dailyTable), BorderLayout.CENTER);

        dailyTable.setName("att.daily");
        daySpinner.setName("att.date");
        prevDay.setName("att.prev");
        nextDay.setName("att.next");
        todayButton.setName("att.today");
        return tab;
    }

    private JPanel buildHistoryTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));

        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(historySearch);
        tab.add(bar, BorderLayout.NORTH);

        UiFactory.styleTable(historyTable);
        historyTable.setAutoCreateRowSorter(true);
        historyTable.getColumnModel().getColumn(historyModel.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(historyTable, 110, 90, 190, 90, 110, 100, 110);
        tab.add(UiFactory.scroll(historyTable), BorderLayout.CENTER);
        tab.add(historyCount, BorderLayout.SOUTH);

        historySearch.setName("att.historySearch");
        historyTable.setName("att.history");
        return tab;
    }

    // ================================================================ events

    private void wireEvents() {
        checkInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deskAction(true);
            }
        });
        checkOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deskAction(false);
            }
        });
        // Enter in the box = check in (the most common action at a front desk)
        deskField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    deskAction(true);
                }
            }
        });
        daySpinner.addChangeListener(e -> loadDaily());
        prevDay.addActionListener(e -> daySpinner.setDate(daySpinner.getDate().minusDays(1)));
        nextDay.addActionListener(e -> daySpinner.setDate(daySpinner.getDate().plusDays(1)));
        todayButton.addActionListener(e -> daySpinner.setDate(LocalDate.now()));
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        loadDaily();
        loadHistory();
        if (isAdmin) {
            deskField.requestFocusInWindow();
        }
    }

    private void loadDaily() {
        final LocalDate date = daySpinner.getDate();
        final int token = ++dailyToken;
        BackgroundTask.run(this, () -> attendance.getAttendanceOn(date), list -> {
            if (token != dailyToken) {
                return;
            }
            dailyModel.setVisits(list);
            long inside = list.stream().filter(a -> !a.isCheckedOut()).count();
            dailyCount.setText(list.size() + (list.size() == 1 ? " visit" : " visits")
                    + (inside > 0 ? "   |   " + inside + " inside now" : ""));
        });
    }

    private void loadHistory() {
        final String keyword = historySearch.getText().trim();
        final int token = liveSearch.nextToken();
        BackgroundTask.run(this, () -> keyword.isEmpty()
                ? attendance.getAttendanceBetween(LocalDate.now().minusDays(30), LocalDate.now())
                : attendance.searchByMember(keyword), (List<Attendance> list) -> {
            if (!liveSearch.isCurrent(token)) {
                return;
            }
            historyModel.setVisits(list);
            historyCount.setText(keyword.isEmpty()
                    ? list.size() + " visits in the last 30 days (type a name or code to search all dates)"
                    : list.size() + " visits found for \"" + keyword + "\"");
        });
    }

    // ================================================================ check-in desk

    /** Finds ONE member from what was typed: an exact member code wins, otherwise the text must match exactly one person. */
    private Member resolve(String text) throws SmartGymException {
        List<Member> found = memberService.searchMembers(text);
        if (found.isEmpty()) {
            throw new InvalidMemberException("No member matches \"" + text + "\".");
        }
        for (Member m : found) {
            if (m.getMemberCode().equalsIgnoreCase(text.trim())) {
                return m;
            }
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        throw new InvalidMemberException(found.size() + " members match \"" + text
                + "\". Type the full member code, for example " + found.get(0).getMemberCode() + ".");
    }

    private void deskAction(final boolean checkIn) {
        final String text = deskField.getText().trim();
        if (text.isEmpty()) {
            showBanner("Type a member code or name first.", false);
            return;
        }
        setDeskBusy(true);
        BackgroundTask.run(this, () -> {
            Member m = resolve(text);
            String who = m.getFullName() + " (" + m.getMemberCode() + ")";
            if (checkIn) {
                Attendance a = attendance.checkIn(m.getMemberId());
                return "\u2714  " + who + " checked in at " + a.getCheckInTime().format(HOUR_MINUTE);
            }
            Attendance a = attendance.checkOut(m.getMemberId());
            return "\u2714  " + who + " checked out at " + a.getCheckOutTime().format(HOUR_MINUTE)
                    + "  -  stayed " + AttendanceTableModel.formatDuration(a);
        }, message -> {
            setDeskBusy(false);
            showBanner(message, true);
            deskField.setText("");
            deskField.requestFocusInWindow();
            daySpinner.setDate(LocalDate.now());
            loadDaily();
            loadHistory();
        }, ex -> {
            setDeskBusy(false);
            showBanner("\u2716  " + (ex instanceof SmartGymException ? ex.getMessage() : "Unexpected error: " + ex.getMessage()), false);
            deskField.requestFocusInWindow();
        });
    }

    private void setDeskBusy(boolean busy) {
        checkInButton.setEnabled(!busy);
        checkOutButton.setEnabled(!busy);
    }

    private void showBanner(String text, boolean success) {
        banner.setText("<html><body style='width:640px'>" + text + "</body></html>");
        banner.setForeground(success ? new Color(0x166534) : new Color(0x991B1B));
        banner.setBackground(success ? new Color(0xDCFCE7) : new Color(0xFEE2E2));
        banner.setVisible(true);
        bannerTimer.restart();
        revalidate();
    }

    private void clearBanner() {
        banner.setVisible(false);
        revalidate();
        repaint();
    }
}
