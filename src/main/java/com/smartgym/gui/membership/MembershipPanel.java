package com.smartgym.gui.membership;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.model.Membership;
import com.smartgym.model.MembershipPlan;
import com.smartgym.service.MemberService;
import com.smartgym.service.MembershipService;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

/**
 * Admin screen for memberships: one tab lists every membership (filter by status or text),
 * the other tab manages the membership plans.
 */
public class MembershipPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient MembershipService service = new MembershipService();
    private final transient MemberService memberService = new MemberService();

    // ---- memberships tab
    private final MembershipTableModel msModel = new MembershipTableModel(true);
    private final JTable msTable = UiFactory.createTable(msModel);
    private final transient TableRowSorter<MembershipTableModel> sorter = new TableRowSorter<>(msModel);
    private final JTextField filterField = UiFactory.searchField("Filter by code, member or plan", 24);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {"All", "Active", "Expired", "Cancelled"});
    private final JLabel msCount = UiFactory.muted(" ");
    private final RoundedButton assignButton = new RoundedButton("+  Assign Plan", Theme.ACCENT);
    private final RoundedButton renewButton = RoundedButton.secondary("Renew");
    private final RoundedButton cancelButton = new RoundedButton("Cancel Membership", Theme.DANGER);
    private final RoundedButton expireButton = RoundedButton.secondary("Expire Overdue");
    private final RoundedButton msRefresh = RoundedButton.secondaryWithIcon("Refresh", "refresh");

    // ---- plans tab
    private final PlanTableModel planModel = new PlanTableModel(true);
    private final JTable planTable = UiFactory.createTable(planModel);
    private final RoundedButton addPlanButton = new RoundedButton("+  Add Plan", Theme.ACCENT);
    private final RoundedButton editPlanButton = RoundedButton.secondary("Edit");
    private final RoundedButton deletePlanButton = new RoundedButton("Delete", Theme.DANGER);

    public MembershipPanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bold(13));
        tabs.addTab("Memberships", buildMembershipsTab());
        tabs.addTab("Plans", buildPlansTab());
        add(tabs, BorderLayout.CENTER);

        wireEvents();
        updateButtons();
    }

    // ================================================================ layout

    private JPanel buildMembershipsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        UiFactory.styleCombo(statusCombo);
        bar.add(filterField);
        bar.add(statusCombo);
        bar.add(assignButton);
        bar.add(renewButton);
        bar.add(cancelButton);
        bar.add(expireButton);
        bar.add(msRefresh);
        tab.add(bar, BorderLayout.NORTH);

        UiFactory.styleTable(msTable);
        msTable.setRowSorter(sorter);
        msTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        msTable.getColumnModel().getColumn(msModel.statusColumn()).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(msTable, 85, 150, 95, 105, 105, 100, 80);
        tab.add(UiFactory.scroll(msTable), BorderLayout.CENTER);
        tab.add(msCount, BorderLayout.SOUTH);

        filterField.setName("ms.filter");
        statusCombo.setName("ms.status");
        msTable.setName("ms.table");
        assignButton.setName("ms.assign");
        renewButton.setName("ms.renew");
        cancelButton.setName("ms.cancel");
        expireButton.setName("ms.expire");
        return tab;
    }

    private JPanel buildPlansTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addPlanButton);
        buttons.add(editPlanButton);
        buttons.add(deletePlanButton);
        tab.add(buttons, BorderLayout.NORTH);

        UiFactory.styleTable(planTable);
        planTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        planTable.setAutoCreateRowSorter(true);
        UiFactory.setColumnWidths(planTable, 120, 70, 100, 300, 80);
        tab.add(UiFactory.scroll(planTable), BorderLayout.CENTER);
        tab.add(UiFactory.muted("Plans that are switched off are hidden from the \"Assign Plan\" list but old memberships keep working."),
                BorderLayout.SOUTH);

        planTable.setName("plan.table");
        addPlanButton.setName("plan.add");
        editPlanButton.setName("plan.edit");
        deletePlanButton.setName("plan.delete");
        return tab;
    }

    // ================================================================ events

    private void wireEvents() {
        // live filtering on the data we already loaded (no database call needed)
        filterField.getDocument().addDocumentListener(new DocumentListener() {
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
        statusCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyFilter();
            }
        });

        assignButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AssignPlanDialog(window(), null, false, MembershipPanel.this::refreshData).setVisible(true);
            }
        });
        renewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renewSelected();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelSelected();
            }
        });
        expireButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expireOverdue();
            }
        });
        msRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });

        addPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PlanFormDialog(window(), null, MembershipPanel.this::refreshData).setVisible(true);
            }
        });
        editPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editPlan();
            }
        });
        deletePlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deletePlan();
            }
        });
        planTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editPlan();
                }
            }
        });

        msTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        planTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
    }

    private void updateButtons() {
        Membership ms = selectedMembership();
        renewButton.setEnabled(ms != null);
        cancelButton.setEnabled(ms != null && Constants.ACTIVE.equals(ms.getStatus()));
        boolean planSelected = planTable.getSelectedRow() >= 0;
        editPlanButton.setEnabled(planSelected);
        deletePlanButton.setEnabled(planSelected);
    }

    /** Combines the status drop-down and the text box into one RowFilter. */
    private void applyFilter() {
        final String status = ((String) statusCombo.getSelectedItem()).toUpperCase();
        final String text = filterField.getText().trim().toLowerCase();
        sorter.setRowFilter(new RowFilter<MembershipTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends MembershipTableModel, ? extends Integer> entry) {
                if (!status.equals("ALL") && !status.equals(entry.getStringValue(5))) {
                    return false;
                }
                if (text.isEmpty()) {
                    return true;
                }
                return entry.getStringValue(0).toLowerCase().contains(text)
                        || entry.getStringValue(1).toLowerCase().contains(text)
                        || entry.getStringValue(2).toLowerCase().contains(text);
            }
        });
        updateCount();
        updateButtons();
    }

    private void updateCount() {
        msCount.setText("Showing " + msTable.getRowCount() + " of " + msModel.getRowCount() + " memberships");
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        BackgroundTask.run(this, service::getAllMemberships, list -> {
            msModel.setMemberships(list);
            applyFilter();
        });
        BackgroundTask.run(this, service::getAllPlans, plans -> {
            planModel.setPlans(plans);
            updateButtons();
        });
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private Membership selectedMembership() {
        int row = msTable.getSelectedRow();
        return row < 0 ? null : msModel.getMembership(msTable.convertRowIndexToModel(row));
    }

    private MembershipPlan selectedPlan() {
        int row = planTable.getSelectedRow();
        return row < 0 ? null : planModel.getPlan(planTable.convertRowIndexToModel(row));
    }

    // ================================================================ actions

    private void renewSelected() {
        final Membership ms = selectedMembership();
        if (ms == null) {
            return;
        }
        // we need the full Member object for the dialog, so load it first
        BackgroundTask.run(this, () -> memberService.getMember(ms.getMemberId()),
                member -> new AssignPlanDialog(window(), member, true, this::refreshData).setVisible(true));
    }

    private void cancelSelected() {
        final Membership ms = selectedMembership();
        if (ms == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel the " + ms.getPlanName() + " membership of " + ms.getMemberName() + "?",
                "Cancel membership", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            BackgroundTask.run(this, () -> {
                service.cancelMembership(ms.getMembershipId());
                return Boolean.TRUE;
            }, ok -> refreshData());
        }
    }

    /** Runs the stored procedure sp_expire_memberships (CallableStatement). */
    private void expireOverdue() {
        BackgroundTask.run(this, service::expireOverdueMemberships, count -> {
            BackgroundTask.showInfo(this, count + (count == 1 ? " membership was" : " memberships were")
                    + " marked as expired.");
            refreshData();
        });
    }

    private void editPlan() {
        MembershipPlan plan = selectedPlan();
        if (plan != null) {
            new PlanFormDialog(window(), plan, this::refreshData).setVisible(true);
        }
    }

    private void deletePlan() {
        final MembershipPlan plan = selectedPlan();
        if (plan == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Delete the plan \"" + plan.getPlanName() + "\"?",
                "Delete plan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            BackgroundTask.run(this, () -> {
                service.deletePlan(plan.getPlanId());
                return Boolean.TRUE;
            }, ok -> refreshData());
        }
    }
}
