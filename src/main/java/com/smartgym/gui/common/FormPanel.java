package com.smartgym.gui.common;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** A vertical form: each field has a small caption above it. Uses GridBagLayout so fields stretch. */
public class FormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private int row = 0;

    public FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    private GridBagConstraints constraints(int x, int width, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = row;
        c.gridwidth = width;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = insets;
        return c;
    }

    /** One caption + one field, full width. */
    public void addField(String caption, JComponent field) {
        add(UiFactory.fieldLabel(caption), constraints(0, 2, new Insets(10, 0, 4, 0)));
        row++;
        add(field, constraints(0, 2, new Insets(0, 0, 0, 0)));
        row++;
    }

    /** Two captions + two fields side by side. */
    public void addFieldPair(String caption1, JComponent field1, String caption2, JComponent field2) {
        add(UiFactory.fieldLabel(caption1), constraints(0, 1, new Insets(10, 0, 4, 8)));
        add(UiFactory.fieldLabel(caption2), constraints(1, 1, new Insets(10, 8, 4, 0)));
        row++;
        add(field1, constraints(0, 1, new Insets(0, 0, 0, 8)));
        add(field2, constraints(1, 1, new Insets(0, 8, 0, 0)));
        row++;
    }

    /** A component without a caption (for example a check box). */
    public void addComponent(JComponent component) {
        add(component, constraints(0, 2, new Insets(8, 0, 0, 0)));
        row++;
    }

    /** A thin line with a section title. */
    public void addSection(String title) {
        JPanel box = new JPanel(new java.awt.BorderLayout(0, 6));
        box.setOpaque(false);
        box.add(new JSeparator(), java.awt.BorderLayout.NORTH);
        JLabel label = UiFactory.label(title, 14, true, Theme.TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        box.add(label, java.awt.BorderLayout.CENTER);
        add(box, constraints(0, 2, new Insets(18, 0, 0, 0)));
        row++;
    }
}
