package com.smartgym.dao;

import com.smartgym.exception.DAOException;

import java.util.List;

/**
 * Generic parent interface with the five basic database operations (CRUD).
 * Other DAO interfaces EXTEND this one (interface inheritance) and add their own methods.
 * T is a generic type: CrudDAO<Member> works with Member objects.
 */
public interface CrudDAO<T> {
    /** Saves a new row and returns the generated primary key. */
    int insert(T item) throws DAOException;

    boolean update(T item) throws DAOException;

    boolean delete(int id) throws DAOException;

    /** Returns the object, or null when no row has this id. */
    T findById(int id) throws DAOException;

    List<T> findAll() throws DAOException;
}
