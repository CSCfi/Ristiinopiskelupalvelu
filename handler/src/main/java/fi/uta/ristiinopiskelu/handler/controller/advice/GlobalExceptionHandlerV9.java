package fi.uta.ristiinopiskelu.handler.controller.advice;

import fi.uta.ristiinopiskelu.handler.exception.FindFailedException;
import fi.uta.ristiinopiskelu.handler.exception.InvalidSearchParametersException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice(basePackages = "fi.uta.ristiinopiskelu.handler.controller.v9")
public class GlobalExceptionHandlerV9 extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerV9.class);

    @ExceptionHandler({ InvalidSearchParametersException.class, IllegalArgumentException.class })
    public ResponseEntity<ErrorResponseBody> handleInvalidSearchParametersException(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseBody(ex.getMessage()));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ FindFailedException.class })
    public void handleFindFailedException(FindFailedException e, HttpServletRequest req) {
        logger.error("Error while processing request '{}'", req.getRequestURI(), e);
    }
    
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ Exception.class })
    public void handleAllUncaughtException(Exception e, HttpServletRequest req) {
        logger.error("Error while processing request '{}'", req.getRequestURI(), e);
    }
}

