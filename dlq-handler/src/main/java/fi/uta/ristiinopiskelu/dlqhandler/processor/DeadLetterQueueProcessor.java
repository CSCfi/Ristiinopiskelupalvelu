package fi.uta.ristiinopiskelu.dlqhandler.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.datamodel.entity.DeadLetterQueueEntity;
import fi.uta.ristiinopiskelu.persistence.repository.DeadLetterQueueRepository;
import fi.uta.ristiinopiskelu.persistence.repository.OrganisationRepository;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

public class DeadLetterQueueProcessor implements Processor {

    public final String UNKNOWN_MESSAGE = "UNKNOWN_MESSAGE";

    private String organisationId;
    private OrganisationRepository organisationRepository;
    private DeadLetterQueueRepository deadLetterQueueRepository;
    private ObjectMapper objectMapper;

    public DeadLetterQueueProcessor(String organisationId,
                                    OrganisationRepository organisationRepository,
                                    DeadLetterQueueRepository deadLetterQueueRepository,
                                    ObjectMapper objectMapper) {
        this.organisationId = organisationId;
        this.organisationRepository = organisationRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Read as object first to prevent objectMapper write escaped line breaks to when writing to string
        Object json = objectMapper.readValue(exchange.getIn().getBody(String.class), Object.class);
        String message = objectMapper.writeValueAsString(json);
        String messageType = exchange.getIn().getHeader(MessageHeader.MESSAGE_TYPE, String.class);

        if(StringUtils.isEmpty(messageType)) {
            messageType = UNKNOWN_MESSAGE;
        }

        DeadLetterQueueEntity deadLetter = new DeadLetterQueueEntity();
        deadLetter.setOrganisationId(organisationId);
        deadLetter.setMessageType(messageType);
        deadLetter.setMessage(message);
        deadLetter.setEmailSent(false);
        deadLetter.setConsumedTimestamp(OffsetDateTime.now());

        deadLetterQueueRepository.create(deadLetter);
    }
}
