package com.tradevalidator.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Result of one validation evaludation
 */
public class ValidationResult {

    private Collection<ValidationError> errors;

    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Collection<ValidationError> errors() {
        return errors;
    }

    public ValidationResult withError(ValidationError error) {
        errors.add(error);
        return this;
    }


    @Override
    public String toString() {
        return "ValidationResult{" +
                "errors=" + errors +
                '}';
    }
}
