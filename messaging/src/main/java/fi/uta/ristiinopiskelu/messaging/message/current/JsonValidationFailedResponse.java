package fi.uta.ristiinopiskelu.messaging.message.current;

import java.util.List;

public class JsonValidationFailedResponse extends AbstractResponse {

    private List<String> errors;

    public JsonValidationFailedResponse(String message, List<String> errors) {
        super(Status.FAILED, message);
        this.errors = errors;
    }

    public JsonValidationFailedResponse() {}

    public List<String> getErrors() {
        return errors;
    }
}
