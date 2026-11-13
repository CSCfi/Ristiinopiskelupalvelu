package fi.uta.ristiinopiskelu.handler.processor.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.RealisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class UpdateRealisationProcessorTest {

    @MockitoBean
    private RealisationService realisationService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private OrganisationService organisationService;

    @MockitoBean
    private JmsMessageForwarder jmsMessageForwarder;

    @MockitoBean
    private MessageSchemaService messageSchemaService;

    @MockitoBean
    private UpdateRealisationValidator validator;

    private JsonMapper jsonMapper;
    private UpdateRealisationProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = spy(new UpdateRealisationProcessor(networkService, organisationService,
                jmsMessageForwarder, realisationService, validator, jsonMapper, messageSchemaService));
    }

    @Test
    public void testUpdateCourseUnitRealisation_ShouldSuccess() throws Exception {
        String organisationIdHeader = "TUNI";

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork("CN-1", new LocalisedString("verkosto", null, null),
                true, LocalDate.now(), LocalDate.now().plusYears(1));
        CooperationNetworkEntity cooperationNetworkEntity = EntityInitializer.getCooperationNetwork("CN-1",
                LocalisedStringEntity.from(new LocalisedString("verkosto", null, null)),
                true, LocalDate.now(), LocalDate.now().plusYears(1));

        CourseUnitEntity cuEntity = new CourseUnitEntity();
        cuEntity.setStudyElementId("OJ-1");
        cuEntity.setStudyElementIdentifierCode("OJ-CODE1");
        cuEntity.setOrganizingOrganisationId("TUNI");
        cuEntity.setCooperationNetworks(Collections.singletonList(cooperationNetworkEntity));

        StudyElementReference reference = new StudyElementReference();
        reference.setReferenceIdentifier(cuEntity.getStudyElementId());
        reference.setReferenceOrganizer(cuEntity.getOrganizingOrganisationId());
        
        RealisationWriteDTO realisation = DtoInitializer.getRealisation("TOT-1", "TOTCODE-1", new LocalisedString("Toteutus 1", null, null),
                null, null, null);

        RealisationEntity realisationEntity = RealisationEntity.from(realisation);
        when(validator.validateJson(any(), any())).thenReturn(realisationEntity);
        when(realisationService.update(any(JsonNode.class), eq(organisationIdHeader))).thenReturn(
                List.of(new CompositeIdentifiedEntityModificationResult(ModificationOperationType.UPDATE, CompositeIdentifiedEntityType.REALISATION, realisationEntity, realisationEntity)));
 
        UpdateRealisationRequest request = new UpdateRealisationRequest();
        request.setRealisation(realisation);

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(jsonMapper.writeValueAsString(request));
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, organisationIdHeader);

        processor.process(exchange);

        verify(realisationService, times(1)).update(any(JsonNode.class), eq(organisationIdHeader));
        verify(validator, times(1)).validateJson(any(), any());
       
        DefaultResponse response = exchange.getMessage().getBody(DefaultResponse.class);
        assertTrue(response.getStatus() == Status.OK);
        assertTrue(response.getMessage().contains(realisation.getRealisationId()));
    }
}
