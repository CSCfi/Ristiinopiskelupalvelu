package fi.uta.ristiinopiskelu.handler.processor;

public class DefaultResponseProcessor extends AbstractResponseProcessor {

    private String message;

    public DefaultResponseProcessor(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
