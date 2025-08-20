package com.tradevalidator.validators;

import com.tradevalidator.validator.TradeValidator;
import com.tradevalidator.model.Trade;
import com.tradevalidator.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static com.tradevalidator.model.ValidationResult.validationResult;
import static com.tradevalidator.model.ValidationError.validationError;

/**
 * Validator which checks business rule:"value date cannot be before trade date"
 */
@Component
public class TradeDateValueValidator implements TradeValidator{

    private static Logger LOG = LoggerFactory.getLogger(TradeDateValueValidator.class);


    @Override
    public ValidationResult validate(Trade trade) {

        ValidationResult validationResult = validationResult();

        if (trade.getValueDate() == null) {
            LOG.warn("Value date is missing for trade {}", trade);
            validationResult.withError(
                    validationError().field("valueDate").message("valueDate is missing")
            );
        }

        if (trade.getTradeDate() == null) {
            LOG.warn("Trade date is missing for trade {}", trade);
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
