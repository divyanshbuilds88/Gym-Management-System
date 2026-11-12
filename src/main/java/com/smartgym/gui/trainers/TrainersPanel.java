package com.smartgym.gui.trainers;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.LiveSearch;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.service.SessionManager;
import com.smartgym.service.TrainerService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Trainer list for everybody; only the admin sees the Add / Edit / Delete buttons and the salary column.
 * (The TrainerService enforces the same rules, so hiding buttons is just for convenience.)
 */
public class TrainersPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient TrainerService service = new TrainerService();
    private final boolean isAdmin = SessionManager.getInstance().getRole() == Role.ADMIN;
    private final TrainerTableModel model = new TrainerTableModel(isAdmin);
    private final JTable table = UiFactory.createTable(model);
    private final JTextField searchField = UiFactory.searchField("Search by name, specialization, phone or email", 28);
    private final JLabel countLabel = UiFactory.muted(" ");
    private final transient LiveSearch liveSearch;

    private final RoundedButton addButton = new RoundedButton("+  Add Trainer", Theme.ACCENT);
    private final RoundedButton editButton = RoundedButton.secondary("Edit");
    private final RoundedButton deleteButton = new RoundedButton("Delete", Theme.DANGER);
    private final RoundedButton refreshButton = RoundedButton.secondaryWithIcon("Refresh", "refresh");

    public TrainersPanel() {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(searchField);
        if (isAdmin) {
            bar.add(addButton);
            bar.add(editButton);
            bar.add(deleteButton);
        }
        bar.add(refreshButton);
        add(bar, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        UiFactory.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiFactory.setColumnWidths(table, 130, 190, 100, 180, 170, 90, 100);
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        add(countLabel, BorderLayout.SOUTH);

        searchField.setName("trainers.search");
        table.setName("trainers.table");
        addButton.setName("trainers.add");
        editButton.setName("trainers.edit");
        deleteButton.setName("trainers.delete");

        liveSearch = new LiveSearch(searchField, 300, this::load);
        wireEvents();
        updateButtons();
    }

    private void wireEvents() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new TrainerFormDialog(window(), null, TrainersPanel.this::load).setVisible(true);
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelected();
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                load();
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
                if (isAdmin && e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelected();
                }
            }
        });
    }

    private void updateButtons() {
        boolean selected = table.getSelectedRow() >= 0;
        editButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
    }

    @Override
    public void refreshData() {
        load();
    }

    private void load() {
        final String keyword = searchField.getText().trim();
        final int token = liveSearch.nextToken();
        BackgroundTask.run(this, () -> service.searchTrainers(keyword), list -> {
            if (!liveSearch.isCurrent(token)) {
                return;
            }
            Trainer before = selectedTrainer();
            model.setTrainers(list);
            if (before != null) {                                       // keep the same trainer selected
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (model.getTrainer(i).getTrainerId() == before.getTrainerId()) {
                        int viewRow = table.convertRowIndexToView(i);
                        table.setRowSelectionInterval(viewRow, viewRow);
                        break;
                    }
                }
            }
            countLabel.setText(list.isEmpty()
                    ? "No trainer matches \"" + keyword + "\"."
                    : "Showing " + list.size() + (list.size() == 1 ? " trainer" : " trainers"));
            updateButtons();
        });
    }

    private Trainer selectedTrainer() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.getTrainer(table.convertRowIndexToModel(row));
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void editSelected() {
        Trainer t = selectedTrainer();
        if (t != null) {
            new TrainerFormDialog(window(), t, this::load).setVisible(true);
        }
    }

    private void deleteSelected() {
        final Trainer t = selectedTrainer();
        if (t == null) {
            return;
        }
        Object[] options = {"Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Delete trainer " + t.getFullName() + "?\n\nTheir login account and training sessions are removed too.\n"
                        + "A trainer who still supervises members' workout plans cannot be deleted.",
                "Delete trainer", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0) {
            return;
        }
        BackgroundTask.run(this, () -> {
            service.deleteTrainer(t.getTrainerId());
            return Boolean.TRUE;
        }, ok -> load());
    }
}
