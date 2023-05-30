package fi.uta.ristiinopiskelu.handler.controller.advice;

public class ErrorResponseBody {

    String message;

    public ErrorResponseBody(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
