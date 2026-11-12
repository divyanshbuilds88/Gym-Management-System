package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.Attendance;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceDAO {

    /** Finds the member's open visit (no check-out yet) and LOCKS the row until the transaction ends. */
    Attendance findOpenCheckInForUpdate(Connection con, int memberId) throws DAOException;

    int checkIn(Connection con, int memberId, LocalDateTime when) throws DAOException;

    boolean checkOut(Connection con, int attendanceId, LocalDateTime when) throws DAOException;

    List<Attendance> findByDate(LocalDate date) throws DAOException;

    List<Attendance> findByMember(int memberId) throws DAOException;

    /** Search by member name or member code. */
    List<Attendance> searchByMember(String keyword) throws DAOException;

    List<Attendance> findBetween(LocalDate from, LocalDate to) throws DAOException;
}
