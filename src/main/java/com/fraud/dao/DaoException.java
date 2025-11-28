package com.fraud.dao;

/**
 * Simple unchecked exception for DAO errors.
 */
public class DaoException extends RuntimeException {
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
    public DaoException(String message) {
        super(message);
    }
}