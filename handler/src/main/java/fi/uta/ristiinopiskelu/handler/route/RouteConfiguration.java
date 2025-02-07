package fi.uta.ristiinopiskelu.handler.route;

import fi.uta.ristiinopiskelu.handler.processor.AbstractErrorProcessor;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import org.apache.camel.Processor;

public class RouteConfiguration {

    private final String from;
    private final MessageType requestType;
    private final MessageType responseType;
    private final Processor requestProcessor;
    private final AbstractErrorProcessor exceptionProcessor;
    private final AbstractErrorProcessor jsonValidationExceptionProcessor;
    private final String messageSchemaFilename;
    private final int minThreads;
    private final int maxThreads;
    private final String threadName;

    // If new route is created in a current version, set this to true (since there is no backward-compatible route)
    // If this is left false and no previous message schema is found, software does not start.
    private final boolean routeBackwardsCompatible;

    public RouteConfiguration(String from, String messageSchemaFilename, MessageType requestType, MessageType responseType,
                              Processor requestProcessor, AbstractErrorProcessor exceptionProcessor, AbstractErrorProcessor jsonValidationExceptionProcessor,
                              boolean routeBackwardsCompatible) {
        this(from, messageSchemaFilename, requestType, responseType, requestProcessor, exceptionProcessor, jsonValidationExceptionProcessor,
            routeBackwardsCompatible, AbstractRoute.DEFAULT_MIN_THREADS, AbstractRoute.DEFAULT_MAX_THREADS);
    }

    public RouteConfiguration(String from, String messageSchemaFilename, MessageType requestType, MessageType responseType,
                              Processor requestProcessor, AbstractErrorProcessor exceptionProcessor, AbstractErrorProcessor jsonValidationExceptionProcessor,
                              boolean routeBackwardsCompatible, int minThreads, int maxThreads) {
        this.from = from;
        this.requestType = requestType;
        this.responseType = responseType;
        this.requestProcessor = requestProcessor;
        this.exceptionProcessor = exceptionProcessor;
        this.messageSchemaFilename = messageSchemaFilename;
        this.jsonValidationExceptionProcessor = jsonValidationExceptionProcessor;
        this.routeBackwardsCompatible = routeBackwardsCompatible;
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
        return routeBackwardsCompatible;
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
