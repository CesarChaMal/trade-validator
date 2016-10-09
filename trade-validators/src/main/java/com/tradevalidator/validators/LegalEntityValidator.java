package com.tradevalidator.validators;

import com.tradevalidator.validator.TradeValidator;
import com.tradevalidator.model.Trade;
import com.tradevalidator.model.ValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tradevalidator.model.ValidationError.validationError;


/**
 * Only one legal entity is used: CS Zurich
 */
@ManagedResource(objectName = "TradeValidators:name=LegalEntityValidator", description = "Legal entity validation")
@Component
public class LegalEntityValidator implements TradeValidator {

    @Value("${validator.legalEntities}")
    private Set<String> legalEntities = new HashSet<>();

    public LegalEntityValidator() {
        legalEntities.add("CS Zurich");
    }

    @Override
    public ValidationResult validate(Trade trade) {

        ValidationResult validationResult = ValidationResult.validationResult();

        if(!legalEntities.contains(trade.getLegalEntity())) {
            validationResult.withError(validationError().field("legalEntity").message("Legal entity is invalid"));
        }

        return validationResult;
    }

    @ManagedAttribute(description = "Get legal Entities")
    public Set<String> getLegalEntities() {
        return legalEntities;
    }

    public void setLegalEntities(Set<String> legalEntities) {
        this.legalEntities = legalEntities;
    }

    @ManagedOperation(description = "Load valid legalEntities")
    @ManagedOperationParameters(
            @ManagedOperationParameter(name = "value", description = "Comma separated list")
    )
    public String loadValidLegalEntities(String value) {
        legalEntities = new HashSet<>(Arrays.asList(value.split(",")));
        return legalEntities.toString();
    }
}
