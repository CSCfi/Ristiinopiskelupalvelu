package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.handler.processor.AbstractErrorProcessor;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.Processor;

public class RouteConfiguration {

    private String from;
    private MessageType requestType;
    private MessageType responseType;
    private Processor requestProcessor;
    private AbstractErrorProcessor exceptionProcessor;
    private AbstractErrorProcessor jsonValidationExceptionProcessor;
    private String messageSchemaFilename;
    private int minThreads = AbstractRoute.DEFAULT_MIN_THREADS;
    private int maxThreads = AbstractRoute.DEFAULT_MAX_THREADS;
    private String threadName;

    // If new route is created in a current version, set this to true (since there is no backward-compatible route)
    // If this is left false and no previous message schema is found, software does not start.
    private boolean isRouteBackwardsCompatible;

    public RouteConfiguration(String from, String messageSchemaFilename, MessageType requestType, MessageType responseType,
                              Processor requestProcessor, AbstractErrorProcessor exceptionProcessor, AbstractErrorProcessor jsonValidationExceptionProcessor,
                              boolean isRouteBackwardsCompatible) {
        this.from = from;
        this.requestType = requestType;
        this.responseType = responseType;
        this.requestProcessor = requestProcessor;
        this.exceptionProcessor = exceptionProcessor;
        this.messageSchemaFilename = messageSchemaFilename;
        this.jsonValidationExceptionProcessor = jsonValidationExceptionProcessor;
        this.isRouteBackwardsCompatible = isRouteBackwardsCompatible;
    }

    public RouteConfiguration(String from, String messageSchemaFilename, MessageType requestType, MessageType responseType,
                              Processor requestProcessor, AbstractErrorProcessor exceptionProcessor, AbstractErrorProcessor jsonValidationExceptionProcessor,
                              boolean isRouteBackwardsCompatible, int minThreads, int maxThreads) {
        this.from = from;
        this.requestType = requestType;
        this.responseType = responseType;
        this.requestProcessor = requestProcessor;
        this.exceptionProcessor = exceptionProcessor;
        this.messageSchemaFilename = messageSchemaFilename;
        this.jsonValidationExceptionProcessor = jsonValidationExceptionProcessor;
        this.isRouteBackwardsCompatible = isRouteBackwardsCompatible;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.threadName = requestType.name();
    }

    public String getFrom() {
        return from;
    }

    public MessageType getRequestType() {
        return requestType;
    }

    public MessageType getResponseType() {
        return responseType;
    }

    public Processor getRequestProcessor() {
        return requestProcessor;
    }

    public String getMessageSchemaFilename() {
        return messageSchemaFilename;
    }

    public boolean isRouteBackwardsCompatible() {
        return isRouteBackwardsCompatible;
    }

    public void setRouteBackwardsCompatible(boolean routeBackwardsCompatible) {
        isRouteBackwardsCompatible = routeBackwardsCompatible;
    }

    public AbstractErrorProcessor getJsonValidationExceptionProcessor() {
        return jsonValidationExceptionProcessor;
    }

    public AbstractErrorProcessor getExceptionProcessor() {
        return exceptionProcessor;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public String getThreadName() {
        return threadName;
    }
}
