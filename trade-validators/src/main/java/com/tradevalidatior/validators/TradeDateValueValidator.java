package com.tradevalidatior.validators;

import com.tradevalidatior.validator.TradeValidator;
import com.tradevalidator.model.Trade;
import com.tradevalidator.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tradevalidator.model.ValidationResult.newValidationResult;
import static com.tradevalidator.model.ValidationError.validationError;

/**
 * Validator which checks business rule:"value date cannot be before trade date"
 */
public class TradeDateValueValidator implements TradeValidator{

    private static Logger LOG = LoggerFactory.getLogger(TradeDateValueValidator.class);


    @Override
    public ValidationResult validate(Trade trade) {

        ValidationResult validationResult = newValidationResult();

        if (trade.getValueDate() == null) {
            validationResult.withError(
                    validationError().field("valueDate").message("valueDate is missing")
            );
        }

        if (trade.getTradeDate() == null) {
            validationResult.withError(
                    validationError().field("tradeDate").message("tradeDate is missing")
            );
        }

        if (!validationResult.hasErrors()) {
            if (trade.getValueDate().before(trade.getTradeDate())) {
                LOG.warn("Value date cannot be before trade date for trade {}", trade);
                validationResult.withError(
                        validationError().field("valueDate").message("Value date cannot be before Trade date")
                );
            }
        }

        return validationResult;
    }
}
