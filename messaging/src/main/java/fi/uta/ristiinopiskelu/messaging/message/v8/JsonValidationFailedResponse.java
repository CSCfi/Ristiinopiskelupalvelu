package fi.uta.ristiinopiskelu.messaging.message.v8;

import fi.uta.ristiinopiskelu.messaging.message.v8.AbstractResponse;
import fi.uta.ristiinopiskelu.messaging.message.v8.Status;

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
