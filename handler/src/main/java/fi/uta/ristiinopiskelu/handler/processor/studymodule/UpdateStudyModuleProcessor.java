package fi.uta.ristiinopiskelu.handler.processor.studymodule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.result.DefaultCompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule.UpdateStudyModuleValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UpdateStudyModuleProcessor extends AbstractCompositeIdentifiedEntityProcessor<StudyModuleEntity> {

    private StudyModuleService studyModuleService;
    private UpdateStudyModuleValidator validator;
    private ObjectMapper objectMapper;

    @Autowired
    public UpdateStudyModuleProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      UpdateStudyModuleValidator validator,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      StudyModuleService studyModuleService,
                                      ObjectMapper objectMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.studyModuleService = studyModuleService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode requestJsonNodeTree = objectMapper.readTree(exchange.getIn().getBody(String.class));
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        StudyModuleEntity original = validator.validateJson(requestJsonNodeTree, organisationId);
        StudyModuleEntity updated = studyModuleService.update(requestJsonNodeTree.get("studyModule"), organisationId);

        // send notifications based on the original entity information
        super.notifyNetworkMembers(organisationId, MessageType.STUDYMODULE_UPDATED_NOTIFICATION,
            new DefaultCompositeIdentifiedEntityModificationResult(null, Collections.singletonList(original), null));

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Study module with id " + updated.getStudyElementId() + " updated successfully")));
    }
}
