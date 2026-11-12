package com.smartgym.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/** Small helper methods shared by all DAO classes. */
public final class DbUtil {

    private DbUtil() { }

    public static void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do
            }
        }
    }

    public static void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
            } catch (SQLException ignored) { }
            try {
                con.close();
            } catch (SQLException ignored) { }
        }
    }

    public static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    public static void setNullableDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setObject(index, value);
        }
    }

    /** Reads an INT column that may be NULL. */
    public static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Turns "abc" into "%abc%" for LIKE searches. */
    public static String like(String keyword) {
        String k = (keyword == null) ? "" : keyword.trim();
        return "%" + k + "%";
    }
}
