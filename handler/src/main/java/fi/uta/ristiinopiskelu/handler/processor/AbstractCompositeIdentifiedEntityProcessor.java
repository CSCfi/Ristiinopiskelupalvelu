package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Notification;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public abstract class AbstractCompositeIdentifiedEntityProcessor<T extends CompositeIdentifiedEntity> implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCompositeIdentifiedEntityProcessor.class);

    private final NetworkService networkService;
    private final OrganisationService organisationService;
    private final JmsMessageForwarder jmsMessageForwarder;
    private final MessageSchemaService messageSchemaService;

    public AbstractCompositeIdentifiedEntityProcessor(NetworkService networkService, OrganisationService organisationService,
                                                      JmsMessageForwarder jmsMessageForwarder, MessageSchemaService messageSchemaService) {
        this.networkService = networkService;
        this.organisationService = organisationService;
        this.jmsMessageForwarder = jmsMessageForwarder;
        this.messageSchemaService = messageSchemaService;
    }

    public void notifyNetworkMembers(String organizingOrganisationId, MessageType messageType, CompositeIdentifiedEntityModificationResult result) throws Exception {
        Assert.hasText(organizingOrganisationId, "Organizing organisation id must not be empty");
        Assert.notNull(messageType, "Message type must not be null");
        Assert.notNull(result, "Course unit creation result cannot be null");

        Map<String, CompositeIdentifiedEntityModifiedNotification> notifications = this.notificationsByOrganisation(organizingOrganisationId, result);

        if(CollectionUtils.isEmpty(notifications)) {
            logger.debug("No notifications to be sent, skipping");
            return;
        }

        this.sendNotifications(organizingOrganisationId, messageType, notifications);
    }

    private Map<String, CompositeIdentifiedEntityModifiedNotification> notificationsByOrganisation(String organizingOrganisationId,
                                                                                                   CompositeIdentifiedEntityModificationResult result) {
        java.util.Map<String, List<CompositeIdentifiedEntityReference>> createdReferencesByOrg = this.getReferencesByOrg(result.getCreated());
        java.util.Map<String, List<CompositeIdentifiedEntityReference>> updatedReferencesByOrg = this.getReferencesByOrg(result.getUpdated());
        java.util.Map<String, List<CompositeIdentifiedEntityReference>> deletedReferencesByOrg = this.getReferencesByOrg(result.getDeleted());
        java.util.Map<String, CompositeIdentifiedEntityModifiedNotification> notifications = new HashMap<>();

        List<String> allOrgs = new ArrayList<>();
        allOrgs.addAll(createdReferencesByOrg.keySet());
        allOrgs.addAll(updatedReferencesByOrg.keySet());
        allOrgs.addAll(deletedReferencesByOrg.keySet());

        for(String orgId : allOrgs) {
            notifications.put(orgId, new CompositeIdentifiedEntityModifiedNotification(organizingOrganisationId, OffsetDateTime.now(),
                createdReferencesByOrg.get(orgId), updatedReferencesByOrg.get(orgId), deletedReferencesByOrg.get(orgId)));
        }

        return notifications;
    }

    private java.util.Map<String, List<CompositeIdentifiedEntityReference>> getReferencesByOrg(List<? extends CompositeIdentifiedEntity> entities) {
        java.util.Map<String, List<CompositeIdentifiedEntityReference>> referencesByOrg = new HashMap<>();

        if(CollectionUtils.isEmpty(entities)) {
            return referencesByOrg;
        }

        for(CompositeIdentifiedEntity entity : entities) {

            if(entity == null) {
                logger.warn("Null entity found, skipping. This should never happen unless running unit tests for example.");
                continue;
            }

            List<CooperationNetwork> cooperationNetworks = entity.getCooperationNetworks();

            if(CollectionUtils.isEmpty(cooperationNetworks)) {
                logger.debug("No cooperation networks found for entity [id={}, type={}], skipping", entity.getId(), entity.getType());
                continue;
            }

            List<String> orgIds = networkService.findOrganisationIdsFromNetworks(cooperationNetworks.stream().map(cn -> cn.getId()).collect(Collectors.toList()));

            for(String orgId : orgIds) {
                List<CompositeIdentifiedEntityReference> refs = referencesByOrg.get(orgId);
                if (refs == null) {
                    refs = new ArrayList<>();
                }

                refs.add(new CompositeIdentifiedEntityReference(entity.getElementId(), entity.getOrganizingOrganisationId(), entity.getType()));
                referencesByOrg.put(orgId, refs);
            }
        }

        return referencesByOrg;
    }

    private void sendNotifications(String organizingOrganisationId, MessageType messageType, Map<String, ? extends Notification> notificationsByOrganisation) throws Exception {
        Assert.hasText(organizingOrganisationId, "Organizing organisation id must not be empty");
        Assert.notNull(messageType, "Message type must not be null");
        Assert.notEmpty(notificationsByOrganisation, "Notifications must not be empty");

        // don't send to self
        if(notificationsByOrganisation.containsKey(organizingOrganisationId)) {
            notificationsByOrganisation.remove(organizingOrganisationId);
        }

        if(notificationsByOrganisation.isEmpty()) {
            logger.debug("No organisations left to send notifications to, skipping", organizingOrganisationId);
            return;
        }

        for(Entry<String, ? extends Notification> entry : notificationsByOrganisation.entrySet()) {
            OrganisationEntity organisation = organisationService.findById(entry.getKey()).orElseThrow(() -> new EntityNotFoundException(OrganisationEntity.class, entry.getKey()));
            if(organisation.isNotificationsEnabled()) {
                try {
                    jmsMessageForwarder.forwardRequestToOrganisation(entry.getValue(), messageType, null, organisation);
                } catch (Exception e) {
                    logger.error("Failed to send notification for organisation: " + organisation.getId(), e);
                }
            }
        }

        logger.debug("Notification sent to organisations [{}]", StringUtils.collectionToCommaDelimitedString(notificationsByOrganisation.keySet()));
    }
}
