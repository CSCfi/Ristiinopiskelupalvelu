package fi.uta.ristiinopiskelu.handler.processor.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.handler.validator.studyelement.courseunit.UpdateCourseUnitValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.persistence.config.EsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class UpdateCourseUnitProcessorTest {

    @MockitoBean
    private CourseUnitService courseUnitService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    @MockitoBean
    private UpdateCourseUnitValidator validator;

    private JsonMapper jsonMapper;
    private UpdateCourseUnitProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = mock(UpdateCourseUnitProcessor.class, withSettings()
            .useConstructor(courseUnitService, networkService, organisationService, validator,
                jmsMessageForwarder, jsonMapper, messageSchemaService)
            .defaultAnswer(CALLS_REAL_METHODS));
    }

    @Test
    public void testUpdateCourseUnit_ShouldSuccess() throws Exception {
        String courseUnitId = "TESTIJAKSO-1";
        CourseUnitEntity cuEntity = EntityInitializer.getCourseUnitEntity("12345", "4CO19KBIOP", "TUNI", null, null);
        cuEntity.setId(courseUnitId);
        cuEntity.setAbbreviation("TJAKS-1");

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, "TUNI");
        exchange.getIn().setBody(
                """
                    {
                        "courseUnit": {
                            "studyElementId": "12345",
                            "studyElementIdentifierCode": "4CO19KBIOP",
                            "abbreviation": "Biop",
                            "teachingLanguage": [
                                "fi"
                            ]
                        }
                    }
                """
        );

        when(courseUnitService.update(any(JsonNode.class), any(String.class))).thenReturn(
                List.of(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.UPDATE, CompositeIdentifiedEntityType.COURSE_UNIT, cuEntity, cuEntity)));

        processor.process(exchange);

        DefaultResponse resp = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, resp.getStatus());
        assertEquals("Course unit with id 12345 updated successfully", resp.getMessage());
    }
}
