package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.processor.AbstractCompositeIdentifiedEntityProcessor;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.result.DefaultCompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.UpdateCourseUnitValidator;
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
public class UpdateCourseUnitProcessor extends AbstractCompositeIdentifiedEntityProcessor<CourseUnitEntity> {

    private CourseUnitService courseUnitService;
    private UpdateCourseUnitValidator validator;
    private ObjectMapper objectMapper;

    @Autowired
    public UpdateCourseUnitProcessor(CourseUnitService courseUnitService,
                                     NetworkService networkService,
                                     OrganisationService organisationService,
                                     UpdateCourseUnitValidator validator,
                                     JmsMessageForwarder jmsMessageForwarder,
                                     ObjectMapper objectMapper,
                                     MessageSchemaService messageSchemaService) {
        super(networkService, organisationService, jmsMessageForwarder, messageSchemaService);
        this.courseUnitService = courseUnitService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode requestJsonNodeTree = objectMapper.readTree(exchange.getIn().getBody(String.class));
        String organisationId = exchange.getIn().getHeader(MessageHeader.JMS_XUSERID, String.class);

        CourseUnitEntity original = validator.validateJson(requestJsonNodeTree, organisationId);
        CourseUnitEntity updated = courseUnitService.update(requestJsonNodeTree.get("courseUnit"), organisationId);

        // send notifications based on the original entity information
        super.notifyNetworkMembers(organisationId, MessageType.COURSEUNIT_UPDATED_NOTIFICATION,
                    new DefaultCompositeIdentifiedEntityModificationResult(null,
                        Collections.singletonList(original), null));

        exchange.setMessage(new RistiinopiskeluMessage(exchange, MessageType.DEFAULT_RESPONSE,
                new DefaultResponse(Status.OK, "Course unit with id " + updated.getStudyElementId() + " updated successfully")));
    }
}
