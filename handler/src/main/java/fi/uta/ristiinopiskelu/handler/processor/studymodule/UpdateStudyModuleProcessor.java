package fi.uta.ristiinopiskelu.handler.processor.studymodule;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule.UpdateStudyModuleValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.UpdateStudyModuleRequest;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateStudyModuleProcessor extends AbstractCompositeIdentifiedEntityProcessor<StudyModuleEntity> {

    private StudyModuleService studyModuleService;
    private UpdateStudyModuleValidator validator;
    private JsonMapper jsonMapper;

    @Autowired
    public UpdateStudyModuleProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      UpdateStudyModuleValidator validator,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      StudyModuleService studyModuleService,
                                      JsonMapper jsonMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.studyModuleService = studyModuleService;
        this.validator = validator;
        this.jsonMapper = jsonMapper;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode requestJsonNodeTree = jsonMapper.readTree(exchange.getIn().getBody(String.class));
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        StudyModuleEntity original = validator.validateJson(requestJsonNodeTree, organisationId);
        List<CompositeIdentifiedEntityModificationResult> modificationResults = studyModuleService.update(requestJsonNodeTree.get("studyModule"), organisationId);

        // send notifications based on the original entity information
        super.notifyNetworkMembers(organisationId, MessageType.STUDYMODULE_UPDATED_NOTIFICATION, modificationResults);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Study module with id " + original.getStudyElementId() + " updated successfully")));
    }
}
