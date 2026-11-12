package com.smartgym.gui.profile;

import com.smartgym.gui.common.AvatarLabel;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.ScrollPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Member;
import com.smartgym.model.Person;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.User;
import com.smartgym.service.ProfileService;
import com.smartgym.service.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Profile screen for every role (admin, trainer, member).
 * Tab 1: account information (read-only) and editable contact details.
 * Tab 2: change password, with a live password-strength hint (KeyAdapter).
 */
public class ProfilePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final transient ProfileService service = new ProfileService();

    // header
    private final AvatarLabel avatar = new AvatarLabel("?", 72, Theme.ACCENT);
    private final JLabel nameLabel = UiFactory.label(" ", 22, true, Theme.TEXT);
    private final JLabel roleLabel = UiFactory.label(" ", 13, true, Theme.ACCENT_DARK);
    private final JLabel usernameLabel = UiFactory.muted(" ");

    // account info (read-only rows are rebuilt for each role)
    private final JPanel infoGrid = new JPanel(new GridLayout(0, 2, 12, 10));

    // contact form
    private final JTextField phoneField = UiFactory.textField(16);
    private final JTextField emailField = UiFactory.textField(16);
    private final JTextArea addressArea = new JTextArea(3, 16);
    private final RoundedButton saveButton = new RoundedButton("Save Changes", Theme.ACCENT);

    // password form
    private final JPasswordField oldPassword = UiFactory.passwordField(16);
    private final JPasswordField newPassword = UiFactory.passwordField(16);
    private final JPasswordField confirmPassword = UiFactory.passwordField(16);
    private final JCheckBox showPasswords = new JCheckBox("Show passwords");
    private final JLabel strengthLabel = UiFactory.label(" ", 12, true, Theme.MUTED);
    private final RoundedButton passwordButton = new RoundedButton("Change Password", Theme.ACCENT);

    public ProfilePanel() {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        ScrollPanel content = new ScrollPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(buildHeaderCard());
        stack.add(Box.createVerticalStrut(16));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bold(13));
        tabs.setBackground(Color.WHITE);
        tabs.addTab("My Details", buildDetailsTab());
        tabs.addTab("Change Password", buildPasswordTab());
        tabs.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(tabs);

        content.add(stack, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        wireEvents();
    }

    // ================================================================ layout

    private JPanel buildHeaderCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(avatar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(nameLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(roleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(usernameLabel);
        text.add(Box.createVerticalGlue());
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDetailsTab() {
        JPanel tab = new JPanel(new GridLayout(1, 2, 16, 0));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // left: account information
        RoundedPanel info = new RoundedPanel(new BorderLayout(0, 12), new Color(0xF9FAFB), 14);
        info.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        info.add(UiFactory.label("Account information", 15, true, Theme.TEXT), BorderLayout.NORTH);
        infoGrid.setOpaque(false);
        JPanel gridHolder = new JPanel(new BorderLayout());
        gridHolder.setOpaque(false);
        gridHolder.add(infoGrid, BorderLayout.NORTH);
        info.add(gridHolder, BorderLayout.CENTER);
        tab.add(info);

        // right: contact details form
        RoundedPanel form = new RoundedPanel(new GridBagLayout(), new Color(0xF9FAFB), 14);
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 6, 0);

        c.gridy = 0;
        form.add(UiFactory.label("Contact details", 15, true, Theme.TEXT), c);
        c.gridy = 1;
        c.insets = new Insets(8, 0, 4, 0);
        form.add(UiFactory.fieldLabel("PHONE"), c);
        c.gridy = 2;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(phoneField, c);
        c.gridy = 3;
        c.insets = new Insets(8, 0, 4, 0);
        form.add(UiFactory.fieldLabel("EMAIL"), c);
        c.gridy = 4;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(emailField, c);
        c.gridy = 5;
        c.insets = new Insets(8, 0, 4, 0);
        form.add(UiFactory.fieldLabel("ADDRESS (members only)"), c);

        addressArea.setFont(Theme.regular(14));
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        addressArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane addressScroll = UiFactory.scroll(addressArea);
        c.gridy = 6;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(addressScroll, c);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(saveButton);
        c.gridy = 7;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(buttonRow, c);
        tab.add(form);
        return tab;
    }

    private JPanel buildPasswordTab() {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tab.setBackground(Color.WHITE);
        tab.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        RoundedPanel form = new RoundedPanel(new GridBagLayout(), new Color(0xF9FAFB), 14);
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        form.setPreferredSize(new Dimension(400, 360));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int row = 0;
        c.gridy = row++;
        c.insets = new Insets(0, 0, 8, 0);
        form.add(UiFactory.label("Change your password", 15, true, Theme.TEXT), c);
        c.gridy = row++;
        c.insets = new Insets(8, 0, 4, 0);
        form.add(UiFactory.fieldLabel("CURRENT PASSWORD"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(oldPassword, c);
        c.gridy = row++;
        c.insets = new Insets(8, 0, 4, 0);
        form.add(UiFactory.fieldLabel("NEW PASSWORD  (6+ characters, a letter and a digit)"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 2, 0);
        form.add(newPassword, c);
        c.gridy = row++;
        form.add(strengthLabel, c);
        c.gridy = row++;
        c.insets = new Insets(8, 0, 4, 0);
        form.add(UiFactory.fieldLabel("CONFIRM NEW PASSWORD"), c);
        c.gridy = row++;
        c.insets = new Insets(0, 0, 8, 0);
        form.add(confirmPassword, c);
        showPasswords.setOpaque(false);
        showPasswords.setFocusPainted(false);
        c.gridy = row++;
        form.add(showPasswords, c);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(passwordButton);
        c.gridy = row;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(buttonRow, c);

        tab.add(form);
        return tab;
    }

    // ================================================================ events

    private void wireEvents() {
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveContact();
            }
        });
        passwordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changePassword();
            }
        });
        showPasswords.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                char echo = showPasswords.isSelected() ? (char) 0 : '\u2022';
                oldPassword.setEchoChar(echo);
                newPassword.setEchoChar(echo);
                confirmPassword.setEchoChar(echo);
            }
        });
        // Live feedback while typing the new password
        newPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateStrengthHint(new String(newPassword.getPassword()));
            }
        });
    }

    private void updateStrengthHint(String password) {
        if (password.isEmpty()) {
            strengthLabel.setText(" ");
            return;
        }
        boolean letter = false;
        boolean digit = false;
        boolean other = false;
        for (char ch : password.toCharArray()) {
            if (Character.isLetter(ch)) {
                letter = true;
            } else if (Character.isDigit(ch)) {
                digit = true;
            } else {
                other = true;
            }
        }
        int score = (password.length() >= 6 ? 1 : 0) + (password.length() >= 10 ? 1 : 0)
                + (letter && digit ? 1 : 0) + (other ? 1 : 0);
        if (score <= 1) {
            strengthLabel.setForeground(Theme.DANGER);
            strengthLabel.setText("Weak");
        } else if (score == 2) {
            strengthLabel.setForeground(Theme.AMBER);
            strengthLabel.setText("Okay");
        } else {
            strengthLabel.setForeground(Theme.SUCCESS);
            strengthLabel.setText("Strong");
        }
    }

    // ================================================================ data

    @Override
    public void refreshData() {
        BackgroundTask.run(this, service::getProfile, this::display);
    }

    private void display(Person p) {
        User user = SessionManager.getInstance().getCurrentUser();
        nameLabel.setText(p.getFullName());
        roleLabel.setText(p.getRoleName());
        usernameLabel.setText("Username: " + user.getUsername());
        avatar.setPerson(p.getFullName());

        infoGrid.removeAll();
        addInfo("Full name", p.getFullName());
        addInfo("Username", user.getUsername());
        addInfo("Role", p.getRoleName());

        // The object is really a Member, a Trainer or a User, so show the fields that fit.
        if (p instanceof Member) {
            Member m = (Member) p;
            addInfo("Member code", m.getMemberCode());
            addInfo("Age / Gender", m.getAge() + " / " + m.getGender());
            addInfo("Joined", m.getJoiningDate().format(DATE_FORMAT));
            addInfo("Plan", m.getPlanName() == null ? "No plan yet" : m.getPlanName());
            addInfo("Expires", m.getMembershipExpiry() == null ? "-" : m.getMembershipExpiry().format(DATE_FORMAT));
            addInfo("Membership", m.getMembershipStatus());
            setAddressEditable(true, m.getAddress() == null ? "" : m.getAddress());
        } else if (p instanceof Trainer) {
            Trainer t = (Trainer) p;
            addInfo("Specialization", t.getSpecialization());
            addInfo("Availability", t.getAvailability());
            addInfo("Experience", t.getExperienceYears() + " years");
            setAddressEditable(false, "");
        } else {
            addInfo("Access level", user.getRole() == Role.ADMIN ? "Full access" : "Limited");
            setAddressEditable(false, "");
        }
        infoGrid.revalidate();
        infoGrid.repaint();

        phoneField.setText(p.getPhone());
        emailField.setText(p.getEmail());
    }

    /** Members can edit their address; for other accounts the box is greyed out. */
    private void setAddressEditable(boolean editable, String text) {
        addressArea.setEnabled(editable);
        addressArea.setBackground(editable ? Color.WHITE : new Color(0xF3F4F6));
        addressArea.setDisabledTextColor(Theme.MUTED);
        addressArea.setText(editable ? text : "Not applicable for this account type");
    }

    private void addInfo(String label, String value) {
        infoGrid.add(UiFactory.label(label, 13, false, Theme.MUTED));
        infoGrid.add(UiFactory.label(value == null ? "-" : value, 13, true, Theme.TEXT));
    }

    private void saveContact() {
        final String phone = phoneField.getText();
        final String email = emailField.getText();
        final String address = addressArea.isEnabled() ? addressArea.getText().trim() : null;
        saveButton.setEnabled(false);
        BackgroundTask.run(this, () -> {
            service.updateContact(phone, email, address);
            return Boolean.TRUE;
        }, ok -> {
            saveButton.setEnabled(true);
            BackgroundTask.showInfo(this, "Your contact details were updated.");
            refreshData();
        }, ex -> {
            saveButton.setEnabled(true);
            BackgroundTask.showError(this, ex);
        });
    }

    private void changePassword() {
        final String oldPw = new String(oldPassword.getPassword());
        final String newPw = new String(newPassword.getPassword());
        final String confirmPw = new String(confirmPassword.getPassword());
        passwordButton.setEnabled(false);
        BackgroundTask.run(this, () -> {
            service.changePassword(oldPw, newPw, confirmPw);
            return Boolean.TRUE;
        }, ok -> {
            passwordButton.setEnabled(true);
            oldPassword.setText("");
            newPassword.setText("");
            confirmPassword.setText("");
            strengthLabel.setText(" ");
            BackgroundTask.showInfo(this, "Your password was changed. Use the new password next time you log in.");
        }, ex -> {
            passwordButton.setEnabled(true);
            BackgroundTask.showError(this, ex);
        });
    }
}
