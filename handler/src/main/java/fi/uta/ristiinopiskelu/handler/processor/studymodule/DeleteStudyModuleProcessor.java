package fi.uta.ristiinopiskelu.handler.processor.studymodule;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.result.DefaultCompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.DeleteStudyModuleRequest;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DeleteStudyModuleProcessor extends AbstractCompositeIdentifiedEntityProcessor<StudyModuleEntity> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStudyModuleProcessor.class);

    private StudyModuleService studyModuleService;
    private ObjectMapper objectMapper;

    @Autowired
    public DeleteStudyModuleProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      StudyModuleService studyModuleService,
                                      ObjectMapper objectMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.studyModuleService = studyModuleService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        DeleteStudyModuleRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), DeleteStudyModuleRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        StudyModuleEntity deleted = studyModuleService.delete(request.getStudyElementId(), organisationId, request.isDeleteCourseUnits());
        super.notifyNetworkMembers(organisationId, MessageType.STUDYMODULE_DELETED_NOTIFICATION,
            new DefaultCompositeIdentifiedEntityModificationResult(null, null, Collections.singletonList(deleted)));

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Study module deleted successfully")));
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
