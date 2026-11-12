package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.TrainerSchedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TrainerScheduleDAO extends CrudDAO<TrainerSchedule> {

    /**
     * True if the trainer already has a non-cancelled session that overlaps start-end on that date.
     * excludeScheduleId is the session being edited (use 0 when adding a new one).
     */
    boolean hasTrainerConflict(int trainerId, LocalDate date, LocalTime start, LocalTime end, int excludeScheduleId) throws DAOException;

    boolean hasMemberConflict(int memberId, LocalDate date, LocalTime start, LocalTime end, int excludeScheduleId) throws DAOException;

    List<TrainerSchedule> findByTrainer(int trainerId) throws DAOException;

    List<TrainerSchedule> findByMember(int memberId) throws DAOException;

    List<TrainerSchedule> findByDate(LocalDate date) throws DAOException;

    /** Next scheduled sessions from now on (dashboard). */
    List<TrainerSchedule> findUpcoming(int limit) throws DAOException;

    boolean updateStatus(int scheduleId, String status) throws DAOException;
}
