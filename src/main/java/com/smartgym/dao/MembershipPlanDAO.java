package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.MembershipPlan;

import java.util.List;

public interface MembershipPlanDAO extends CrudDAO<MembershipPlan> {
    /** Only plans that are switched on (is_active = 1). */
    List<MembershipPlan> findAllActive() throws DAOException;
}
