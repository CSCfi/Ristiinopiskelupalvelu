package fi.csc.ristiinopiskelu.admin.controller.advice;

import fi.csc.ristiinopiskelu.admin.exception.EntityNotFoundException;
import fi.csc.ristiinopiskelu.admin.exception.MessageSendingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class AdminUiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminUiExceptionHandler.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ EntityNotFoundException.class })
    public void handleEntityNotFoundException(EntityNotFoundException ex) {
        logger.error("Entity not found", ex);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ MessageSendingFailedException.class })
    public void handleMessageSendingFailedException(MessageSendingFailedException e) {
        logger.error("Message sending failed", e);
    }
}
