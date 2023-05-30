package fi.uta.ristiinopiskelu.handler.processor.realisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.result.DefaultCompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.DeleteRealisationRequest;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DeleteRealisationProcessor extends AbstractCompositeIdentifiedEntityProcessor<RealisationEntity> {

    private ObjectMapper objectMapper;
    private RealisationService realisationService;

    @Autowired
    public DeleteRealisationProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      RealisationService realisationService,
                                      ObjectMapper objectMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.objectMapper = objectMapper;
        this.realisationService = realisationService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        DeleteRealisationRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), DeleteRealisationRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        RealisationEntity deleted = realisationService.delete(request.getRealisationId(), organisationId);
        super.notifyNetworkMembers(organisationId, MessageType.REALISATION_DELETED_NOTIFICATION,
            new DefaultCompositeIdentifiedEntityModificationResult(null, null, Collections.singletonList(deleted)));

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Realisation deleted successfully")));
    }
}
