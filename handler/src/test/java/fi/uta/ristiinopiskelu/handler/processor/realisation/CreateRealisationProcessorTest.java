package fi.uta.ristiinopiskelu.handler.processor.realisation;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CooperationNetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.jms.JmsMessageForwarder;
import fi.uta.ristiinopiskelu.handler.service.MessageSchemaService;
import fi.uta.ristiinopiskelu.handler.service.NetworkService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.handler.service.RealisationService;
import fi.uta.ristiinopiskelu.handler.validator.realisation.CreateRealisationValidator;
import fi.uta.ristiinopiskelu.messaging.message.MessageHeader;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.realisation.CreateRealisationRequest;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
public class CreateRealisationProcessorTest {

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
    private CreateRealisationValidator validator;

    private JsonMapper jsonMapper;
    private CreateRealisationProcessor processor;

    @BeforeEach
    public void before() {
        jsonMapper = new EsConfig().jsonMapper(null);
        processor = spy(new CreateRealisationProcessor(networkService, organisationService, jmsMessageForwarder,
                realisationService, validator, jsonMapper, messageSchemaService));
    }

    @Test
    public void testCreateCourseUnitRealisation_ShouldSuccess() throws Exception {
        String organisingOrganisationId = "TUNI";
        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CooperationNetwork cooperationNetwork = DtoInitializer.getCooperationNetwork("CN-1",
                new LocalisedString("verkosto 1", null, null), true, LocalDate.now(), LocalDate.now().plusYears(1));
        CooperationNetworkEntity cooperationNetworkEntity = EntityInitializer.getCooperationNetwork("CN-1",
                LocalisedStringEntity.from(new LocalisedString("verkosto 1", null, null)),
                true, LocalDate.now(), LocalDate.now().plusYears(1));

        CourseUnitEntity cuEntity = EntityInitializer.getCourseUnitEntity("OJ-1", "OJ-CODE1", organisingOrganisationId,
                Collections.singletonList(cooperationNetworkEntity), LocalisedStringEntity.from(new LocalisedString("Opintojakso 1", null, null)));

        StudyElementReference reference = DtoInitializer.getStudyElementReferenceForCourseUnit(
                cuEntity.getStudyElementId(), cuEntity.getOrganizingOrganisationId());

        RealisationWriteDTO realisation = DtoInitializer.getRealisation("TOT-1", "TOT1CODE-1", new LocalisedString("Toteutus 1", null, null),
                Collections.singletonList(reference), Collections.singletonList(cooperationNetwork), Collections.singletonList(organisationReference));

        doNothing().when(validator).validateRequest(any(), any());
        when(realisationService.createAll(any())).thenReturn(new ArrayList<>());

        CreateRealisationRequest request = new CreateRealisationRequest();
        request.setRealisations(Collections.singletonList(realisation));

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader(MessageHeader.JMS_XUSERID, organisingOrganisationId);
        exchange.getIn().setBody(jsonMapper.writeValueAsString(request));

        processor.process(exchange);

        verify(realisationService, times(1)).createAll(any());
        verify(validator, times(1)).validateRequest(any(), any());

        DefaultResponse response = exchange.getMessage().getBody(DefaultResponse.class);
        assertEquals(Status.OK, response.getStatus());
    }
}
