package com.smartgym.gui.scheduling;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.service.SchedulingService;
import com.smartgym.service.SessionManager;
import com.smartgym.service.TrainerService;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Trainer scheduling for admins and trainers.
 *   Tab "Sessions":  a filterable list of all sessions, with book / edit / complete / cancel / delete.
 *   Tab "Timetable": a visual one-day grid (one column per trainer). Double-click an empty slot to book.
 * An admin manages every session; a trainer can only manage their OWN sessions (buttons are disabled
 * for others, and SchedulingService refuses anyway).
 */
public class SchedulePanel extends JPanel implements Refreshable, TimetablePanel.Listener {

    private static final long serialVersionUID = 1L;
    private static final String ALL_TRAINERS = "All trainers";

    /** Everything loaded in one background task. */
    private static final class Data {
        List<Trainer> trainers;
        List<TrainerSchedule> sessions;
    }

    private final transient SchedulingService service = new SchedulingService();
    private final transient TrainerService trainerService = new TrainerService();
    private final transient SessionManager session = SessionManager.getInstance();
    private final boolean isAdmin = session.getRole() == Role.ADMIN;

    private transient List<Trainer> allTrainers = new ArrayList<>();
    private boolean updating = false;                   // true while combo boxes are being refilled (ignore their events)
    private boolean firstLoad = true;
    private int timetableToken = 0;

