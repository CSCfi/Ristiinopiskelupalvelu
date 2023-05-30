package fi.uta.ristiinopiskelu.handler.processor.network;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.network.NetworkWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Notification;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkCreatedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.NetworkUpdatedNotification;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractNetworkProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNetworkProcessor.class);

    private final OrganisationService organisationService;
    private final JmsMessageForwarder jmsMessageForwarder;

    public AbstractNetworkProcessor(OrganisationService organisationService, JmsMessageForwarder jmsMessageForwarder) {
        this.organisationService = organisationService;
        this.jmsMessageForwarder = jmsMessageForwarder;
    }

    protected void sendNetworkCreatedNotification(NetworkEntity network) {
        List<OrganisationEntity> organisations = getNetworkOrganisations(network);

        NetworkCreatedNotification networkCreatedNotification = new NetworkCreatedNotification(OffsetDateTime.now(),
            NetworkWriteDTO.fromEntity(network));

        logger.info("Sending network create notification to organisations " + organisations.stream().map(o -> o.getId() + "/" + o.getOrganisationIdentifier()).collect(Collectors.joining(", ")) +
            " (network: " + network.getId() + "/" + networkCreatedNotification.getNetwork().getId() + ", organisations: " +
            networkCreatedNotification.getNetwork().getOrganisations().stream().map(o -> o.getOrganisationTkCode()).collect(Collectors.joining(", ")) + ")");

        // forward
        forwardNotification(null, network, organisations, networkCreatedNotification, MessageType.NETWORK_CREATED_NOTIFICATION);
    }

    protected void sendNetworkUpdatedNotification(String messageId, NetworkEntity network) {
        List<OrganisationEntity> organisations = getNetworkOrganisations(network);

        NetworkUpdatedNotification networkUpdatedNotification = new NetworkUpdatedNotification(OffsetDateTime.now(),
            NetworkWriteDTO.fromEntity(network));

        logger.info("Sending network updated notification to organisations " + organisations.stream().map(o -> o.getId() + "/" + o.getOrganisationIdentifier()).collect(Collectors.joining(", ")) +
            " (network: " + network.getId() + "/" + networkUpdatedNotification.getNetwork().getId() + ", organisations: " +
            networkUpdatedNotification.getNetwork().getOrganisations().stream().map(o -> o.getOrganisationTkCode()).collect(Collectors.joining(", ")) + ")");

        // forward
        forwardNotification(messageId, network, organisations, networkUpdatedNotification, MessageType.NETWORK_UPDATED_NOTIFICATION);
    }

    private List<OrganisationEntity> getNetworkOrganisations(NetworkEntity network) {
        return organisationService.findByIds(network.getOrganisations().stream().map(NetworkOrganisation::getOrganisationTkCode)
            .collect(Collectors.toList()));
    }

    private void forwardNotification(String messageId, NetworkEntity network, List<OrganisationEntity> organisations,
                                     Notification notification, MessageType messageType) {
        for(OrganisationEntity organisation : organisations) {
            this.jmsMessageForwarder.forwardRequestToOrganisation(network.getId(), notification,
                messageType, messageId, organisation);
        }
    }
}
