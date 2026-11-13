package fi.uta.ristiinopiskelu.handler.exception;

import fi.uta.ristiinopiskelu.messaging.message.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidSearchParametersException extends RistiinopiskeluException {
    public InvalidSearchParametersException(String message) {
        super(message, ErrorCode.INVALID_SEARCH_PARAMETERS);
    }
}
