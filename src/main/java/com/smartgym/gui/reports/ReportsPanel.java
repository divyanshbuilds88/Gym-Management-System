package com.smartgym.gui.reports;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.service.MemberService;
import com.smartgym.service.ReportService;
import com.smartgym.service.ReportTable;
import com.smartgym.service.SessionManager;
import com.smartgym.service.TrainerService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reports screen: pick one of the six reports on the left, adjust its options (date, months, trainer, member),
 * see the result in a table and export it to a CSV file that opens in Excel.
 * Trainers see four reports; the revenue and payment-history reports are for admins only.
 */
public class ReportsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    /** One entry in the list of reports. */
    private static final class ReportItem {
        final String title;
        final String description;
        final String optionCard;                                        // which options to show: none/date/months/trainer/member
        final boolean adminOnly;
        /** Reads the option controls (on the Swing thread) and returns the work to run in the background. */
        final Supplier<BackgroundTask.Work<ReportTable>> factory;

        ReportItem(String title, String description, String optionCard, boolean adminOnly,
                   Supplier<BackgroundTask.Work<ReportTable>> factory) {
            this.title = title;
            this.description = description;
            this.optionCard = optionCard;
            this.adminOnly = adminOnly;
            this.factory = factory;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /** Trainers and members for the option drop-downs, loaded together. */
    private static final class Lists {
        List<Trainer> trainers;
        List<Member> members;
    }

    private final transient ReportService reportService = new ReportService();
    private final transient MemberService memberService = new MemberService();
    private final transient TrainerService trainerService = new TrainerService();
    private final boolean isAdmin = SessionManager.getInstance().getRole() == Role.ADMIN;

    private final DefaultListModel<ReportItem> listModel = new DefaultListModel<>();
    private final JList<ReportItem> list = new JList<>(listModel);

    private final JLabel titleLabel = UiFactory.label(" ", 20, true, Theme.TEXT);
    private final JLabel descriptionLabel = UiFactory.muted(" ");
    private final CardLayout optionLayout = new CardLayout();
    private final JPanel optionPanel = new JPanel(optionLayout);

    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JSpinner monthsSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 24, 1));
    private final JComboBox<Object> trainerCombo = new JComboBox<>();
    private final JComboBox<Member> memberCombo = new JComboBox<>();

    private final DefaultTableModel tableModel = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = UiFactory.createTable(tableModel);
    private final JLabel countLabel = UiFactory.muted(" ");
    private final RoundedButton exportButton = new RoundedButton("Export to CSV", Theme.ACCENT);
    private final RoundedButton runButton = RoundedButton.secondaryWithIcon("Refresh", "refresh");

    private transient ReportTable current;                                // the report that is on screen
    private int token = 0;
    private boolean filling = false;                                      // true while drop-downs are refilled

    public ReportsPanel() {
        super(new BorderLayout(16, 0));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        buildReportList();
        add(buildListCard(), BorderLayout.WEST);
        add(buildResultCard(), BorderLayout.CENTER);

        wireEvents();
        exportButton.setEnabled(false);
        list.setSelectedIndex(0);
    }

    // ================================================================ the six reports

    private void buildReportList() {
        listModel.addElement(new ReportItem("Member Summary",
                "Total members, active and expired memberships, trainers, today's attendance.", "none", false,
                () -> reportService::memberSummaryReport));

        listModel.addElement(new ReportItem("Membership Status",
                "Every membership with its start, expiry and status (active, expired, cancelled).", "none", false,
                () -> reportService::membershipStatusReport));

        listModel.addElement(new ReportItem("Daily Attendance",
                "Who visited the gym on the chosen day, and for how long.", "date", false, () -> {
                    final LocalDate date = dateSpinner.getDate();
                    return () -> reportService.dailyAttendanceReport(date);
                }));

        if (isAdmin) {
            listModel.addElement(new ReportItem("Monthly Revenue",
                    "Paid payments per month for the last few months (calculated by a stored procedure).", "months", true, () -> {
                        final int months = (Integer) monthsSpinner.getValue();
                        return () -> reportService.monthlyRevenueReport(months);
                    }));
        }

        listModel.addElement(new ReportItem("Trainer Schedules",
                "All training sessions, for every trainer or for one trainer.", "trainer", false, () -> {
                    Object choice = trainerCombo.getSelectedItem();
                    final Integer trainerId = choice instanceof Trainer ? ((Trainer) choice).getTrainerId() : null;
                    return () -> reportService.trainerScheduleReport(trainerId);
                }));

        if (isAdmin) {
            listModel.addElement(new ReportItem("Member Payment History",
                    "All payments of one member with receipt numbers and status.", "member", true, () -> {
                        Member member = (Member) memberCombo.getSelectedItem();
                        if (member == null) {
                            return () -> new ReportTable("Payment History (choose a member)", "Info", "Message");
                        }
                        final int id = member.getMemberId();
                        return () -> reportService.memberPaymentHistoryReport(id);
                    }));
        }
    }

    // ================================================================ layout

    private JPanel buildListCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 14, 16, 14));
        card.setPreferredSize(new Dimension(300, 100));
        card.add(UiFactory.label("Reports", 16, true, Theme.TEXT), BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ReportCellRenderer());
        list.setFixedCellHeight(62);
        list.setBackground(Color.WHITE);
        list.setName("rep.list");
        javax.swing.JScrollPane listScroll = UiFactory.scroll(list);
        listScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(listScroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildResultCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        // heading + options + buttons
        JPanel head = new JPanel(new BorderLayout(0, 8));
        head.setOpaque(false);
        JPanel texts = new JPanel(new BorderLayout(0, 2));
        texts.setOpaque(false);
        texts.add(titleLabel, BorderLayout.NORTH);
        texts.add(descriptionLabel, BorderLayout.CENTER);
        head.add(texts, BorderLayout.NORTH);

        UiFactory.styleSpinner(monthsSpinner);
        UiFactory.styleCombo(trainerCombo);
        UiFactory.styleCombo(memberCombo);
        trainerCombo.setPreferredSize(new Dimension(260, 38));
        memberCombo.setPreferredSize(new Dimension(260, 38));
        optionPanel.setOpaque(false);
        optionPanel.add(new JPanel(), "none");
        optionPanel.add(option("Date:", dateSpinner), "date");
        optionPanel.add(option("Months:", monthsSpinner), "months");
        optionPanel.add(option("Trainer:", trainerCombo), "trainer");
        optionPanel.add(option("Member:", memberCombo), "member");
        optionPanel.setPreferredSize(new Dimension(100, 46));

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(optionPanel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(runButton);
        buttons.add(exportButton);
        row.add(buttons, BorderLayout.EAST);
        head.add(row, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        UiFactory.styleTable(table);
        table.setAutoCreateRowSorter(true);
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        card.add(countLabel, BorderLayout.SOUTH);

        dateSpinner.setName("rep.date");
        monthsSpinner.setName("rep.months");
        trainerCombo.setName("rep.trainer");
        memberCombo.setName("rep.member");
        table.setName("rep.table");
        exportButton.setName("rep.export");
        runButton.setName("rep.run");
        titleLabel.setName("rep.title");
        return card;
    }

    private static JPanel option(String caption, Component control) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.add(UiFactory.label(caption, 13, true, Theme.MUTED));
        p.add(control);
        return p;
    }

    // ================================================================ events

    private void wireEvents() {
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                generate();
            }
        });
        runButton.addActionListener(e -> generate());
        exportButton.addActionListener(e -> exportCsv());
        // changing an option regenerates the report
        dateSpinner.addChangeListener(e -> generate());
        monthsSpinner.addChangeListener(e -> generate());
        trainerCombo.addActionListener(e -> {
            if (!filling) {
                generate();
            }
        });
        memberCombo.addActionListener(e -> {
            if (!filling) {
                generate();
            }
        });
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        BackgroundTask.run(this, () -> {
            Lists lists = new Lists();
            lists.trainers = trainerService.getAllTrainers();
            lists.members = isAdmin ? memberService.getAllMembers() : new java.util.ArrayList<Member>();
            return lists;
        }, lists -> {
            filling = true;
            try {
                Object keepTrainer = trainerCombo.getSelectedItem();
                trainerCombo.removeAllItems();
                trainerCombo.addItem("All trainers");
                for (Trainer t : lists.trainers) {
                    trainerCombo.addItem(t);
                    if (keepTrainer instanceof Trainer && ((Trainer) keepTrainer).getTrainerId() == t.getTrainerId()) {
                        trainerCombo.setSelectedItem(t);
                    }
                }
                Member keepMember = (Member) memberCombo.getSelectedItem();
                memberCombo.removeAllItems();
                for (Member m : lists.members) {
                    memberCombo.addItem(m);
                    if (keepMember != null && keepMember.getMemberId() == m.getMemberId()) {
                        memberCombo.setSelectedItem(m);
                    }
                }
            } finally {
                filling = false;
            }
            generate();
        });
    }

    /** Runs the selected report in the background and shows the result. */
    private void generate() {
        final ReportItem item = list.getSelectedValue();
        if (item == null) {
            return;
        }
        titleLabel.setText(item.title);
        descriptionLabel.setText(item.description);
        optionLayout.show(optionPanel, item.optionCard);

        final int myToken = ++token;
        runButton.setEnabled(false);
        BackgroundTask.run(this, item.factory.get(), report -> {
            runButton.setEnabled(true);
            if (myToken == token) {                                      // ignore answers of older requests
                display(report);
            }
        }, ex -> {
            runButton.setEnabled(true);
            countLabel.setText(" ");
            BackgroundTask.showError(this, ex);
        });
    }

    private void display(ReportTable report) {
        current = report;
        Object[][] data = new Object[report.getRowCount()][];
        for (int i = 0; i < data.length; i++) {
            data[i] = report.getRows().get(i);
        }
        tableModel.setDataVector(data, report.getHeaders());
        countLabel.setText(report.getRowCount() + (report.getRowCount() == 1 ? " row" : " rows")
                + "   |   click a column title to sort   |   Export to CSV opens in Excel");
        exportButton.setEnabled(true);
    }

    // ================================================================ CSV export

    private void exportCsv() {
        if (current == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export report to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV file (*.csv)", "csv"));
        String suggestion = current.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        chooser.setSelectedFile(new File(suggestion + "-" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = chooser.getSelectedFile();
        final File file = chosen.getName().toLowerCase().endsWith(".csv")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".csv");
        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this, file.getName() + " already exists. Replace it?",
                    "Export", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }
        final ReportTable report = current;
        BackgroundTask.run(this, () -> {
            reportService.exportToCsv(report, file);
            return file;
        }, saved -> BackgroundTask.showInfo(this, report.getRowCount() + " rows saved to\n" + saved.getAbsolutePath()));
    }

    // ================================================================ list rendering

    /** Draws one report in the list: bold title with a grey description below. */
    private static class ReportCellRenderer implements ListCellRenderer<ReportItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ReportItem> list, ReportItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout(0, 2));
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, isSelected ? 4 : 0, 1, 0, isSelected ? Theme.ACCENT : Theme.BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 8)));
            p.setBackground(isSelected ? new Color(0xFFF7ED) : Color.WHITE);
            JLabel title = UiFactory.label(value.title, 14, true, Theme.TEXT);
            JLabel desc = UiFactory.label("<html><div style='width:200px'>" + value.description + "</div></html>", 11, false, Theme.MUTED);
            p.add(title, BorderLayout.NORTH);
            p.add(desc, BorderLayout.CENTER);
            return p;
        }
    }
}
