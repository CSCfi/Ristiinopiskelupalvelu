package fi.uta.ristiinopiskelu.handler.processor.student;

import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RegistrationEntity;
import fi.uta.ristiinopiskelu.handler.exception.validation.MessageForwardingFailedException;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.student.AbstractForwardedStudentMessage;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractStudentProcessor  implements Processor {

    private JmsMessageForwarder jmsMessageForwarder;

    public AbstractStudentProcessor(JmsMessageForwarder jmsMessageForwarder) {
        this.jmsMessageForwarder = jmsMessageForwarder;
    }

    protected void forwardMessage(Exchange exchange, MessageType messageType,
                                  Map<OrganisationEntity, List<RegistrationEntity>> organisationsAndRegistrations,
                                  AbstractForwardedStudentMessage forwardedRequest, Map<String, String> customHeaders) throws Exception {
        String correlationId = exchange.getIn().getHeader("JMSMessageID", String.class);
        List<String> messageForwardingFailedOrganisations = new ArrayList<>();
        List<String> messageForwardingSucceededOrganisations = new ArrayList<>();

        for(Map.Entry<OrganisationEntity, List<RegistrationEntity>> entry : organisationsAndRegistrations.entrySet()) {
            OrganisationEntity organisation = entry.getKey();
            List<RegistrationEntity> registrations = entry.getValue();

            forwardedRequest.setHostStudyRightIdentifiers(registrations.stream()
                .filter(r -> r.getStudent().getHostStudyRight() != null
                    && r.getStudent().getHostStudyRight().getIdentifiers() != null
                    && r.getStudent().getHostStudyRight().getIdentifiers().getStudyRightId() != null)
                .map(r -> r.getStudent().getHostStudyRight().getIdentifiers())
                .distinct()
                .collect(Collectors.toList()));

            try {
                jmsMessageForwarder.forwardRequestToOrganisation(forwardedRequest.getStudentRequestId(), forwardedRequest,
                    messageType, correlationId, organisation, customHeaders);
                messageForwardingSucceededOrganisations.add(organisation.getId());
            } catch (MessageForwardingFailedException e) {
                messageForwardingFailedOrganisations.add(organisation.getId());
            }
        }

        if(!CollectionUtils.isEmpty(messageForwardingFailedOrganisations)) {
            throw new MessageForwardingFailedException(
                "Failed to forward " + messageType + " for all organisations that were supposed to receive it. "
                    + " Forwarding failed to organisations: " + String.join(", ", messageForwardingFailedOrganisations)
                    + " Forwarding was successful to organisations: " + String.join(", ", messageForwardingSucceededOrganisations)
                    + " Message was saved to index with id: " + forwardedRequest.getStudentRequestId());
        }
    }
}
