package fi.uta.ristiinopiskelu.handler.controller.advice;

import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(basePackages = "fi.uta.ristiinopiskelu.handler.controller.v8")
public class GlobalExceptionHandlerV8 {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerV8.class);

    @ExceptionHandler({ InvalidSearchParametersException.class })
    public ResponseEntity<ErrorResponseBody> handleBadRequestException(InvalidSearchParametersException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseBody(ex.getMessage()));
    }
    
    @ExceptionHandler({ EntityNotFoundException.class })
    public ResponseEntity<List> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    /** Provides handling for exceptions throughout this service. */
    @ExceptionHandler({ Exception.class, NoNodeAvailableException.class, FindFailedException.class })
    public ResponseEntity handleException(HttpServletRequest req, Exception ex) {
        logger.error("Error while processing request '{}'", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
