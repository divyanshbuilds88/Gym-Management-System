package com.smartgym.gui;

import com.smartgym.gui.common.AvatarLabel;
import com.smartgym.gui.common.IconFactory;
import com.smartgym.gui.common.NavButton;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.attendance.AttendancePanel;
import com.smartgym.gui.attendance.MyAttendancePanel;
import com.smartgym.gui.dashboard.DashboardPanel;
import com.smartgym.gui.login.LoginFrame;
import com.smartgym.gui.members.MembersPanel;
import com.smartgym.gui.membership.MembershipPanel;
import com.smartgym.gui.membership.MyMembershipPanel;
import com.smartgym.gui.payments.MyPaymentsPanel;
import com.smartgym.gui.payments.PaymentsPanel;
import com.smartgym.gui.profile.ProfilePanel;
import com.smartgym.gui.reports.ReportsPanel;
import com.smartgym.gui.scheduling.MySessionsPanel;
import com.smartgym.gui.scheduling.SchedulePanel;
import com.smartgym.gui.trainers.TrainersPanel;
import com.smartgym.gui.workout.MyWorkoutsPanel;
import com.smartgym.gui.workout.WorkoutsPanel;
import com.smartgym.model.Role;
import com.smartgym.model.User;
import com.smartgym.service.AuthService;
import com.smartgym.service.SessionManager;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The main application window: header on top, sidebar on the left, and a CardLayout in the middle
 * that shows one module panel at a time. The menu is built from the logged-in user's ROLE.
 *
 * To add a new module, add one line to definePages():
 *   list.add(new PageDef("key", "Title", "Title for members or null", "icon", roles, YourPanel::new));
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService = new AuthService();
    private final transient SessionManager session = SessionManager.getInstance();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);
    private final transient Map<String, JPanel> openedPanels = new HashMap<>();
    private final transient Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final transient List<PageDef> pages = new ArrayList<>();
    private final JLabel pageTitle = UiFactory.label("", 22, true, Theme.TEXT);
    private String currentKey;

    public MainFrame() {
        super("Smart Gym Management System");
        setIconImage(Theme.appIcon());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1020, 640));
        setSize(1300, 800);
        setLocationRelativeTo(null);

        pages.addAll(definePages());

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.add(buildHeader(), BorderLayout.NORTH);
        contentArea.setBackground(Theme.BACKGROUND);
        right.add(contentArea, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);

        installShortcuts();
        installWindowEvents();

        Role role = session.getRole();
        showPage(role == Role.MEMBER ? "profile" : "dashboard");
    }

    // ================================================================ pages

    /** One menu entry + the panel it opens. */
    private static final class PageDef {
        final String key;
        final String title;
        final String memberTitle;          // different menu text for members, or null
        final String icon;
        final Role[] roles;
        final Supplier<JPanel> factory;

        PageDef(String key, String title, String memberTitle, String icon, Role[] roles, Supplier<JPanel> factory) {
            this.key = key;
            this.title = title;
            this.memberTitle = memberTitle;
            this.icon = icon;
            this.roles = roles;
            this.factory = factory;
        }

        boolean allows(Role role) {
            for (Role r : roles) {
                if (r == role) {
                    return true;
                }
            }
            return false;
        }

        String titleFor(Role role) {
            return (role == Role.MEMBER && memberTitle != null) ? memberTitle : title;
        }
    }

    private List<PageDef> definePages() {
        Role[] all = {Role.ADMIN, Role.TRAINER, Role.MEMBER};
        Role[] staff = {Role.ADMIN, Role.TRAINER};
        Role[] adminAndMember = {Role.ADMIN, Role.MEMBER};

        List<PageDef> list = new ArrayList<>();
        list.add(new PageDef("dashboard", "Dashboard", null, "dashboard", staff, DashboardPanel::new));
        list.add(new PageDef("members", "Members", null, "members", staff, MembersPanel::new));
        // admins manage all memberships; a member sees only their own, so the panel depends on the role
        list.add(new PageDef("memberships", "Memberships", "My Membership", "membership", adminAndMember,
                () -> session.getRole() == Role.MEMBER ? new MyMembershipPanel() : new MembershipPanel()));
        list.add(new PageDef("trainers", "Trainers", null, "trainers", all, TrainersPanel::new));
        // admins and trainers manage plans; a member only sees the plans given to them
        list.add(new PageDef("workouts", "Workout Plans", "My Workouts", "workout", all,
                () -> session.getRole() == Role.MEMBER ? new MyWorkoutsPanel() : new WorkoutsPanel()));
        list.add(new PageDef("scheduling", "Trainer Schedule", "My Sessions", "schedule", all,
                () -> session.getRole() == Role.MEMBER ? new MySessionsPanel() : new SchedulePanel()));
        list.add(new PageDef("attendance", "Attendance", null, "attendance", all,
                () -> session.getRole() == Role.MEMBER ? new MyAttendancePanel() : new AttendancePanel()));
        list.add(new PageDef("payments", "Payments", "My Payments", "payments", adminAndMember,
                () -> session.getRole() == Role.MEMBER ? new MyPaymentsPanel() : new PaymentsPanel()));
        list.add(new PageDef("reports", "Reports", null, "reports", staff, ReportsPanel::new));
        list.add(new PageDef("profile", "My Profile", null, "profile", all, ProfilePanel::new));
        return list;
    }

    private PageDef find(String key) {
        for (PageDef p : pages) {
            if (p.key.equals(key)) {
                return p;
            }
        }
        return null;
    }

    /** Creates the panel the first time, then just shows it. Refreshable panels reload their data. */
    private void showPage(String key) {
        PageDef def = find(key);
        if (def == null) {
            return;
        }
        JPanel panel = openedPanels.get(key);
        if (panel == null) {
            panel = def.factory.get();
            openedPanels.put(key, panel);
            contentArea.add(panel, key);
        }
        cardLayout.show(contentArea, key);
        currentKey = key;
        pageTitle.setText(def.titleFor(session.getRole()));
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setSelected(e.getKey().equals(key));
        }
        if (panel instanceof Refreshable) {
            ((Refreshable) panel).refreshData();
        }
    }

    // ================================================================ sidebar and header

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(240, 0));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 22));
        brand.setOpaque(false);
        JLabel logo = new JLabel("SMART GYM", IconFactory.get("workout", 26, Theme.ACCENT), JLabel.LEFT);
        logo.setIconTextGap(10);
        logo.setFont(Theme.bold(19));
        logo.setForeground(Color.WHITE);
        brand.add(logo);
        side.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        JLabel section = UiFactory.label("  MENU", 11, true, new Color(0x6B7280));
        section.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 0));
        section.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(section);

        final Role role = session.getRole();
        for (final PageDef def : pages) {
            if (!def.allows(role)) {
                continue;                                   // hide menu items this role may not use
            }
            NavButton button = new NavButton(def.titleFor(role), def.icon);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showPage(def.key);
                }
            });
            navButtons.put(def.key, button);
            menu.add(button);
        }
        side.add(menu, BorderLayout.CENTER);

        JLabel footer = UiFactory.label("  Java Swing + MySQL  |  v1.0", 11, false, new Color(0x6B7280));
        footer.setBorder(BorderFactory.createEmptyBorder(12, 8, 14, 0));
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 26, 12, 22)));
        header.add(pageTitle, BorderLayout.WEST);

        User user = session.getCurrentUser();
        AvatarLabel avatar = new AvatarLabel(user.getFullName(), 40, roleColor(user.getRole()));
        avatar.setPerson(user.getFullName());

        JLabel name = UiFactory.label(user.getFullName(), 14, true, Theme.TEXT);
        JLabel badge = UiFactory.label(user.getRoleName(), 11, true, roleColor(user.getRole()));
        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(name);
        who.add(badge);

        RoundedButton logout = RoundedButton.secondaryWithIcon("Logout", "logout");
        logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(avatar);
        right.add(who);
        right.add(Box.createHorizontalStrut(10));
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private static Color roleColor(Role role) {
        switch (role) {
            case ADMIN:
                return Theme.ACCENT_DARK;
            case TRAINER:
                return Theme.TEAL;
            default:
                return Theme.INFO;
        }
    }

    // ================================================================ events

    /** F5 reloads the current screen. Key bindings are the modern way to handle keyboard shortcuts. */
    private void installShortcuts() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "reload");
        root.getActionMap().put("reload", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel panel = openedPanels.get(currentKey);
                if (panel instanceof Refreshable) {
                    ((Refreshable) panel).refreshData();
                }
            }
        });
    }

    private void installWindowEvents() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Do you want to log out and exit Smart Gym?", "Exit", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    authService.logout();
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of Smart Gym?", "Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            authService.logout();                            // clears the session
            dispose();
            LoginFrame.showLogin();
        }
    }
}
