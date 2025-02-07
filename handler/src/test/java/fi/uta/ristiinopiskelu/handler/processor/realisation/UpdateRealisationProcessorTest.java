package fi.uta.ristiinopiskelu.handler.processor.realisation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.service.result.CompositeIdentifiedEntityModificationResult;
import fi.uta.ristiinopiskelu.handler.service.result.ModificationOperationType;
import fi.uta.ristiinopiskelu.handler.validator.realisation.UpdateRealisationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.UpdateRealisationRequest;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UpdateRealisationProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateRealisationProcessorTest.class);

    @MockBean
    private RealisationService realisationService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private OrganisationService organisationService;

    @MockBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockBean
    private MessageSchemaService messageSchemaService;

    @MockBean
    private UpdateRealisationValidator validator;

    private ObjectMapper objectMapper;
    private ModelMapper modelMapper;
    private UpdateRealisationProcessor processor;

    @BeforeEach
    public void before() {
        modelMapper = new ModelMapper();

        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        processor = spy(new UpdateRealisationProcessor(networkService, organisationService,
                jmsMessageForwarder, realisationService, validator, objectMapper, messageSchemaService));
    }

    @Test
    public void testUpdateCourseUnitRealisation_ShouldSuccess() throws Exception {
        String organisationIdHeader = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork("CN-1", new LocalisedString("verkosto", null, null),
                true, LocalDate.now(), LocalDate.now().plusYears(1));

        CourseUnitEntity cuEntity = new CourseUnitEntity();
        cuEntity.setStudyElementId("OJ-1");
        cuEntity.setStudyElementIdentifierCode("OJ-CODE1");
        cuEntity.setOrganizingOrganisationId("TUNI");
        cuEntity.setCooperationNetworks(Collections.singletonList(cooperationNetwork));

        StudyElementReference reference = new StudyElementReference();
        reference.setReferenceIdentifier(cuEntity.getStudyElementId());
        reference.setReferenceOrganizer(cuEntity.getOrganizingOrganisationId());
        
        RealisationWriteDTO realisation = DtoInitializer.getRealisation("TOT-1", "TOTCODE-1", new LocalisedString("Toteutus 1", null, null),
                null, null, null);

        RealisationEntity realisationEntity = modelMapper.map(realisation, RealisationEntity.class);
        when(validator.validateJson(any(), any())).thenReturn(realisationEntity);
        when(realisationService.update(any(JsonNode.class), eq(organisationIdHeader))).thenReturn(
                List.of(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.UPDATE, CompositeIdentifiedEntityType.REALISATION, realisationEntity, realisationEntity)));
 
        UpdateRealisationRequest request = new UpdateRealisationRequest();
        request.setRealisation(realisation);

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(objectMapper.writeValueAsString(request));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, organisationIdHeader);

        processor.process(exchange);

        verify(realisationService, times(1)).update(any(JsonNode.class), eq(organisationIdHeader));
        verify(validator, times(1)).validateJson(any(), any());
       
        DefaultResponse response = exchange.getMessage().getBody(DefaultResponse.class);
        assertTrue(response.getStatus() == Status.OK);
        assertTrue(response.getMessage().contains(realisation.getRealisationId()));
    }
}