    // ---- sessions tab
    private final SessionTableModel model = new SessionTableModel(true, true);
    private final JTable table = UiFactory.createTable(model);
    private final transient TableRowSorter<SessionTableModel> sorter = new TableRowSorter<>(model);
    private final JComboBox<Object> trainerFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[] {"All", "Scheduled", "Completed", "Cancelled"});
    private final JTextField textFilter = UiFactory.searchField("Filter by member or notes", 18);
    private final JLabel countLabel = UiFactory.muted(" ");
    private final RoundedButton bookButton = new RoundedButton("+  Book Session", Theme.ACCENT);
    private final RoundedButton editButton = RoundedButton.secondary("Edit");
    private final RoundedButton completeButton = RoundedButton.secondary("Mark Completed");
    private final RoundedButton cancelButton = RoundedButton.secondary("Cancel Session");
    private final RoundedButton deleteButton = new RoundedButton("Delete", Theme.DANGER);

    // ---- timetable tab
    private final TimetablePanel timetable = new TimetablePanel();
    private final DateSpinner daySpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<Object> dayTrainer = new JComboBox<>();
    private final JLabel dayTitle = UiFactory.label(" ", 16, true, Theme.TEXT);
    private final RoundedButton prevDay = RoundedButton.secondary("\u2039");
    private final RoundedButton nextDay = RoundedButton.secondary("\u203A");
    private final RoundedButton todayButton = RoundedButton.secondary("Today");

    public SchedulePanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bold(13));
        tabs.addTab("Sessions", buildSessionsTab());
        tabs.addTab("Timetable", buildTimetableTab());
        tabs.setName("sch.tabs");
        add(tabs, BorderLayout.CENTER);

        timetable.setListener(this);
        wireEvents();
        updateButtons();
    }

    // ================================================================ layout

    private JPanel buildSessionsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));

        UiFactory.styleCombo(trainerFilter);
        UiFactory.styleCombo(statusFilter);
        trainerFilter.setPreferredSize(new java.awt.Dimension(170, 38));
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(textFilter);
        bar.add(trainerFilter);
        bar.add(statusFilter);
        bar.add(bookButton);
        bar.add(editButton);
        bar.add(completeButton);
        bar.add(cancelButton);
        if (isAdmin) {
            bar.add(deleteButton);
        }
        tab.add(bar, BorderLayout.NORTH);

        UiFactory.styleTable(table);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(model.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 110, 120, 70, 130, 140, 110, 220);
        tab.add(UiFactory.scroll(table), BorderLayout.CENTER);
        tab.add(countLabel, BorderLayout.SOUTH);

        table.setName("sch.table");
        trainerFilter.setName("sch.trainer");
        statusFilter.setName("sch.status");
        textFilter.setName("sch.filter");
        bookButton.setName("sch.book");
        editButton.setName("sch.edit");
        completeButton.setName("sch.complete");
        cancelButton.setName("sch.cancel");
        deleteButton.setName("sch.delete");
        return tab;
    }

    private JPanel buildTimetableTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));

        UiFactory.styleCombo(dayTrainer);
        dayTrainer.setPreferredSize(new java.awt.Dimension(170, 38));
        prevDay.setPreferredSize(new java.awt.Dimension(44, 38));
        nextDay.setPreferredSize(new java.awt.Dimension(44, 38));
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(prevDay);
        bar.add(daySpinner);
        bar.add(nextDay);
        bar.add(todayButton);
        bar.add(dayTrainer);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(bar, BorderLayout.NORTH);
        top.add(dayTitle, BorderLayout.CENTER);
        tab.add(top, BorderLayout.NORTH);

        JScrollPane scroll = UiFactory.scroll(timetable);
        scroll.getViewport().setBackground(Color.WHITE);
        tab.add(scroll, BorderLayout.CENTER);
        tab.add(UiFactory.muted("Double-click an empty slot to book a session there, or double-click a block to edit it. "
                + "Grey areas are outside the trainer's working hours."), BorderLayout.SOUTH);

        timetable.setName("sch.timetable");
        daySpinner.setName("tt.date");
        dayTrainer.setName("tt.trainer");
        prevDay.setName("tt.prev");
        nextDay.setName("tt.next");
        todayButton.setName("tt.today");
        return tab;
    }

    // ================================================================ events

    private void wireEvents() {
        textFilter.getDocument().addDocumentListener(new DocumentListener() {
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
        trainerFilter.addActionListener(e -> {
            if (!updating) {
                applyFilter();
            }
        });
        statusFilter.addActionListener(e -> applyFilter());

        bookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object chosen = trainerFilter.getSelectedItem();
                Trainer pre = chosen instanceof Trainer ? (Trainer) chosen : null;
                openBooking(pre, null, null);
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TrainerSchedule s = selected();
                if (s != null) {
                    openEdit(s);
                }
            }
        });
        completeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completeSelected();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelSelected();
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selected() != null) {
                    openEdit(selected());
                }
            }
        });

        // timetable controls
        daySpinner.addChangeListener(e -> loadTimetable());
        dayTrainer.addActionListener(e -> {
            if (!updating) {
                loadTimetable();
            }
        });
        prevDay.addActionListener(e -> daySpinner.setDate(daySpinner.getDate().minusDays(1)));
        nextDay.addActionListener(e -> daySpinner.setDate(daySpinner.getDate().plusDays(1)));
        todayButton.addActionListener(e -> daySpinner.setDate(LocalDate.now()));
    }

    // ================================================================ who may do what

    private boolean canManage(TrainerSchedule s) {
        if (isAdmin) {
            return true;
        }
        return session.getTrainerId() != null && session.getTrainerId() == s.getTrainerId();
    }

    private void updateButtons() {
        TrainerSchedule s = selected();
        boolean mine = s != null && canManage(s);
        boolean open = mine && Constants.SCHEDULED.equals(s.getStatus());
        editButton.setEnabled(open);
        completeButton.setEnabled(open);
        cancelButton.setEnabled(open);
        deleteButton.setEnabled(s != null);
    }

    // ================================================================ filtering

    private void applyFilter() {
        final String status = ((String) statusFilter.getSelectedItem()).toUpperCase();
        final Object trainerChoice = trainerFilter.getSelectedItem();
        final String trainerName = trainerChoice instanceof Trainer ? ((Trainer) trainerChoice).getFullName() : null;
        final String text = textFilter.getText().trim().toLowerCase();
        final int trainerCol = model.columnOf(3);
        final int memberCol = model.columnOf(4);
        final int notesCol = model.columnOf(6);
        final int statusCol = model.statusColumn();
        sorter.setRowFilter(new RowFilter<SessionTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends SessionTableModel, ? extends Integer> entry) {
                if (!status.equals("ALL") && !status.equals(entry.getStringValue(statusCol))) {
                    return false;
                }
                if (trainerName != null && !trainerName.equals(entry.getStringValue(trainerCol))) {
                    return false;
                }
                return text.isEmpty()
                        || entry.getStringValue(memberCol).toLowerCase().contains(text)
                        || entry.getStringValue(notesCol).toLowerCase().contains(text);
            }
        });
        countLabel.setText("Showing " + table.getRowCount() + " of " + model.getRowCount() + " sessions");
        updateButtons();
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        BackgroundTask.run(this, () -> {
            Data d = new Data();
            d.trainers = trainerService.getAllTrainers();
            d.sessions = service.getAllSessions();
            return d;
        }, d -> {
            allTrainers = d.trainers;
            fillTrainerCombos();
            model.setSessions(d.sessions);
            applyFilter();
            loadTimetable();
        });
    }

    /** Fills both trainer drop-downs ("All trainers" + every trainer), keeping the current choice. */
    private void fillTrainerCombos() {
        updating = true;
        try {
            refill(trainerFilter);
            refill(dayTrainer);
            if (firstLoad) {                                       // a trainer starts by looking at their own work
                firstLoad = false;
                if (session.getRole() == Role.TRAINER && session.getTrainerId() != null) {
                    selectTrainer(trainerFilter, session.getTrainerId());
                    selectTrainer(dayTrainer, session.getTrainerId());
                }
            }
        } finally {
            updating = false;
        }
    }

    private void refill(JComboBox<Object> combo) {
        Object before = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(ALL_TRAINERS);
        for (Trainer t : allTrainers) {
            combo.addItem(t);
        }
        if (before instanceof Trainer) {
            selectTrainer(combo, ((Trainer) before).getTrainerId());
        }
    }

    private static void selectTrainer(JComboBox<Object> combo, int trainerId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item instanceof Trainer && ((Trainer) item).getTrainerId() == trainerId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Loads the sessions of the chosen day for the timetable. */
    private void loadTimetable() {
        final LocalDate date = daySpinner.getDate();
        final int token = ++timetableToken;
        dayTitle.setText(TimetablePanel.dayTitle(date));
        final Object choice = dayTrainer.getSelectedItem();
        BackgroundTask.run(this, () -> service.getSessionsOnDate(date), list -> {
            if (token != timetableToken) {
                return;                                            // an older answer, ignore
            }
            List<Trainer> columns = new ArrayList<>();
            if (choice instanceof Trainer) {
                columns.add((Trainer) choice);
            } else {
                columns.addAll(allTrainers);
            }
            timetable.setData(date, columns, list);
        });
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private TrainerSchedule selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.getSession(table.convertRowIndexToModel(row));
    }

    // ================================================================ opening dialogs

    private void openBooking(Trainer trainer, LocalDate date, LocalTime time) {
        new SessionFormDialog(window(), null, trainer, date, time, this::refreshData).setVisible(true);
    }

    private void openEdit(TrainerSchedule s) {
        if (!canManage(s)) {
            BackgroundTask.showInfo(this, "This session belongs to " + s.getTrainerName()
                    + ". Trainers can only change their own sessions.");
            return;
        }
        new SessionFormDialog(window(), s, null, null, null, this::refreshData).setVisible(true);
    }

    /** Timetable callback: double-click on an empty slot. */
    @Override
    public void emptySlotOpened(Trainer trainer, LocalTime time) {
        if (!isAdmin && (session.getTrainerId() == null || session.getTrainerId() != trainer.getTrainerId())) {
            BackgroundTask.showInfo(this, "Trainers can only book sessions for themselves.");
            return;
        }
        openBooking(trainer, daySpinner.getDate(), time);
    }

    /** Timetable callback: double-click on a session block. */
    @Override
    public void sessionOpened(TrainerSchedule s) {
        openEdit(s);
    }

    // ================================================================ actions

    private void completeSelected() {
        final TrainerSchedule s = selected();
        if (s == null) {
            return;
        }
        BackgroundTask.run(this, () -> {
            service.completeSession(s.getScheduleId());
            return Boolean.TRUE;
        }, ok -> refreshData());
    }

    private void cancelSelected() {
        final TrainerSchedule s = selected();
        if (s == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Cancel the session of " + s.getMemberName() + " with "
                + s.getTrainerName() + " on " + s.getSessionDate().format(UiFactory.DATE_FORMAT) + "?",
                "Cancel session", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            BackgroundTask.run(this, () -> {
                service.cancelSession(s.getScheduleId());
                return Boolean.TRUE;
            }, ok -> refreshData());
        }
    }

    private void deleteSelected() {
        final TrainerSchedule s = selected();
        if (s == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete this session record?\n(Use \"Cancel Session\" to keep a record of it.)",
                "Delete session", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            BackgroundTask.run(this, () -> {
                service.deleteSession(s.getScheduleId());
                return Boolean.TRUE;
            }, ok -> refreshData());
        }
    }
}
