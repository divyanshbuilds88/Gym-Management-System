package com.smartgym.gui.members;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.LiveSearch;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.gui.membership.AssignPlanDialog;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.service.MemberService;
import com.smartgym.service.SessionManager;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Member management: view, live search, add, edit, delete, and assign / renew a membership plan.
 * Admins can change data; trainers can only look (the buttons are hidden for them, and the
 * MemberService refuses the action anyway).
 */
public class MembersPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient MemberService memberService = new MemberService();
    private final MemberTableModel model = new MemberTableModel();
    private final JTable table = UiFactory.createTable(model);
    private final JTextField searchField = UiFactory.searchField("Search by name, code, phone or email", 28);
    private final JLabel countLabel = UiFactory.muted(" ");
    private final boolean canEdit = SessionManager.getInstance().getRole() == Role.ADMIN;

    private final RoundedButton addButton = new RoundedButton("+  Add Member", Theme.ACCENT);
    private final RoundedButton editButton = RoundedButton.secondary("Edit");
    private final RoundedButton deleteButton = new RoundedButton("Delete", Theme.DANGER);
    private final RoundedButton planButton = RoundedButton.secondary("Assign Plan");
    private final RoundedButton renewButton = RoundedButton.secondary("Renew");
    private final RoundedButton refreshButton = RoundedButton.secondaryWithIcon("Refresh", "refresh");

    private final JPopupMenu popup = new JPopupMenu();
    private final transient LiveSearch liveSearch;

    public MembersPanel() {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(buildToolbar(), BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        UiFactory.styleTable(table);
        table.setAutoCreateRowSorter(true);                          // click a header to sort
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(8).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 85, 130, 45, 75, 100, 165, 95, 105, 100);
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        add(countLabel, BorderLayout.SOUTH);

        // search 300 ms after the last key press; old answers are ignored (see LiveSearch)
        liveSearch = new LiveSearch(searchField, 300, this::load);

        wireEvents();
        updateButtons();
    }

    // ================================================================ layout

    private JPanel buildToolbar() {
        // WrapLayout: buttons move to a second row when the window is narrow
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(searchField);
        if (canEdit) {
            bar.add(addButton);
            bar.add(editButton);
            bar.add(planButton);
            bar.add(renewButton);
            bar.add(deleteButton);
        }
        bar.add(refreshButton);

        searchField.setName("members.search");
        table.setName("members.table");
        addButton.setName("members.add");
        editButton.setName("members.edit");
        deleteButton.setName("members.delete");
        planButton.setName("members.plan");
        renewButton.setName("members.renew");
        return bar;
    }

    // ================================================================ events

    private void wireEvents() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMember();
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
        planButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPlanDialog(false);
            }
        });
        renewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPlanDialog(true);
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                load();
            }
        });

        // enable / disable buttons depending on the selected row
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });

        // Mouse: double-click = edit, right-click = pop-up menu
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (canEdit && e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelected();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });

        if (canEdit) {
            addPopupItem("Edit member", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editSelected();
                }
            });
            addPopupItem("Assign membership plan...", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openPlanDialog(false);
                }
            });
            addPopupItem("Renew membership...", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openPlanDialog(true);
                }
            });
            popup.addSeparator();
            addPopupItem("Delete member", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteSelected();
                }
            });
        }

        // Keyboard shortcuts (key bindings): Enter = edit, Delete = delete, Ctrl+F = search
        if (canEdit) {
            bindKey(table, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "edit", this::editSelected);
            bindKey(table, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", this::deleteSelected);
        }
        bindKey(this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                KeyStroke.getKeyStroke(KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK), "find",
                () -> searchField.requestFocusInWindow());
    }

    private static void bindKey(JComponent component, int condition, KeyStroke key, String name, final Runnable action) {
        component.getInputMap(condition).put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void addPopupItem(String text, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(Theme.regular(13));
        item.addActionListener(listener);
        popup.add(item);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (canEdit && e.isPopupTrigger()) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
                popup.show(table, e.getX(), e.getY());
            }
        }
    }

    private void updateButtons() {
        boolean selected = table.getSelectedRow() >= 0;
        editButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
        planButton.setEnabled(selected);
        renewButton.setEnabled(selected);
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        load();
    }

    private void load() {
        final String keyword = searchField.getText().trim();
        final int token = liveSearch.nextToken();
        BackgroundTask.run(this, () -> memberService.searchMembers(keyword), list -> {
            if (!liveSearch.isCurrent(token)) {
                return;                                   // a newer search was started, ignore this old answer
            }
            int selectedId = selectedMember() == null ? -1 : selectedMember().getMemberId();
            model.setMembers(list);
            for (int i = 0; i < model.getRowCount(); i++) {            // keep the same member selected
                if (model.getMember(i).getMemberId() == selectedId) {
                    int viewRow = table.convertRowIndexToView(i);
                    table.setRowSelectionInterval(viewRow, viewRow);
                    break;
                }
            }
            countLabel.setText(list.isEmpty()
                    ? (keyword.isEmpty() ? "No members yet." : "No member matches \"" + keyword + "\".")
                    : "Showing " + list.size() + (list.size() == 1 ? " member" : " members")
                            + (keyword.isEmpty() ? "" : " matching \"" + keyword + "\"")
                            + "   |   click a column title to sort, double-click a row to edit");
            updateButtons();
        });
    }

    private Member selectedMember() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return model.getMember(table.convertRowIndexToModel(viewRow));   // sorting changes row order
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    // ================================================================ actions

    private void addMember() {
        new MemberFormDialog(window(), null, this::load).setVisible(true);
    }

    private void editSelected() {
        Member m = selectedMember();
        if (m != null) {
            new MemberFormDialog(window(), m, this::load).setVisible(true);
        }
    }

    private void openPlanDialog(boolean renew) {
        Member m = selectedMember();
        if (m != null) {
            new AssignPlanDialog(window(), m, renew, this::load).setVisible(true);
        }
    }

    private void deleteSelected() {
        final Member m = selectedMember();
        if (m == null) {
            return;
        }
        Object[] options = {"Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Delete member " + m.getFullName() + " (" + m.getMemberCode() + ")?\n\n"
                        + "Their memberships, payments, attendance and sessions will be deleted too.\n"
                        + "This cannot be undone.",
                "Delete member", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0) {
            return;
        }
        BackgroundTask.run(this, () -> {
            memberService.deleteMember(m.getMemberId());
            return Boolean.TRUE;
        }, ok -> load());
    }
}
