package com.nexocrm.service;

/** Violación de una regla de negocio (se traduce a HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
