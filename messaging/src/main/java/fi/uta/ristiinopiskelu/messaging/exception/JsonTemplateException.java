package fi.uta.ristiinopiskelu.messaging.exception;

public class JsonTemplateException extends Exception {
    public JsonTemplateException(String message) {
        super(message);
    }

    public JsonTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
