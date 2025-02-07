package fi.uta.ristiinopiskelu.handler.integration.route.current.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.Validity;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.handler.helper.HistoryHelper;
import fi.uta.ristiinopiskelu.handler.helper.JmsHelper;
import fi.uta.ristiinopiskelu.handler.integration.route.current.AbstractRouteIntegrationTest;
import fi.uta.ristiinopiskelu.handler.service.CourseUnitService;
import fi.uta.ristiinopiskelu.handler.service.OrganisationService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.JsonValidationFailedResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class UpdateCourseUnitRouteIntegrationTest extends AbstractRouteIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateCourseUnitRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(500000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private CourseUnitService courseUnitService;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private RealisationRepository realisationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Value("${general.message-schema.version.current}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                    new LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisation.setSchemaVersion(this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_containsSpecialCharactersAndXss_shoudlSucceedAndTagsRemoved() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"en\": \"<script type='text/javascript'>xss here</script>\",\n" +
                "\t\t\t\t\"sv\": \"<strong>blaah</strong> jaksonnimi2 sv\",\n" +
                "\t\t\t\t\"fi\": \"<xc jaksonnimi2 fi\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("", result.getName().getValue("fi"));
        assertEquals("blaah jaksonnimi2 sv", result.getName().getValue("sv"));
        assertEquals("", result.getName().getValue("en"));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceed() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> courseUnit.getCooperationNetworks().stream()
                .anyMatch(cn -> cn.getId().equals(resultCn.getId())
                    && cn.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                    && cn.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                    && cn.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                )));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_withExpiredCooperationNetwork_shouldSucceed() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().minusDays(2));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        Validity validity = new Validity(Validity.ContinuityEnum.FIXED, OffsetDateTime.now().minusYears(1), OffsetDateTime.now().minusDays(2));
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(network.getId(), network.getName(), Collections.singletonList(new NetworkOrganisation(organizingOrganisationId, true, validity)),
            validity, true);
        networkRepository.create(networkEntity);

        String updateCourseUnitJson = """
            {
                "courseUnit": {
                    "studyElementId": "ID1",
                    "name": {
                        "values": {
                            "fi": "jaksonnimi2 fi",
                            "en": "jaksonnimi2 en",
                            "sv": "jaksonnimi2 sv"
                        }
                    },
                    "cooperationNetworks": [
                        {
                            "id": "%s",
                            "name": {
                                "values": {
                                    "fi": "Verkosto",
                                    "en": null,
                                    "sv": null
                                }
                            },
                            "enrollable": true
                        }
                    ]
                }
            }""".formatted(networkEntity.getId());

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> courseUnit.getCooperationNetworks().stream()
                .anyMatch(cn -> cn.getId().equals(resultCn.getId())
                    && cn.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                    && cn.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                    && cn.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                )));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_noCooperationNetworkNameOrStatusGiven_shouldFillCooperationNetworkFields() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organizingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity.getId(), new LocalisedString("JUUJUU", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "           \"cooperationNetworks\": [\n" +
                "               {\n" +
                "                   \"id\": \"" + networkEntity.getId() +"\",\n" +
                "                   \"enrollable\": true\n" +
                "               }\n" +
                "           ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> networkEntity.getId().equals(resultCn.getId())
                && networkEntity.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                && networkEntity.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                && networkEntity.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
            ));
    }

    @Test
    public void testSendingUpdateCourseUnitMessageWithEmptyCooperationNetworks_shouldSucceed() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\"cooperationNetworks\": [\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(StudyStatus.ACTIVE, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedContainsForwardSlash() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "I/D1", "RAI/RAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"I/D1\",\n" +
                "\t\t\"status\": \"ARCHIVED\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(StudyStatus.ARCHIVED, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_missingStudyElementId_shouldFail() throws JMSException {
        String updateCourseUnitJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertEquals(1, resp.getErrors().size());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailBrokenJson() throws JMSException {
        String updateCourseUnitJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"IDASDASD\",,,,,,,\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"jaksonnimi2 fi\",\n" +
                "\t\t\t\t\"en\": \"jaksonnimi2 en\",\n" +
                "\t\t\t\t\"sv\": \"jaksonnimi2 sv\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedNoMatchingNetworksForRealisation() throws JMSException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity1 = persistNetworkEntity("CN-1", null, Collections.singletonList(organizingOrganisationId));
        NetworkEntity networkEntity2 = persistNetworkEntity("CN-3", null, Collections.singletonList(organizingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity1.getId(), networkEntity1.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        StudyElementReference reference = DtoInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organizingOrganisationId);
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("R1", "RCODE1", organizingOrganisationId,
            Collections.singletonList(reference), Collections.singletonList(network));
        realisationRepository.create(realisationEntity);

        String updateCourseUnitJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi2 fi\",\n" +
                "                \"en\": \"jaksonnimi2 en\",\n" +
                "                \"sv\": \"jaksonnimi2 sv\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"cooperationNetworks\": [\n" +
                "            {\n" +
                "                \"id\": \"" + networkEntity2.getId() + "\",\n" +
                "                \"name\": {\n" +
                "                    \"values\": {\n" +
                "                        \"fi\": \"Verkosto 3\",\n" +
                "                        \"en\": \"Network 3\",\n" +
                "                        \"sv\": null\n" +
                "                    }\n" +
                "                },\n" +
                "                \"enrollable\": true\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSuccessAndIgnoreType() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"type\": \"COURSE_UNIT\"," +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi2 fi\",\n" +
                "                \"en\": \"jaksonnimi2 en\",\n" +
                "                \"sv\": \"jaksonnimi2 sv\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSuccessAndSuccessfullySaveHistory() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi2 fi\",\n" +
                "                \"en\": \"jaksonnimi2 en\",\n" +
                "                \"sv\": \"jaksonnimi2 sv\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity updatedCourseUnit = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), updatedCourseUnit.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), updatedCourseUnit.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi2 fi", updatedCourseUnit.getName().getValue("fi"));
        assertEquals("jaksonnimi2 en", updatedCourseUnit.getName().getValue("en"));
        assertEquals("jaksonnimi2 sv", updatedCourseUnit.getName().getValue("sv"));
        assertEquals(2, updatedCourseUnit.getVersion());
        assertNotNull(updatedCourseUnit.getCreatedTime());
        assertNotNull(updatedCourseUnit.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, updatedCourseUnit.getType());

        List<CourseUnitEntity> historyEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate,"opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        CourseUnitEntity historyEntity = historyEntities.get(0);
        assertEquals(courseUnit.getName().getValue("fi"), historyEntity.getName().getValue("fi"));
        assertEquals(courseUnit.getName().getValue("en"), historyEntity.getName().getValue("en"));
        assertEquals(courseUnit.getName().getValue("sv"), historyEntity.getName().getValue("sv"));

        String courseUnitUpdateJson2 =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi3 fi\",\n" +
                "                \"en\": \"jaksonnimi3 en\",\n" +
                "                \"sv\": \"jaksonnimi3 sv\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson2, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity updatedCourseUnit2 = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), updatedCourseUnit2.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), updatedCourseUnit2.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi3 fi", updatedCourseUnit2.getName().getValue("fi"));
        assertEquals("jaksonnimi3 en", updatedCourseUnit2.getName().getValue("en"));
        assertEquals("jaksonnimi3 sv", updatedCourseUnit2.getName().getValue("sv"));
        assertEquals(3, updatedCourseUnit2.getVersion());
        assertNotNull(updatedCourseUnit2.getCreatedTime());
        assertNotNull(updatedCourseUnit2.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, updatedCourseUnit2.getType());

        historyEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate,"opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(historyEntities);
        assertEquals(2, historyEntities.size());

        CourseUnitEntity newHistoryEntity = historyEntities.stream()
            .sorted(Comparator.comparing(CourseUnitEntity::getUpdateTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
            .findFirst()
            .orElse(null);

        assertEquals(updatedCourseUnit.getName().getValue("fi"), newHistoryEntity.getName().getValue("fi"));
        assertEquals(updatedCourseUnit.getName().getValue("en"), newHistoryEntity.getName().getValue("en"));
        assertEquals(updatedCourseUnit.getName().getValue("sv"), newHistoryEntity.getName().getValue("sv"));
    }
    
    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailHasExtraField() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"type\": \"COURSE_UNIT\"," +
                "        \"thisdoesnotexist\": \"somerandomcontent\"" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailStatusNull() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"status\": null\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertNull(result.getUpdateTime());
        assertEquals(StudyStatus.ACTIVE, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedCooperationNetworksEmpty() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"cooperationNetworks\": []\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(0, result.getCooperationNetworks().size());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedCooperationNetworksNull() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "\t\"courseUnit\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"cooperationNetworks\": null\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(null, result.getCooperationNetworks());
    }


    @Test
    public void testSendingUpdateCourseUnitMessage_containsRealisations_shouldNotUpdateRealisationInfoAndSucceed() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatus.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"jaksonnimi 2 fi\",\n" +
                "                \"en\": \"jaksonnimi 2 en\",\n" +
                "                \"sv\": \"jaksonnimi 2 sv\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"completionOptions\": [\n" +
                "            {\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"arvioinninkohde 1\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        \"realisations\": [\n" +
                "                            {\n" +
                "                                \"realisationId\": \"ID1\",\n" +
                "                                \"realisationIdentifierCode\": \"IDENTIFIERCODE\",\n" +
                "                                \"selections\": null,\n" +
                "                                \"questionSets\": null,\n" +
                "                                \"enrollmentStartDateTime\": \"2019-12-24T15:55:52.134+02:00\",\n" +
                "                                \"enrollmentEndDateTime\": \"2020-01-23T15:55:52.134+02:00\",\n" +
                "                                \"startDate\": \"2019-12-08\",\n" +
                "                                \"endDate\": \"2020-02-08\",\n" +
                "                                \"personReferences\": null,\n" +
                "                                \"minSeats\": null,\n" +
                "                                \"maxSeats\": null,\n" +
                "                                \"location\": null,\n" +
                "                                \"organisationSpecificDescriptions\": null,\n" +
                "                                \"studyElementReferences\": [\n" +
                "                                    {\n" +
                "                                        \"referenceIdentifier\": \"ID1\",\n" +
                "                                        \"referenceOrganizer\": \"TUNI\",\n" +
                "                                        \"referenceType\": \"ASSESSMENT_ITEM\",\n" +
                "                                        \"referenceAssessmentItemId\": \"AI-1\"\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"name\": {\n" +
                "                                    \"values\": {\n" +
                "                                        \"fi\": \"nimi\"\n" +
                "                                    }\n" +
                "                                },\n" +
                "                                \"organisationReferences\": [\n" +
                "                                    {\n" +
                "                                        \"percent\": 100,\n" +
                "                                        \"organisationRole\": 1,\n" +
                "                                        \"target\": {\n" +
                "                                            \"organisationIdentifier\": \"TUNI\",\n" +
                "                                            \"organisationTkCode\": \"TUNI\",\n" +
                "                                            \"organisationName\": null,\n" +
                "                                            \"unitName\": null,\n" +
                "                                            \"nameDescription\": null,\n" +
                "                                            \"postalAddress\": null,\n" +
                "                                            \"streetAddress\": null,\n" +
                "                                            \"municipalityCode\": null,\n" +
                "                                            \"phone\": null,\n" +
                "                                            \"url\": null\n" +
                "                                        },\n" +
                "                                        \"description\": null\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"groupSelections\": null,\n" +
                "                                \"cooperationNetworks\": [\n" +
                "                                    {\n" +
                "                                        \"validityStartDate\": \"2019-01-08\",\n" +
                "                                        \"validityEndDate\": \"2021-01-08\",\n" +
                "                                        \"enrollable\": true,\n" +
                "                                        \"id\": \"CN-1\",\n" +
                "                                        \"name\": {\n" +
                "                                            \"values\": {\n" +
                "                                                \"fi\": \"Verkosto 1\",\n" +
                "                                                \"sv\": \"Verkosto sv\",\n" +
                "                                                \"en\": \"Verkosto en\"\n" +
                "                                            }\n" +
                "                                        }\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"createdTime\": null,\n" +
                "                                \"updateTime\": null,\n" +
                "                                \"groupQuotas\": null,\n" +
                "                                \"status\": null,\n" +
                "                                \"creditsMin\": null,\n" +
                "                                \"creditsMax\": null,\n" +
                "                                \"teachingLanguage\": null,\n" +
                "                                \"minEduGuidanceArea\": null\n" +
                "                            },\n" +
                "                            {\n" +
                "                                \"realisationId\": \"ID2\",\n" +
                "                                \"realisationIdentifierCode\": \"IDENTIFIERCODE2\",\n" +
                "                                \"selections\": null,\n" +
                "                                \"questionSets\": null,\n" +
                "                                \"enrollmentStartDateTime\": \"2019-12-19T15:55:52.134+02:00\",\n" +
                "                                \"enrollmentEndDateTime\": \"2020-01-28T15:55:52.134+02:00\",\n" +
                "                                \"startDate\": \"2019-11-08\",\n" +
                "                                \"endDate\": \"2020-03-08\",\n" +
                "                                \"personReferences\": null,\n" +
                "                                \"minSeats\": null,\n" +
                "                                \"maxSeats\": null,\n" +
                "                                \"location\": null,\n" +
                "                                \"organisationSpecificDescriptions\": null,\n" +
                "                                \"studyElementReferences\": [\n" +
                "                                    {\n" +
                "                                        \"referenceIdentifier\": \"ID1\",\n" +
                "                                        \"referenceOrganizer\": \"TUNI\",\n" +
                "                                        \"referenceType\": \"ASSESSMENT_ITEM\",\n" +
                "                                        \"referenceAssessmentItemId\": \"AI-2\"\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"name\": {\n" +
                "                                    \"values\": {\n" +
                "                                        \"fi\": \"nimi2\"\n" +
                "                                    }\n" +
                "                                },\n" +
                "                                \"organisationReferences\": [\n" +
                "                                    {\n" +
                "                                        \"percent\": 100,\n" +
                "                                        \"organisationRole\": 1,\n" +
                "                                        \"target\": {\n" +
                "                                            \"organisationIdentifier\": \"TUNI\",\n" +
                "                                            \"organisationTkCode\": \"TUNI\",\n" +
                "                                            \"organisationName\": null,\n" +
                "                                            \"unitName\": null,\n" +
                "                                            \"nameDescription\": null,\n" +
                "                                            \"postalAddress\": null,\n" +
                "                                            \"streetAddress\": null,\n" +
                "                                            \"municipalityCode\": null,\n" +
                "                                            \"phone\": null,\n" +
                "                                            \"url\": null\n" +
                "                                        },\n" +
                "                                        \"description\": null\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"groupSelections\": null,\n" +
                "                                \"cooperationNetworks\": [\n" +
                "                                    {\n" +
                "                                        \"validityStartDate\": \"2019-01-08\",\n" +
                "                                        \"validityEndDate\": \"2021-01-08\",\n" +
                "                                        \"enrollable\": true,\n" +
                "                                        \"id\": \"CN-1\",\n" +
                "                                        \"name\": {\n" +
                "                                            \"values\": {\n" +
                "                                                \"fi\": \"Verkosto 1\",\n" +
                "                                                \"sv\": \"Verkosto sv\",\n" +
                "                                                \"en\": \"Verkosto en\"\n" +
                "                                            }\n" +
                "                                        }\n" +
                "                                    }\n" +
                "                                ],\n" +
                "                                \"createdTime\": null,\n" +
                "                                \"updateTime\": null,\n" +
                "                                \"groupQuotas\": null,\n" +
                "                                \"status\": null,\n" +
                "                                \"creditsMin\": null,\n" +
                "                                \"creditsMax\": null,\n" +
                "                                \"teachingLanguage\": null,\n" +
                "                                \"minEduGuidanceArea\": null\n" +
                "                            }\n" +
                "                        ],\n" +
                "                        \"type\": null,\n" +
                "                        \"assessmentItemId\": \"AI-1\",\n" +
                "                        \"creditsMin\": null,\n" +
                "                        \"creditsMax\": null\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"name\": {\n" +
                "                    \"values\": {\n" +
                "                        \"fi\": \"suoritustapa 1\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"completionOptionId\": \"CO-1\",\n" +
                "                \"description\": \"kuvaus\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": null,\n" +
                "                        \"realisations\": null,\n" +
                "                        \"type\": null,\n" +
                "                        \"assessmentItemId\": \"AI-2\",\n" +
                "                        \"creditsMin\": null,\n" +
                "                        \"creditsMax\": null\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"name\": null,\n" +
                "                \"completionOptionId\": \"CO-2\",\n" +
                "                \"description\": \"kuvaus\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"realisations\": [\n" +
                "            {\n" +
                "                \"realisationId\": \"ID3\",\n" +
                "                \"realisationIdentifierCode\": \"IDENTIFIERCODE3\",\n" +
                "                \"selections\": null,\n" +
                "                \"questionSets\": null,\n" +
                "                \"enrollmentStartDateTime\": \"2019-12-19T15:55:52.134+02:00\",\n" +
                "                \"enrollmentEndDateTime\": \"2020-01-28T15:55:52.134+02:00\",\n" +
                "                \"startDate\": \"2019-11-08\",\n" +
                "                \"endDate\": \"2020-03-08\",\n" +
                "                \"personReferences\": null,\n" +
                "                \"minSeats\": null,\n" +
                "                \"maxSeats\": null,\n" +
                "                \"location\": null,\n" +
                "                \"organisationSpecificDescriptions\": null,\n" +
                "                \"studyElementReferences\": [\n" +
                "                    {\n" +
                "                        \"referenceIdentifier\": \"ID1\",\n" +
                "                        \"referenceOrganizer\": \"TUNI\",\n" +
                "                        \"referenceType\": \"ASSESSMENT_ITEM\",\n" +
                "                        \"referenceAssessmentItemId\": \"AI-2\"\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"name\": {\n" +
                "                    \"values\": {\n" +
                "                        \"fi\": \"nimi3\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"organisationReferences\": [\n" +
                "                    {\n" +
                "                        \"percent\": 100,\n" +
                "                        \"organisationRole\": 1,\n" +
                "                        \"target\": {\n" +
                "                            \"organisationIdentifier\": \"TUNI\",\n" +
                "                            \"organisationTkCode\": \"TUNI\",\n" +
                "                            \"organisationName\": null,\n" +
                "                            \"unitName\": null,\n" +
                "                            \"nameDescription\": null,\n" +
                "                            \"postalAddress\": null,\n" +
                "                            \"streetAddress\": null,\n" +
                "                            \"municipalityCode\": null,\n" +
                "                            \"phone\": null,\n" +
                "                            \"url\": null\n" +
                "                        },\n" +
                "                        \"description\": null\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"groupSelections\": null,\n" +
                "                \"cooperationNetworks\": [\n" +
                "                    {\n" +
                "                        \"validityStartDate\": \"2019-01-08\",\n" +
                "                        \"validityEndDate\": \"2021-01-08\",\n" +
                "                        \"enrollable\": true,\n" +
                "                        \"id\": \"CN-1\",\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"Verkosto 1\",\n" +
                "                                \"sv\": \"Verkosto sv\",\n" +
                "                                \"en\": \"Verkosto en\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"createdTime\": null,\n" +
                "                \"updateTime\": null,\n" +
                "                \"groupQuotas\": null,\n" +
                "                \"status\": null,\n" +
                "                \"creditsMin\": null,\n" +
                "                \"creditsMax\": null,\n" +
                "                \"teachingLanguage\": null,\n" +
                "                \"minEduGuidanceArea\": null\n" +
                "            }\n" +
                "        ],\n" +
                "        \"sendingTime\": \"2020-01-08T15:55:52.134+02:00\",\n" +
                "        \"creditsMin\": 2.5,\n" +
                "        \"creditsMax\": 5,\n" +
                "        \"organisationReferences\": [\n" +
                "            {\n" +
                "                \"percent\": 100,\n" +
                "                \"organisationRole\": 1,\n" +
                "                \"target\": {\n" +
                "                    \"organisationIdentifier\": \"TUNI\",\n" +
                "                    \"organisationTkCode\": \"TUNI\",\n" +
                "                    \"organisationName\": null,\n" +
                "                    \"unitName\": null,\n" +
                "                    \"nameDescription\": null,\n" +
                "                    \"postalAddress\": null,\n" +
                "                    \"streetAddress\": null,\n" +
                "                    \"municipalityCode\": null,\n" +
                "                    \"phone\": null,\n" +
                "                    \"url\": null\n" +
                "                },\n" +
                "                \"description\": null\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertEquals(courseUnit.getStudyElementId(), result.getStudyElementId());
        assertEquals(courseUnit.getStudyElementIdentifierCode(), result.getStudyElementIdentifierCode());
        //assertEquals(courseUnit.getShortName(), result.getShortName());
        assertEquals("jaksonnimi 2 fi", result.getName().getValue("fi"));
        assertEquals("jaksonnimi 2 en", result.getName().getValue("en"));
        assertEquals("jaksonnimi 2 sv", result.getName().getValue("sv"));
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, result.getType());
        assertEquals(StudyStatus.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> courseUnit.getCooperationNetworks().stream()
                .anyMatch(cn -> cn.getId().equals(resultCn.getId())
                    && cn.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                    && cn.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                    && cn.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                )));
        assertNull(result.getRealisations());
        assertNull(result.getCompletionOptions().get(0).getAssessmentItems().get(0).getRealisations());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_removesAssessmentItemWithExistingRealisation_shouldFail() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(removedAssessmentItemEntity));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName, Arrays.asList(completionOptionEntity, completionOptionEntity2));
        courseUnit.setStatus(StudyStatus.ACTIVE);

        courseUnitRepository.create(courseUnit);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity.getAssessmentItemId());

        StudyElementReference removedReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), removedAssessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("ID1", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference), Collections.singletonList(network));

        realisationRepository.create(realisation);

        RealisationEntity realisationWithRefToRemovedAssessmentItem = EntityInitializer.getRealisationEntity("ID2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(removedReference), Collections.singletonList(network));

        realisationRepository.create(realisationWithRefToRemovedAssessmentItem);

        String updateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"completionOptions\": [\n" +
                "            {\n" +
                "                \"completionOptionId\": \"" + completionOptionEntity.getCompletionOptionId() + "\",\n" +
                "                \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\",\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"assessmentItemId\": \"" + assessmentItemEntity.getAssessmentItemId() + "\",\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"arvioinninkohde 1\"\n" +
                "                            }\n" +
                "                         }\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"completionOptionId\": \"" + completionOptionEntity2.getCompletionOptionId() + "\",\n" +
                "                \"description\": \"completion option in which assessment item is removed that has realisation reference \",\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"assessmentItemId\": \"NEWASSESSMENTITEM\",\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"uusi arvioinninkohde\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ]" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains(removedAssessmentItemEntity.getAssessmentItemId()));
        assertTrue(resp.getMessage().contains(realisationWithRefToRemovedAssessmentItem.getRealisationId()));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_removesAssessmentItemWithExistingRealisation_shouldReAddDenormalizedData() throws JMSException, IOException {
        String organizingOrganisationId = "TUNI";

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity assessmentItemEntity2 = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-3", "ID3");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Arrays.asList(assessmentItemEntity2, removedAssessmentItemEntity));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedString originalName = new LocalisedString("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName, Arrays.asList(completionOptionEntity, completionOptionEntity2));
        courseUnit.setStatus(StudyStatus.ACTIVE);

        courseUnitRepository.create(courseUnit);

        StudyElementReference studyElementReference = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity.getAssessmentItemId());

        StudyElementReference studyElementReference2 = DtoInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity2.getAssessmentItemId());

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("ID1", courseUnit.getOrganizingOrganisationId(),
            Arrays.asList(studyElementReference), Collections.singletonList(network));
        realisation.setName(new LocalisedString("Toteutus 1", "Realisation1", "Real1"));
        realisation.setStartDate(LocalDate.now().minusMonths(1));
        realisation.setEndDate(LocalDate.now().plusMonths(1));
        realisation.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(15));
        realisation.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(15));

        realisationRepository.create(realisation);

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("ID2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference2), Collections.singletonList(network));
        realisation2.setName(new LocalisedString("Toteutus 2", "Realisation2", "Real2"));
        realisation2.setStartDate(LocalDate.now().minusMonths(2));
        realisation2.setEndDate(LocalDate.now().plusMonths(2));
        realisation2.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(20));
        realisation2.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(20));

        realisationRepository.create(realisation2);

        String updateJson =
            "{\n" +
                "    \"courseUnit\": {\n" +
                "        \"studyElementId\": \"" + courseUnit.getStudyElementId() + "\",\n" +
                "        \"completionOptions\": [\n" +
                "            {\n" +
                "                \"completionOptionId\": \"" + completionOptionEntity.getCompletionOptionId() + "\",\n" +
                "                \"description\": \"aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtävien tekeminen, itsenäinen opiskelu sekä moodle-tentti\",\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"assessmentItemId\": \"" + assessmentItemEntity.getAssessmentItemId() + "\",\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"arvioinninkohde 1\"\n" +
                "                            }\n" +
                "                         }\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"completionOptionId\": \"" + completionOptionEntity2.getCompletionOptionId() + "\",\n" +
                "                \"description\": \"completion option in which one assessment item is removed \",\n" +
                "                \"assessmentItems\": [\n" +
                "                    {\n" +
                "                        \"assessmentItemId\": \"" + assessmentItemEntity2.getAssessmentItemId() + "\",\n" +
                "                        \"primaryCourseUnitId\": null,\n" +
                "                        \"name\": {\n" +
                "                            \"values\": {\n" +
                "                                \"fi\": \"uusi arvioinninkohde\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ]" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.OK);

        CourseUnitEntity createdCourseUnit = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnit.getOrganizingOrganisationId()).orElse(null);

        // Verify data denormalized
        List<CourseUnitRealisationEntity> courseUnitRealisations = createdCourseUnit.getCompletionOptions().stream()
            .filter(co -> co.getCompletionOptionId().equals(completionOptionEntity.getCompletionOptionId())).findFirst().get()
            .getAssessmentItems().stream().filter(ai -> ai.getAssessmentItemId().equals(assessmentItemEntity.getAssessmentItemId())).findFirst().get()
            .getRealisations();

        assertEquals(1, courseUnitRealisations.size());
        CourseUnitRealisationEntity courseUnitRealisation = courseUnitRealisations.get(0);

        assertEquals(realisation.getRealisationId(), courseUnitRealisation.getRealisationId());
        assertEquals(realisation.getRealisationIdentifierCode(), courseUnitRealisation.getRealisationIdentifierCode());
        assertEquals(realisation.getOrganizingOrganisationId(), courseUnitRealisation.getOrganizingOrganisationId());
        assertEquals(realisation.getName().getValue("fi"), courseUnitRealisation.getName().getValue("fi"));
        assertEquals(realisation.getName().getValue("en"), courseUnitRealisation.getName().getValue("en"));
        assertEquals(realisation.getName().getValue("sv"), courseUnitRealisation.getName().getValue("sv"));
        assertEquals(realisation.getStartDate(), courseUnitRealisation.getStartDate());
        assertEquals(realisation.getEndDate(), courseUnitRealisation.getEndDate());
        assertEquals(DateUtils.getFormatted(realisation.getEnrollmentStartDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentStartDateTime()));
        assertEquals(DateUtils.getFormatted(realisation.getEnrollmentEndDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentEndDateTime()));

        courseUnitRealisations = createdCourseUnit.getCompletionOptions().stream()
            .filter(co -> co.getCompletionOptionId().equals(completionOptionEntity2.getCompletionOptionId())).findFirst().get()
            .getAssessmentItems().stream().filter(ai -> ai.getAssessmentItemId().equals(assessmentItemEntity2.getAssessmentItemId())).findFirst().get()
            .getRealisations();

        assertEquals(1, courseUnitRealisations.size());
        courseUnitRealisation = courseUnitRealisations.get(0);

        assertEquals(realisation2.getRealisationId(), courseUnitRealisation.getRealisationId());
        assertEquals(realisation2.getRealisationIdentifierCode(), courseUnitRealisation.getRealisationIdentifierCode());
        assertEquals(realisation2.getOrganizingOrganisationId(), courseUnitRealisation.getOrganizingOrganisationId());
        assertEquals(realisation2.getName().getValue("fi"), courseUnitRealisation.getName().getValue("fi"));
        assertEquals(realisation2.getName().getValue("en"), courseUnitRealisation.getName().getValue("en"));
        assertEquals(realisation2.getName().getValue("sv"), courseUnitRealisation.getName().getValue("sv"));
        assertEquals(realisation2.getStartDate(), courseUnitRealisation.getStartDate());
        assertEquals(realisation2.getEndDate(), courseUnitRealisation.getEndDate());
        assertEquals(DateUtils.getFormatted(realisation2.getEnrollmentStartDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentStartDateTime()));
        assertEquals(DateUtils.getFormatted(realisation2.getEnrollmentEndDateTime()), DateUtils.getFormatted(courseUnitRealisation.getEnrollmentEndDateTime()));
    }

}
