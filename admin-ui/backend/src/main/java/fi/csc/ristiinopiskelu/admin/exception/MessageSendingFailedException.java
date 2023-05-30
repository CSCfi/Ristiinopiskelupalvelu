package fi.csc.ristiinopiskelu.admin.exception;

public class MessageSendingFailedException extends RuntimeException {
    public MessageSendingFailedException(String message) {
        super(message);
    }

    public MessageSendingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
