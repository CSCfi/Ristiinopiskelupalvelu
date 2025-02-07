package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
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
    private ObjectMapper objectMapper;

    @Autowired
    public DeleteCourseUnitProcessor(CourseUnitService courseUnitService,
                                     NetworkService networkService,
                                     OrganisationService organisationService,
                                     JmsMessageForwarder jmsMessageForwarder,
                                     ObjectMapper objectMapper,
                                     MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.courseUnitService = courseUnitService;
        this.objectMapper = objectMapper;
    }

    @AsyncListener(operation = @AsyncOperation(
            channelName = "handler",
            description = "Deletes a course unit",
            servers = {"production", "staging"},
            message = @AsyncMessage(
                    description = "Deletes a course unit"
            ),
            payloadType = DeleteCourseUnitRequest.class
    ))
    @AsyncPublisher(operation = @AsyncOperation(
            channelName = "<ORGANISATION_QUEUE>",
            description = "Notification about changed elements",
            servers = {"production", "staging"},
            payloadType = CompositeIdentifiedEntityModifiedNotification.class
    ))
    @Override
    public void process(Exchange exchange) throws Exception {
        DeleteCourseUnitRequest request = objectMapper.readValue(exchange.getIn().getBody(String.class), DeleteCourseUnitRequest.class);
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        List<CompositeIdentifiedEntityModificationResult> modificationResults = this.courseUnitService.delete(request.getStudyElementId(), organisationId, request.isDeleteRealisations());
        super.notifyNetworkMembers(organisationId, MessageType.COURSEUNIT_DELETED_NOTIFICATION, modificationResults);

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "CourseUnit deleted successfully")));
    }
}
