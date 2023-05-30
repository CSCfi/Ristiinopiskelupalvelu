package fi.uta.ristiinopiskelu.handler.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidSearchParametersException extends RistiinopiskeluException {
    public InvalidSearchParametersException(String message) {
        super(message);
    }
}
