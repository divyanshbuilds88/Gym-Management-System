package com.smartgym.gui.login;

import com.smartgym.exception.AuthenticationException;
import com.smartgym.gui.MainFrame;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.IconFactory;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Role;
import com.smartgym.model.User;
import com.smartgym.service.AuthService;
import com.smartgym.util.DatabaseConnection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Login window.
 *
 * Event handling used here (the "delegation event model": an EVENT SOURCE such as a button
 * fires an EVENT object, and a registered LISTENER handles it):
 *   ActionListener    - Sign In button, show-password check box, role radio buttons
 *   KeyAdapter        - Enter = sign in, Esc = clear, Caps-Lock warning
 *   MouseAdapter      - click a demo account to fill the form, hover effect
 *   WindowAdapter     - ask "Exit?" when the window is closed
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService = new AuthService();

    private final JTextField usernameField = UiFactory.textField(18);
    private final JPasswordField passwordField = UiFactory.passwordField(18);
    private final JCheckBox showPassword = new JCheckBox("Show password");
    private final JRadioButton adminRadio = new JRadioButton("Admin", true);
    private final JRadioButton trainerRadio = new JRadioButton("Trainer");
    private final JRadioButton memberRadio = new JRadioButton("Member");
    private final RoundedButton loginButton = new RoundedButton("Sign In", Theme.ACCENT);
    private final JLabel messageLabel = UiFactory.label(" ", 13, true, Theme.DANGER);
    private final JLabel statusLabel = UiFactory.label("Checking database...", 12, false, Theme.MUTED);

    public LoginFrame() {
        super("Smart Gym Management System - Login");
        setIconImage(Theme.appIcon());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);          // we decide in the WindowAdapter
        setMinimumSize(new Dimension(820, 560));
        setSize(980, 620);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(new BrandPanel());
        root.add(buildFormPanel());
        setContentPane(root);

        wireEvents();
        checkDatabaseInBackground();
    }

    // ================================================================ layout

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new java.awt.GridBagLayout());
        outer.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        form.add(left(UiFactory.title("Welcome back")));
        form.add(Box.createVerticalStrut(4));
        form.add(left(UiFactory.muted("Sign in to manage your gym")));
        form.add(Box.createVerticalStrut(22));

        form.add(left(UiFactory.fieldLabel("LOGIN AS")));
        form.add(Box.createVerticalStrut(6));
        ButtonGroup group = new ButtonGroup();                   // only one radio button can be selected
        JPanel roles = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        roles.setOpaque(false);
        for (JRadioButton r : new JRadioButton[] {adminRadio, trainerRadio, memberRadio}) {
            r.setOpaque(false);
            r.setFont(Theme.regular(13));
            r.setFocusPainted(false);
            group.add(r);
            roles.add(r);
            roles.add(Box.createHorizontalStrut(10));
        }
        form.add(left(roles));
        form.add(Box.createVerticalStrut(16));

        form.add(left(UiFactory.fieldLabel("USERNAME")));
        form.add(Box.createVerticalStrut(6));
        form.add(left(usernameField));
        form.add(Box.createVerticalStrut(14));

        form.add(left(UiFactory.fieldLabel("PASSWORD")));
        form.add(Box.createVerticalStrut(6));
        form.add(left(passwordField));
        form.add(Box.createVerticalStrut(8));
        showPassword.setOpaque(false);
        showPassword.setFocusPainted(false);
        form.add(left(showPassword));
        form.add(Box.createVerticalStrut(10));

        messageLabel.setPreferredSize(new Dimension(300, 34));
        form.add(left(messageLabel));
        form.add(Box.createVerticalStrut(6));

        loginButton.setFont(Theme.bold(15));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        form.add(loginButton);
        form.add(Box.createVerticalStrut(22));

        form.add(left(UiFactory.fieldLabel("DEMO ACCOUNTS  (click to fill)")));
        form.add(Box.createVerticalStrut(6));
        form.add(left(demoAccount("Admin", "admin", "Admin@123", Role.ADMIN)));
        form.add(left(demoAccount("Trainer", "trainer1", "Trainer@123", Role.TRAINER)));
        form.add(left(demoAccount("Member", "member1", "Member@123", Role.MEMBER)));
        form.add(Box.createVerticalStrut(14));
        form.add(left(statusLabel));

        outer.add(form);
        return outer;
    }

    /** Makes a component line up on the left inside a vertical BoxLayout. */
    private static JPanel left(Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.WEST);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        if (c instanceof JTextField) {
            p.add(c, BorderLayout.CENTER);                       // text fields stretch to the full width
        }
        return p;
    }

    private JLabel demoAccount(String roleText, String user, String pass, Role role) {
        JLabel l = UiFactory.label(roleText + ":  " + user + " / " + pass, 12, false, Theme.INFO);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        l.addMouseListener(new DemoAccountClick(l, user, pass, role));
        return l;
    }

    // ================================================================ events

    private void wireEvents() {
        // ActionListener as an anonymous inner class
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });

        // Show / hide the password: echo char 0 means "show the real characters"
        showPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : '\u2022');
            }
        });

        // KeyAdapter: we only override the one method we need
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocusInWindow();        // Enter moves to the password
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearForm();
                }
            }
        });

        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();                              // Enter submits the form
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    clearForm();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                warnIfCapsLock();
            }
        });

        // WindowAdapter: react only to the window-closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(LoginFrame.this,
                        "Do you want to exit Smart Gym?", "Exit", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                usernameField.requestFocusInWindow();
            }
        });
    }

    private void warnIfCapsLock() {
        try {
            boolean caps = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
            if (caps && messageLabel.getText().trim().isEmpty()) {
                showMessage("Caps Lock is ON", Theme.AMBER);
            } else if (!caps && "Caps Lock is ON".equals(messageLabel.getText())) {
                showMessage(" ", Theme.DANGER);
            }
        } catch (UnsupportedOperationException ex) {
            // some systems cannot report the key state - ignore
        }
    }

    private void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        showMessage(" ", Theme.DANGER);
        usernameField.requestFocusInWindow();
    }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    private Role selectedRole() {
        if (trainerRadio.isSelected()) {
            return Role.TRAINER;
        }
        return memberRadio.isSelected() ? Role.MEMBER : Role.ADMIN;
    }

    // ================================================================ actions

    private void attemptLogin() {
        if (!loginButton.isEnabled()) {
            return;                                              // a login is already running
        }
        final String username = usernameField.getText();
        final String password = new String(passwordField.getPassword());
        final Role chosen = selectedRole();

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        showMessage(" ", Theme.DANGER);

        // Database work runs in a SwingWorker, so the window stays responsive.
        BackgroundTask.run(this, () -> {
            User user = authService.login(username, password);
            if (user.getRole() != chosen) {
                authService.logout();
                String roleName = capitalise(user.getRole().name());
                String article = "AEIOU".indexOf(roleName.charAt(0)) >= 0 ? "an" : "a";
                throw new AuthenticationException("This is " + article + " " + roleName
                        + " account. Please choose \"" + roleName + "\" above.");
            }
            return user;
        }, user -> openMainWindow(), ex -> {
            loginButton.setEnabled(true);
            loginButton.setText("Sign In");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            showMessage("<html>" + ex.getMessage() + "</html>", Theme.DANGER);
        });
    }

    private static String capitalise(String text) {
        return text.charAt(0) + text.substring(1).toLowerCase();
    }

    private void openMainWindow() {
        MainFrame main = new MainFrame();
        main.setVisible(true);
        dispose();
    }

    private void checkDatabaseInBackground() {
        BackgroundTask.run(this, DatabaseConnection::testConnection, ok -> {
            if (ok) {
                statusLabel.setForeground(Theme.SUCCESS);
                statusLabel.setText("\u25CF  Database connected");
            } else {
                statusLabel.setForeground(Theme.DANGER);
                statusLabel.setText("\u25CF  Database not reachable - check db.properties and MySQL");
            }
        }, ex -> {
            statusLabel.setForeground(Theme.DANGER);
            statusLabel.setText("\u25CF  Database check failed");
        });
    }

    // ================================================================ inner classes

    /** Named inner class: clicking a demo account fills the form; hovering underlines it. */
    private class DemoAccountClick extends MouseAdapter {
        private final JLabel label;
        private final String user;
        private final String pass;
        private final Role role;
        private final String plainText;

        DemoAccountClick(JLabel label, String user, String pass, Role role) {
            this.label = label;
            this.user = user;
            this.pass = pass;
            this.role = role;
            this.plainText = label.getText();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            usernameField.setText(user);
            passwordField.setText(pass);
            adminRadio.setSelected(role == Role.ADMIN);
            trainerRadio.setSelected(role == Role.TRAINER);
            memberRadio.setSelected(role == Role.MEMBER);
            showMessage(" ", Theme.DANGER);
            passwordField.requestFocusInWindow();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setText("<html><u>" + plainText + "</u></html>");
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setText(plainText);
        }
    }

    /** The dark gradient panel on the left, drawn with Java2D. */
    private static class BrandPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        BrandPanel() {
            setOpaque(false);                // we paint the whole background ourselves in paintComponent
            setLayout(new BorderLayout());
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 30));

            JLabel logo = new JLabel(IconFactory.get("workout", 56, Theme.ACCENT));
            logo.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel name = UiFactory.label("SMART GYM", 38, true, Color.WHITE);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel tag = UiFactory.label("Management System", 18, false, new Color(0xFDBA74));
            tag.setAlignmentX(Component.LEFT_ALIGNMENT);

            text.add(Box.createVerticalGlue());
            text.add(logo);
            text.add(Box.createVerticalStrut(18));
            text.add(name);
            text.add(Box.createVerticalStrut(4));
            text.add(tag);
            text.add(Box.createVerticalStrut(34));
            String[] features = {"Members & memberships", "Trainer scheduling", "Attendance & payments", "Reports & analytics"};
            for (String f : features) {
                JLabel l = UiFactory.label("  " + f, 15, false, new Color(0xE5E7EB));
                l.setIcon(IconFactory.get("check", 16, Theme.ACCENT));
                l.setIconTextGap(8);
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
                l.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                text.add(l);
            }
            text.add(Box.createVerticalGlue());
            add(text, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(0x0B1220), 0, getHeight(), new Color(0x1F2937)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // decorative circles
            g2.setColor(new Color(249, 115, 22, 40));
            g2.fillOval(getWidth() - 170, -70, 280, 280);
            g2.setColor(new Color(249, 115, 22, 26));
            g2.fillOval(-90, getHeight() - 190, 300, 300);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(getWidth() - 110, getHeight() - 160, 150, 150);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Helper for starting the window safely on the Event Dispatch Thread. */
    public static void showLogin() {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
