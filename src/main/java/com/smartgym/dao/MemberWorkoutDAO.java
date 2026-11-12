package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.MemberWorkout;

import java.util.List;

public interface MemberWorkoutDAO {
    int assign(MemberWorkout assignment) throws DAOException;

    List<MemberWorkout> findAll() throws DAOException;

    List<MemberWorkout> findByMember(int memberId) throws DAOException;

    List<MemberWorkout> findByTrainer(int trainerId) throws DAOException;

    boolean updateStatus(int assignmentId, String status) throws DAOException;

    boolean delete(int assignmentId) throws DAOException;
}
