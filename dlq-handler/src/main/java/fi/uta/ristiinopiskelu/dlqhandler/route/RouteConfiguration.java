package fi.uta.ristiinopiskelu.dlqhandler.route;

import fi.uta.ristiinopiskelu.dlqhandler.processor.DeadLetterQueueProcessor;

public class RouteConfiguration {
    private String organisationQueue;

    private DeadLetterQueueProcessor deadLetterQueueProcessor;

    public RouteConfiguration(String organisationQueue, DeadLetterQueueProcessor deadLetterQueueProcessor) {
        this.organisationQueue = organisationQueue;
        this.deadLetterQueueProcessor = deadLetterQueueProcessor;
    }

    public String getOrganisationQueue() {
        return organisationQueue;
    }

    public DeadLetterQueueProcessor getDeadLetterQueueProcessor() {
        return deadLetterQueueProcessor;
    }
}
