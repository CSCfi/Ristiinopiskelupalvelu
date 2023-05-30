package fi.uta.ristiinopiskelu.handler.processor.studymodule;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.studymodule.CreateStudyModuleValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.CreateStudyModuleRequest;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreateStudyModuleProcessor extends AbstractCompositeIdentifiedEntityProcessor<StudyModuleEntity> {

    private StudyModuleService studyModuleService;
    private ObjectMapper objectMapper;
    private CreateStudyModuleValidator validator;
    private MessageSchemaService messageSchemaService;

    @Autowired
    public CreateStudyModuleProcessor(NetworkService networkService,
                                      OrganisationService organisationService,
                                      JmsMessageForwarder jmsMessageForwarder,
                                      StudyModuleService studyModuleService,
                                      CreateStudyModuleValidator validator,
                                      ObjectMapper objectMapper,
                                      MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.studyModuleService = studyModuleService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.messageSchemaService = messageSchemaService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        CreateStudyModuleRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), CreateStudyModuleRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        List<StudyModuleWriteDTO> studyModules = request.getStudyModules();
        validator.validateObject(studyModules, organisationId);

        CompositeIdentifiedEntityModificationResult result = studyModuleService.createAll(studyModules, organisationId);
        super.notifyNetworkMembers(organisationId, MessageType.STUDYMODULE_CREATED_NOTIFICATION, result);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Study module creation successful [created=" + result.getCreated().size() + ", updated=" + result.getUpdated().size())));
    }
}
