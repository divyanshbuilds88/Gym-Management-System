package com.smartgym.gui.common;

import javax.swing.BorderFactory;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.Dimension;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** A date picker: a spinner showing "08 Nov 2026" with up/down arrows (and typing). Works with LocalDate. */
public class DateSpinner extends JSpinner {

    private static final long serialVersionUID = 1L;

    public DateSpinner(LocalDate initial) {
        super(new SpinnerDateModel(toDate(initial), null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(this, "dd MMM yyyy");
        setEditor(editor);
        editor.getTextField().setFont(Theme.regular(14));
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        setPreferredSize(new Dimension(150, 38));
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public LocalDate getDate() {
        Date d = (Date) getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setDate(LocalDate date) {
        setValue(toDate(date));
    }
}
