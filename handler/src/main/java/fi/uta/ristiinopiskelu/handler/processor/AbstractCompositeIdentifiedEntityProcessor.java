package fi.uta.ristiinopiskelu.handler.processor;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.entity.CompositeIdentifiedEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.EntityNotFoundException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

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

    public void notifyNetworkMembers(String organizingOrganisationId, MessageType messageType, List<CompositeIdentifiedEntityModificationResult> modificationResults) throws Exception {
        Assert.hasText(organizingOrganisationId, "organizingOrganisationId cannot be null");
        Assert.notNull(messageType, "messageType cannot be null");
        Assert.notNull(modificationResults, "modificationResults cannot be null");

        Map<String, CompositeIdentifiedEntityModifiedNotification> notifications = getNotificationsByOrganisation(organizingOrganisationId, modificationResults);

        if(CollectionUtils.isEmpty(notifications)) {
            logger.debug("No notifications to be sent, skipping");
            return;
        }

        this.sendNotifications(organizingOrganisationId, messageType, notifications);
    }

    private Map<String, CompositeIdentifiedEntityModifiedNotification> getNotificationsByOrganisation(String organizingOrganisationId, List<CompositeIdentifiedEntityModificationResult> modificationResults) {
        Map<String, CompositeIdentifiedEntityModifiedNotification> notificationsByOrganisation = new HashMap<>();

        for(CompositeIdentifiedEntityModificationResult result : modificationResults) {
            CompositeIdentifiedEntity previous = result.getPrevious();
            CompositeIdentifiedEntity current = result.getCurrent();

            if(result.getOperationType() == ModificationOperationType.UPDATE) {
                final List<CooperationNetwork> originalNetworks = !CollectionUtils.isEmpty(previous.getCooperationNetworks()) ? previous.getCooperationNetworks() : new ArrayList<>();
                final List<CooperationNetwork> updatedNetworks = !CollectionUtils.isEmpty(current.getCooperationNetworks()) ? current.getCooperationNetworks() : new ArrayList<>();

                List<String> originalNetworkOrganisations = networkService.findOrganisationIdsFromNetworks(originalNetworks.stream().map(CooperationNetwork::getId).toList());
                List<String> updatedNetworkOrganisations = networkService.findOrganisationIdsFromNetworks(updatedNetworks.stream().map(CooperationNetwork::getId).toList());

                populateReferences(organizingOrganisationId, notificationsByOrganisation, result, originalNetworkOrganisations, updatedNetworkOrganisations);
            } else {
                List<CooperationNetwork> cooperationNetworks = null;

                if(result.getOperationType() == ModificationOperationType.CREATE) {
                    cooperationNetworks = !CollectionUtils.isEmpty(current.getCooperationNetworks()) ? current.getCooperationNetworks() : new ArrayList<>();
                } else if(result.getOperationType() == ModificationOperationType.DELETE) {
                    cooperationNetworks = !CollectionUtils.isEmpty(previous.getCooperationNetworks()) ? previous.getCooperationNetworks() : new ArrayList<>();
                }

                List<String> orgs = networkService.findOrganisationIdsFromNetworks(cooperationNetworks.stream().map(CooperationNetwork::getId).toList());

                for(String organisationId : orgs) {
                    CompositeIdentifiedEntityModifiedNotification notification = notificationsByOrganisation.get(organisationId);

                    if(notification == null) {
                        notification = new CompositeIdentifiedEntityModifiedNotification();
                        notification.setTimestamp(OffsetDateTime.now());
                        notification.setSendingOrganisationTkCode(organizingOrganisationId);
                    }

                    if(result.getOperationType() == ModificationOperationType.CREATE) {
                        notification.getCreated().add(new CompositeIdentifiedEntityReference(current.getElementId(), current.getOrganizingOrganisationId(), result.getType()));
                    } else if(result.getOperationType() == ModificationOperationType.DELETE) {
                        notification.getDeleted().add(new CompositeIdentifiedEntityReference(previous.getElementId(), previous.getOrganizingOrganisationId(), result.getType()));
                    }

                    notificationsByOrganisation.put(organisationId, notification);
                }
            }
        }

        return notificationsByOrganisation;
    }

    private void populateReferences(String organizingOrganisationId, Map<String, CompositeIdentifiedEntityModifiedNotification> notificationsByOrganisation,
                                    CompositeIdentifiedEntityModificationResult result, List<String> originalNetworkOrganisations, List<String> updatedNetworkOrganisations) {
        List<String> organisationIds = Stream.concat(
            originalNetworkOrganisations.stream(),
            updatedNetworkOrganisations.stream()
        ).distinct().toList();

        for(String organisationId : organisationIds) {
            CompositeIdentifiedEntityModifiedNotification notification = notificationsByOrganisation.get(organisationId);

            if(notification == null) {
                notification = new CompositeIdentifiedEntityModifiedNotification();
                notification.setTimestamp(OffsetDateTime.now());
                notification.setSendingOrganisationTkCode(organizingOrganisationId);
            }

            List<CompositeIdentifiedEntityReference> references = null;

            switch (getNotificationListTypeByOrganisation(originalNetworkOrganisations, updatedNetworkOrganisations, organisationId)) {
                case CREATED -> references = notification.getCreated();
                case UPDATED -> references = notification.getUpdated();
                case DELETED -> references = notification.getDeleted();
            }

            if(references == null) {
                // this shouldn't be possible. ignore.
                continue;
            }

            references.add(new CompositeIdentifiedEntityReference(result.getCurrent().getElementId(), result.getCurrent().getOrganizingOrganisationId(), result.getType()));
            notificationsByOrganisation.put(organisationId, notification);
        }
    }

    public NotificationListType getNotificationListTypeByOrganisation(List<String> originalNetworkOrganisations,
                                                                      List<String> updatedNetworkOrganisations,
                                                                      String organisationId) {
        if(!originalNetworkOrganisations.contains(organisationId) && updatedNetworkOrganisations.contains(organisationId)) {
            return NotificationListType.CREATED;
        } else if(originalNetworkOrganisations.contains(organisationId) && !updatedNetworkOrganisations.contains(organisationId)) {
            return NotificationListType.DELETED;
        } else if(originalNetworkOrganisations.contains(organisationId) && updatedNetworkOrganisations.contains(organisationId)) {
            return NotificationListType.UPDATED;
        }

        return null;
    }

    private enum NotificationListType {
        CREATED,
        UPDATED,
        DELETED
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

    protected long getResultAmount(ModificationOperationType operationType, CompositeIdentifiedEntityType type, List<CompositeIdentifiedEntityModificationResult> modificationResults) {
        Assert.notNull(operationType, "operationType cannot be null");

        if(CollectionUtils.isEmpty(modificationResults)) {
            return 0;
        }

        return modificationResults.stream()
                .filter(result -> {
                    if(type == null) {
                        return result.getOperationType() == operationType;
                    }

                    return result.getOperationType() == operationType && result.getType() == type;
                })
                .count();
    }
}
