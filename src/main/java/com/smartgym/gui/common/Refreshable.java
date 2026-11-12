package com.smartgym.gui.common;

/** Implemented by screens that reload their data from the database whenever they are shown (or F5 is pressed). */
public interface Refreshable {
    void refreshData();
}
