package com.smartgym.service;

import java.util.ArrayList;
import java.util.List;

/**
 * A report as plain data: a title, column headings and rows of text.
 * The GUI shows it in a JTable; ReportService can write it to a CSV file.
 */
public class ReportTable {

    private final String title;
    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public ReportTable(String title, String... headers) {     // varargs
        this.title = title;
        this.headers = headers;
    }

    /** Adds one row; the number of cells must match the number of headers. */
    public void addRow(String... cells) {
        if (cells.length != headers.length) {
            throw new IllegalArgumentException("Row has " + cells.length + " cells but the report has "
                    + headers.length + " columns");                // unchecked exception
        }
        rows.add(cells);
    }

    public String getTitle()          { return title; }
    public String[] getHeaders()      { return headers; }
    public List<String[]> getRows()   { return rows; }
    public int getRowCount()          { return rows.size(); }
}
