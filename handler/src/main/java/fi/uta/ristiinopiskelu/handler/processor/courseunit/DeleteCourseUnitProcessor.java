package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import tools.jackson.databind.json.JsonMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.RistiinopiskeluMessage;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.DeleteCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteCourseUnitProcessor extends AbstractCompositeIdentifiedEntityProcessor<CourseUnitEntity> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteCourseUnitProcessor.class);

    private CourseUnitService courseUnitService;
    private JsonMapper jsonMapper;

    @Autowired
    public DeleteCourseUnitProcessor(CourseUnitService courseUnitService,
                                     NetworkService networkService,
                                     OrganisationService organisationService,
                                     JmsMessageForwarder jmsMessageForwarder,
                                     JsonMapper jsonMapper,
                                     MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.courseUnitService = courseUnitService;
        this.jsonMapper = jsonMapper;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        DeleteCourseUnitRequest request = jsonMapper.readValue(exchange.getIn().getBody(String.class), DeleteCourseUnitRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        List<CompositeIdentifiedEntityModificationResult> modificationResults = this.courseUnitService.delete(request.getStudyElementId(), organisationId, request.isDeleteRealisations());
        super.notifyNetworkMembers(organisationId, MessageType.COURSEUNIT_DELETED_NOTIFICATION, modificationResults);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "CourseUnit deleted successfully")));
    }
}
