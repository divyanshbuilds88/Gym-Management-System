package com.smartgym;

import com.smartgym.gui.common.Theme;
import com.smartgym.gui.login.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Smart Gym Management System - program entry point.
 *
 * All Swing windows must be created on the Event Dispatch Thread (EDT),
 * so the first window is opened inside SwingUtilities.invokeLater(...).
 * (To test only the database layer, run demo/Phase2Test or demo/Phase3Test instead.)
 */
public class Main {

    public static void main(String[] args) {
        // crisper text on most systems
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    // keep the default look and feel
                }
                Theme.applyGlobalDefaults();
                new LoginFrame().setVisible(true);
            }
        });
    }
}
