package fi.uta.ristiinopiskelu.handler.integration.route.current.studymodule;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.OrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
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
import fi.uta.ristiinopiskelu.handler.service.StudyModuleService;
import fi.uta.ristiinopiskelu.messaging.message.current.DefaultResponse;
import fi.uta.ristiinopiskelu.messaging.message.current.MessageType;
import fi.uta.ristiinopiskelu.messaging.message.current.Status;
import fi.uta.ristiinopiskelu.messaging.message.current.courseunit.CreateCourseUnitRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.CreateStudyModuleRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studymodule.UpdateStudyModuleRequest;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyModuleRepository;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EmbeddedActiveMQInitializer.class)
@ExtendWith(EmbeddedElasticsearchInitializer.class)
@SpringBootTest(classes = TestEsConfig.class)
@ActiveProfiles("integration")
public class UpdateStudyModuleRouteIntegrationTest extends AbstractRouteIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStudyModuleRouteIntegrationTest.class);

    private JmsTemplate jmsTemplate;

    @Autowired
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        jmsTemplate.setReceiveTimeout(500000);
        this.jmsTemplate = jmsTemplate;
    }

    @Autowired
    private StudyModuleService studyModuleService;

    @Autowired
    private CourseUnitService courseUnitService;
    
    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private StudyModuleRepository studyModuleRepository;

    @Autowired
    private CourseUnitRepository courseUnitRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${general.messageSchema.version}")
    private int messageSchemaVersion;

    @BeforeEach
    public void setUp() {
        JmsHelper.setMessageSchemaVersion(this.messageSchemaVersion);

        List<String> organisationIds = Arrays.asList("TUNI", "METROP", "LAUREA");

        for(String organisationId : organisationIds) {
            if(!organisationService.findById(organisationId).isPresent()) {
                OrganisationEntity organisation = EntityInitializer.getOrganisationEntity(organisationId, organisationId,
                    new LocalisedString(organisationId, null, null), this.messageSchemaVersion);
                organisationService.create(modelMapper.map(organisation, OrganisationEntity.class));
            }
        }
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldSuccess() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
            "{\n" +
                "\t\"studyModule\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"status\": \"ARCHIVED\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"Kokonaisuuden nimi 2\",\n" +
                "\t\t\t\t\"en\": \"Kokonaisuuden nimi 2 Englanniksi\",\n" +
                "\t\t\t\t\"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ARCHIVED, updatedEntity.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_noCooperationNetworkNameOrStatusGiven_shouldFillCooperationNetworkFields() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";
        NetworkEntity networkEntity = persistNetworkEntity("CN-1", new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(networkEntity.getId(), new LocalisedString("HEIHEI", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));
        studyModuleEntity.setStatus(StudyStatus.ACTIVE);

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertTrue(entity.getCooperationNetworks().stream()
            .allMatch(createdCn -> network.getId().equals(createdCn.getId())
                && network.getName().getValue("fi").equals(createdCn.getName().getValue("fi"))
                && network.getName().getValue("en").equals(createdCn.getName().getValue("en"))
                && network.getName().getValue("sv").equals(createdCn.getName().getValue("sv"))));

        String updateJson =
            "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"cooperationNetworks\": [\n" +
                "            {\n" +
                "                \"id\": \"" + networkEntity.getId() +"\",\n" +
                "                \"enrollable\": true\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());
        assertTrue(updatedEntity.getCooperationNetworks().stream()
            .allMatch(updatedCn -> networkEntity.getId().equals(updatedCn.getId())
                && networkEntity.getName().getValue("fi").equals(updatedCn.getName().getValue("fi"))
                && networkEntity.getName().getValue("en").equals(updatedCn.getName().getValue("en"))
                && networkEntity.getName().getValue("sv").equals(updatedCn.getName().getValue("sv"))
            ));
    }

    @Test
    public void testSendingUpdateStudyModuleMessageWithEmptyCooperationNetworks_shouldSuccess() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
            "{\n" +
                "\t\"studyModule\": {\n" +
                "\t\t\"studyElementId\": \"ID1\",\n" +
                "\t\t\"status\": \"ARCHIVED\",\n" +
                "\t\t\"name\": {\n" +
                "\t\t\t\"values\": {\n" +
                "\t\t\t\t\"fi\": \"Kokonaisuuden nimi 2\",\n" +
                "\t\t\t\t\"en\": \"Kokonaisuuden nimi 2 Englanniksi\",\n" +
                "\t\t\t\t\"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\"\n" +
                "\t\t\t\t}\n" +
                "\t\t\t},\n" +
                "\t\t\"cooperationNetworks\": [\n" +
                "\t\t\t]\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ARCHIVED, updatedEntity.getStatus());
    }
    
    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailHasExtraField() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
            "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"thisdoesnotexist\": \"randomcontent\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        assertNotNull(responseMessage);

        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldSuccessAndCreateHistory() throws JMSException, IOException {
        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            "CN-1", new LocalisedString("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation("TUNI", "TUNI");
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleEntity studyModuleEntity = EntityInitializer.getStudyModuleEntity(
            "ID1", "RAIRAI", organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network),
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null));

        studyModuleRepository.create(studyModuleEntity);

        // Verify created study module exists
        StudyModuleEntity entity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisation.getOrganisationTkCode()).orElse(null);

        assertNotNull(entity.getCreatedTime());
        assertNotNull(entity);
        assertEquals(studyModuleEntity.getName().getValue("fi"), entity.getName().getValue("fi"));

        String updateJson =
            "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\",\n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\",\n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertNotNull(updatedEntity.getCreatedTime());
        assertEquals(updatedEntity.getCreatedTime(), entity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));

        List<StudyModuleEntity> studyModuleHistoryEntities = HistoryHelper.queryHistoryIndex(elasticsearchTemplate,
            "opintokokonaisuudet-history", StudyModuleEntity.class);
        assertNotNull(studyModuleHistoryEntities);
        assertEquals(1, studyModuleHistoryEntities.size());

        StudyModuleEntity historyEntity = studyModuleHistoryEntities.get(0);
        assertEquals(studyModuleEntity.getStudyElementId(), historyEntity.getStudyElementId());
        assertEquals(studyModuleEntity.getStudyElementIdentifierCode(), historyEntity.getStudyElementIdentifierCode());
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getCreatedTime()), DateUtils.getFormatted(historyEntity.getCreatedTime()));
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getSendingTime()), DateUtils.getFormatted(historyEntity.getSendingTime()));
        assertEquals(1, historyEntity.getCooperationNetworks().size());
        assertEquals(studyModuleEntity.getCooperationNetworks().get(0).getId(), historyEntity.getCooperationNetworks().get(0).getId());
        assertEquals(studyModuleEntity.getName().getValue("fi"), historyEntity.getName().getValue("fi"));
        assertEquals(studyModuleEntity.getName().getValue("sv"), historyEntity.getName().getValue("sv"));
        assertEquals(studyModuleEntity.getName().getValue("en"), historyEntity.getName().getValue("en"));

        String updateJson2 =
            "{\n" +
                "    \"studyModule\": {\n" +
                "        \"studyElementId\": \"ID1\",\n" +
                "        \"studyElementIdentifierCode\": \"RAIRAI\",\n" +
                "        \"name\": {\n" +
                "            \"values\": {\n" +
                "                \"fi\": \"Kokonaisuuden nimi 3\",\n" +
                "                \"en\": \"Kokonaisuuden nimi 3 Englanniksi\",\n" +
                "                \"sv\": \"Kokonaisuuden nimi 3 Ruotsiksi\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson2, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity2 = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModuleEntity.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity2);
        assertNotNull(updatedEntity2.getCreatedTime());
        assertEquals(DateUtils.getFormatted(studyModuleEntity.getCreatedTime()), DateUtils.getFormatted(updatedEntity2.getCreatedTime()));
        assertNotNull(updatedEntity2.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 3", updatedEntity2.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 3 Englanniksi", updatedEntity2.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 3 Ruotsiksi", updatedEntity2.getName().getValue("sv"));

        studyModuleHistoryEntities = HistoryHelper.queryHistoryIndex(elasticsearchTemplate,
            "opintokokonaisuudet-history", StudyModuleEntity.class);
        assertNotNull(studyModuleHistoryEntities);
        assertEquals(2, studyModuleHistoryEntities.size());

        StudyModuleEntity historyEntity2 = studyModuleHistoryEntities.stream()
            .sorted(Comparator.comparing(StudyModuleEntity::getUpdateTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
            .findFirst()
            .orElse(null);
        assertEquals(updatedEntity.getStudyElementId(), historyEntity2.getStudyElementId());
        assertEquals(updatedEntity.getStudyElementIdentifierCode(), historyEntity2.getStudyElementIdentifierCode());
        assertEquals(updatedEntity.getCreatedTime(), historyEntity2.getCreatedTime());
        assertEquals(updatedEntity.getSendingTime(), historyEntity2.getSendingTime());
        assertEquals(updatedEntity.getUpdateTime(), historyEntity2.getUpdateTime());
        assertEquals(1, historyEntity2.getCooperationNetworks().size());
        assertEquals(updatedEntity.getCooperationNetworks().get(0).getId(), historyEntity2.getCooperationNetworks().get(0).getId());
        assertEquals(updatedEntity.getName().getValue("fi"), historyEntity2.getName().getValue("fi"));
        assertEquals(updatedEntity.getName().getValue("sv"), historyEntity2.getName().getValue("sv"));
        assertEquals(updatedEntity.getName().getValue("en"), historyEntity2.getName().getValue("en"));
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingCourseUnitAndRemoveCurrentReference() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Create new course unit to be added to study module
        CourseUnitEntity courseUnitEntityToAddRefs = EntityInitializer.getCourseUnitEntity("CU-2", "CU-CODE2",
            organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network), new LocalisedString("OJ 2", null, null));

        courseUnitRepository.create(courseUnitEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify parent study module updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify one course unit is found with parent reference to parent study module
        List<CourseUnitEntity> courseUnitEntities = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);

        assertNotNull(courseUnitEntities);
        assertTrue(courseUnitEntities.size() == 1);
        assertEquals(courseUnitEntityToAddRefs.getStudyElementId(), courseUnitEntities.get(0).getStudyElementId());
        assertEquals(courseUnitEntityToAddRefs.getStudyElementIdentifierCode(), courseUnitEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(courseUnitEntityToAddRefs.getOrganizingOrganisationId(), courseUnitEntities.get(0).getOrganizingOrganisationId());

        // Verify existing reference to course unit is removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldSuccessAndNotMultiplyParentRefs() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO subCourseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModuleRefToBeRemoved = DtoInitializer.getCreateStudyModuleRequestDTO("ID3", "RAIRAI3",
            new LocalisedString("Alikokonaisuus", "Alikokonaisuus 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID2", "RAIRAI2",
            new LocalisedString("Alikokonaisuus", "Alikokonaisuus 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5),
            Arrays.asList(subStudyModule, subCourseUnit, subStudyModuleRefToBeRemoved));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        // Verify course unit created and has parent reference
        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            subCourseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Verify course unit created and has parent reference
        StudyModuleEntity studyModuleEntityCreatedWithModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleEntityCreatedWithModule);
        assertEquals(1, studyModuleEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, studyModuleEntityCreatedWithModule.getStatus());

        // Verify course unit created and has parent reference
        StudyModuleEntity studyModuleRefToBeRemoved = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModuleRefToBeRemoved.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleRefToBeRemoved);
        assertEquals(1, studyModuleRefToBeRemoved.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleRefToBeRemoved.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleRefToBeRemoved.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, studyModuleRefToBeRemoved.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, studyModuleRefToBeRemoved.getStatus());

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity("OK-2", "OK-CODE2",
            organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network), new LocalisedString("OK 2", null, null));

        studyModuleRepository.create(studyModuleEntityToAddRefs);

        // Send update study module message with same sub study elements and one new
        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"studyElementIdentifierCode\": \""+ courseUnitEntityCreatedWithModule.getStudyElementIdentifierCode() + "\",\n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"studyElementIdentifierCode\": \""+ studyModuleEntityToAddRefs.getStudyElementIdentifierCode() + "\",\n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(), organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify parent study module updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify parent reference is not duplicated
        studyModuleEntityCreatedWithModule = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleEntityCreatedWithModule);
        assertEquals(1, studyModuleEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, studyModuleEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, studyModuleEntityCreatedWithModule.getStatus());

        // Verify parent reference is not duplicated
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            subCourseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Verify parent reference is added
        StudyModuleEntity studyModuleRefsAdded = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
                studyModuleEntityToAddRefs.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode())
            .orElse(null);
        assertNotNull(studyModuleRefsAdded);
        assertEquals(1, studyModuleRefsAdded.getParents().size());
        assertEquals(studyModule.getStudyElementId(), studyModuleRefsAdded.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), studyModuleRefsAdded.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, studyModuleRefsAdded.getParents().get(0).getReferenceType());

        // Verify study module parent reference was removed
        studyModuleRefToBeRemoved = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModuleRefToBeRemoved.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(studyModuleRefToBeRemoved);
        assertEquals(0, studyModuleRefToBeRemoved.getParents().size());
        assertEquals(StudyStatus.ACTIVE, studyModuleRefToBeRemoved.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingStudyModule() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity("OK-2", "OK-CODE2",
            organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network), new LocalisedString("OK 2", null, null));

        studyModuleRepository.save(studyModuleEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntities = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntities);
        assertTrue(studyModuleEntities.size() == 1);
        assertEquals(studyModuleEntityToAddRefs.getStudyElementId(), studyModuleEntities.get(0).getStudyElementId());
        assertEquals(studyModuleEntityToAddRefs.getStudyElementIdentifierCode(), studyModuleEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(studyModuleEntityToAddRefs.getOrganizingOrganisationId(), studyModuleEntities.get(0).getOrganizingOrganisationId());

        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingCourseUnitAndRemoveCurrentReferenceContainsForwardSlashes() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU/ID1", "JEP/JEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID/1", "RAI/RAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Create new course unit to be added to study module
        CourseUnitEntity courseUnitEntityToAddRefs = EntityInitializer.getCourseUnitEntity("CU/2", "CU/CODE2",
            organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network), new LocalisedString("OJ 2", null, null));

        courseUnitRepository.create(courseUnitEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"" + studyModule.getStudyElementId() + "\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify parent study module updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify one course unit is found with parent reference to parent study module
        List<CourseUnitEntity> courseUnitEntities = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);

        assertNotNull(courseUnitEntities);
        assertTrue(courseUnitEntities.size() == 1);
        assertEquals(courseUnitEntityToAddRefs.getStudyElementId(), courseUnitEntities.get(0).getStudyElementId());
        assertEquals(courseUnitEntityToAddRefs.getStudyElementIdentifierCode(), courseUnitEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(courseUnitEntityToAddRefs.getOrganizingOrganisationId(), courseUnitEntities.get(0).getOrganizingOrganisationId());

        // Verify existing reference to course unit is removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailSubElementRefNotFound() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        // SEND UPDATE STUDY MODULE MESSAGE
        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \"NOTFOUND_ID\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"NOT_FOUND\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldRemoveAllReferencesWithEmptySubElements() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID123", "SUBSTUDYMODULE-1",
            new LocalisedString("Alikokonaisuuden nimi 1", "Alikokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Arrays.asList(courseUnit, subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        StudyModuleEntity createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(1, createdSubStudyModuleEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyStatus.ACTIVE, createdSubStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Send update study module to remove all sub elements
        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": []\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntitiesWithRef = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntitiesWithRef);
        assertTrue(studyModuleEntitiesWithRef.size() == 0);

        List<CourseUnitEntity> courseUnitEntitiesWithRef = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);;
        assertNotNull(courseUnitEntitiesWithRef);
        assertTrue(courseUnitEntitiesWithRef.size() == 0);

        // Double check references have been removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertNotNull(courseUnitEntityCreatedWithModule.getUpdateTime());
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());

        createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(0, createdSubStudyModuleEntity.getParents().size());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldRemoveAllReferencesWithNullSubElements() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO subStudyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID123", "SUBSTUDYMODULE-1",
            new LocalisedString("Alikokonaisuuden nimi 1", "Alikokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), null);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Arrays.asList(courseUnit, subStudyModule));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        StudyModuleEntity createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(1, createdSubStudyModuleEntity.getParents().size());
        assertEquals(studyModule.getStudyElementId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), createdSubStudyModuleEntity.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyStatus.ACTIVE, createdSubStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(1, courseUnitEntityCreatedWithModule.getParents().size());
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Send update study module to remove all sub elements
        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": null\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntitiesWithRef = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntitiesWithRef);
        assertTrue(studyModuleEntitiesWithRef.size() == 0);

        List<CourseUnitEntity> courseUnitEntitiesWithRef = courseUnitRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), CourseUnitEntity.class);;
        assertNotNull(courseUnitEntitiesWithRef);
        assertTrue(courseUnitEntitiesWithRef.size() == 0);

        // Double check references have been removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertNotNull(courseUnitEntityCreatedWithModule.getUpdateTime());
        assertEquals(0, courseUnitEntityCreatedWithModule.getParents().size());

        createdSubStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            subStudyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdSubStudyModuleEntity);
        assertNotNull(createdSubStudyModuleEntity.getCreatedTime());
        assertEquals(0, createdSubStudyModuleEntity.getParents().size());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldUpdateParentAndAddReferencesToExistingStudyModuleButNotUpdateSubElementFields() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity(
            "OK-2", "OK-CODE2", organisationReference.getOrganisation().getOrganisationTkCode(),
            Collections.singletonList(network), new LocalisedString("OK 2", "OK 2 en", null));

        studyModuleRepository.save(studyModuleEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ courseUnitEntityCreatedWithModule.getStudyElementId() + "\", \n" +
                "                \"type\": \"COURSE_UNIT\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ],\n" +
                "                \"name\": { \n" +
                "                    \"values\": { \n" +
                "                        \"fi\": \"Opintojakson nimi 2\", \n" +
                "                        \"en\": \"Opintojakson nimi 2 Englanniksi\", \n" +
                "                        \"sv\": \"Opintojakson nimi 2 Ruotsiksi\" \n" +
                "                    } \n" +
                "               }\n" +
                "            },\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ],\n" +
                "                \"name\": { \n" +
                "                    \"values\": { \n" +
                "                        \"fi\": \"OK 3\", \n" +
                "                        \"en\": \"OK 3 en\", \n" +
                "                        \"sv\": \"OK 3 sv\" \n" +
                "                    } \n" +
                "               }\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.OK);

        // Verify created study module exists
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNotNull(updatedEntity.getUpdateTime());
        assertEquals("Kokonaisuuden nimi 2", updatedEntity.getName().getValue("fi"));
        assertEquals("Kokonaisuuden nimi 2 Englanniksi", updatedEntity.getName().getValue("en"));
        assertEquals("Kokonaisuuden nimi 2 Ruotsiksi", updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        List<StudyModuleEntity> studyModuleEntities = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntities);
        assertTrue(studyModuleEntities.size() == 1);
        assertEquals(studyModuleEntityToAddRefs.getStudyElementId(), studyModuleEntities.get(0).getStudyElementId());
        assertEquals(studyModuleEntityToAddRefs.getStudyElementIdentifierCode(), studyModuleEntities.get(0).getStudyElementIdentifierCode());
        assertEquals(studyModuleEntityToAddRefs.getOrganizingOrganisationId(), studyModuleEntities.get(0).getOrganizingOrganisationId());
        assertEquals(studyModuleEntityToAddRefs.getName().getValue("fi"), studyModuleEntities.get(0).getName().getValue("fi"));
        assertEquals(studyModuleEntityToAddRefs.getName().getValue("en"), studyModuleEntities.get(0).getName().getValue("en"));
        assertEquals(studyModuleEntityToAddRefs.getName().getValue("sv"), studyModuleEntities.get(0).getName().getValue("sv"));

        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
        assertEquals(courseUnit.getName().getValue("fi"), courseUnitEntityCreatedWithModule.getName().getValue("fi"));
        assertEquals(courseUnit.getName().getValue("en"), courseUnitEntityCreatedWithModule.getName().getValue("en"));
        assertEquals(courseUnit.getName().getValue("sv"), courseUnitEntityCreatedWithModule.getName().getValue("sv"));
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailAndNotUpdateParentAndAddReferencesToExistingStudyModule() throws JMSException, IOException {
        String organisingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation organisation = DtoInitializer.getOrganisation(organisingOrganisationId, organisingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5));

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(organisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.singletonList(courseUnit));

        CreateStudyModuleRequest req = new CreateStudyModuleRequest();
        req.setStudyModules(Collections.singletonList(studyModule));

        Message responseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, req, organisation.getOrganisationTkCode());
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);

        StudyModuleEntity createdStudyModuleEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(createdStudyModuleEntity);
        assertNotNull(createdStudyModuleEntity.getCreatedTime());
        assertEquals(StudyStatus.ACTIVE, createdStudyModuleEntity.getStatus());

        CourseUnitEntity courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());

        // Create new study module to be added to study module
        StudyModuleEntity studyModuleEntityToAddRefs = EntityInitializer.getStudyModuleEntity("OK-2", "OK-CODE2",
            organisationReference.getOrganisation().getOrganisationTkCode(), Collections.singletonList(network), new LocalisedString("OK 2", null, null));

        studyModuleRepository.save(studyModuleEntityToAddRefs);

        // SEND UPDATE STUDY MODULE MESSAGE

        String updateJson =
            "{\n" +
                "    \"studyModule\": { \n" +
                "        \"studyElementId\": \"ID1\", \n" +
                "        \"EXTRAFIELD\": \"ASDASD\", \n" +
                "        \"UNKNOWNFIELD\": \"ASDASD\", \n" +
                "        \"name\": { \n" +
                "            \"values\": { \n" +
                "                \"fi\": \"Kokonaisuuden nimi 2\", \n" +
                "                \"en\": \"Kokonaisuuden nimi 2 Englanniksi\", \n" +
                "                \"sv\": \"Kokonaisuuden nimi 2 Ruotsiksi\" \n" +
                "            } \n" +
                "        }, \n" +
                "        \"subElements\": [\n" +
                "            {\n" +
                "                \"studyElementId\": \""+ studyModuleEntityToAddRefs.getStudyElementId() + "\", \n" +
                "                \"type\": \"STUDY_MODULE\",\n" +
                "                \"organisationReferences\": [\n" +
                "                   {\n" +
                "                       \"organisationRole\": 1,\n" +
                "                       \"target\": {\n" +
                "                           \"organisationTkCode\": \"TUNI\"\n" +
                "                       }\n" +
                "                   }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_STUDYMODULE_REQUEST.name(),
            organisationReference.getOrganisation().getOrganisationTkCode());
        DefaultResponse updResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertTrue(updResp.getStatus() == Status.FAILED);

        // Verify created study module exists but is not updated
        StudyModuleEntity updatedEntity = studyModuleService.findByStudyElementIdAndOrganizingOrganisationId(
            studyModule.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);

        assertNotNull(updatedEntity);
        assertEquals(updatedEntity.getCreatedTime(), createdStudyModuleEntity.getCreatedTime());
        assertNull(updatedEntity.getUpdateTime());
        assertEquals(studyModule.getName().getValue("fi"), updatedEntity.getName().getValue("fi"));
        assertEquals(studyModule.getName().getValue("en"), updatedEntity.getName().getValue("en"));
        assertEquals(studyModule.getName().getValue("sv"), updatedEntity.getName().getValue("sv"));
        assertEquals(StudyStatus.ACTIVE, updatedEntity.getStatus());

        // Verify no references added
        List<StudyModuleEntity> studyModuleEntities = studyModuleRepository.findByStudyElementReference(studyModule.getStudyElementId(),
            organisationReference.getOrganisation().getOrganisationTkCode(), StudyModuleEntity.class);
        assertNotNull(studyModuleEntities);
        assertTrue(studyModuleEntities.size() == 0);

        // Verify reference not removed
        courseUnitEntityCreatedWithModule = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), organisationReference.getOrganisation().getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntityCreatedWithModule);
        assertEquals(studyModule.getStudyElementId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceIdentifier());
        assertEquals(createdStudyModuleEntity.getOrganizingOrganisationId(), courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, courseUnitEntityCreatedWithModule.getParents().get(0).getReferenceType());
        assertEquals(StudyStatus.ACTIVE, courseUnitEntityCreatedWithModule.getStatus());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldAllowCourseUnitReferencesFromDifferentOrganisationInSameNetwork() throws JMSException {
        String studyModuleOrganisingOrganisationId = "METROP";
        String courseUnitOrganisingOrganisationId = "LAUREA";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(studyModuleOrganisingOrganisationId, courseUnitOrganisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation studyModuleOrganisation = DtoInitializer.getOrganisation(studyModuleOrganisingOrganisationId, studyModuleOrganisingOrganisationId);
        Organisation courseUnitOrganisation = DtoInitializer.getOrganisation(courseUnitOrganisingOrganisationId, courseUnitOrganisingOrganisationId);
        OrganisationReference studyModuleOrganisationReference = DtoInitializer.getOrganisationReference(studyModuleOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference courseUnitOrganisationReference = DtoInitializer.getOrganisationReference(courseUnitOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(studyModuleOrganisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.emptyList());

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(courseUnitOrganisationReference), new BigDecimal(2.5), new BigDecimal(5));

        // LAUREA creates the courseunit
        CreateCourseUnitRequest createCourseUnitRequest = new CreateCourseUnitRequest();
        createCourseUnitRequest.setCourseUnits(Collections.singletonList(courseUnit));

        Message courseUnitResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createCourseUnitRequest, courseUnitOrganisation.getOrganisationTkCode());
        DefaultResponse courseUnitResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(courseUnitResponseMessage);
        assertTrue(courseUnitResp.getStatus() == Status.OK);

        // METROP creates the studymodule
        CreateStudyModuleRequest createStudyModuleRequest = new CreateStudyModuleRequest();
        createStudyModuleRequest.setStudyModules(Collections.singletonList(studyModule));

        Message studyModuleResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleResponseMessage);
        assertTrue(studyModuleResp.getStatus() == Status.OK);

        // METROP updates the studymodule to include the LAUREA courseunit
        studyModule.setSubElements(Collections.singletonList(courseUnit));

        UpdateStudyModuleRequest updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModuleWriteDTO.class));

        Message studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.OK);

        // validate courseunit parent references
        CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnitOrganisation.getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntity);
        assertNotNull(courseUnitEntity.getParents());
        assertEquals(1, courseUnitEntity.getParents().size());

        StudyElementReference studyModuleReference = courseUnitEntity.getParents().get(0);

        assertEquals(studyModule.getStudyElementId(), studyModuleReference.getReferenceIdentifier());
        assertEquals(studyModuleOrganisation.getOrganisationTkCode(), studyModuleReference.getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, studyModuleReference.getReferenceType());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_cooperationNetworksEmpty_shouldSucceed() throws JMSException {
        String studyModuleOrganisingOrganisationId = "METROP";
        String courseUnitOrganisingOrganisationId = "LAUREA";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(studyModuleOrganisingOrganisationId, courseUnitOrganisingOrganisationId));

        CooperationNetwork network = DtoInitializer.getCooperationNetwork(
            networkEntity.getId(), networkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation studyModuleOrganisation = DtoInitializer.getOrganisation(studyModuleOrganisingOrganisationId, studyModuleOrganisingOrganisationId);
        Organisation courseUnitOrganisation = DtoInitializer.getOrganisation(courseUnitOrganisingOrganisationId, courseUnitOrganisingOrganisationId);
        OrganisationReference studyModuleOrganisationReference = DtoInitializer.getOrganisationReference(studyModuleOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference courseUnitOrganisationReference = DtoInitializer.getOrganisationReference(courseUnitOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(studyModuleOrganisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.emptyList());

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(network),
            Collections.singletonList(courseUnitOrganisationReference), new BigDecimal(2.5), new BigDecimal(5));

        // LAUREA creates the courseunit
        CreateCourseUnitRequest createCourseUnitRequest = new CreateCourseUnitRequest();
        createCourseUnitRequest.setCourseUnits(Collections.singletonList(courseUnit));

        Message courseUnitResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createCourseUnitRequest, courseUnitOrganisation.getOrganisationTkCode());
        DefaultResponse courseUnitResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(courseUnitResponseMessage);
        assertTrue(courseUnitResp.getStatus() == Status.OK);

        // METROP creates the studymodule
        CreateStudyModuleRequest createStudyModuleRequest = new CreateStudyModuleRequest();
        createStudyModuleRequest.setStudyModules(Collections.singletonList(studyModule));

        Message studyModuleResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleResponseMessage);
        assertTrue(studyModuleResp.getStatus() == Status.OK);

        // METROP updates the studymodule to include the LAUREA courseunit
        studyModule.setSubElements(Collections.singletonList(courseUnit));

        UpdateStudyModuleRequest updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModuleWriteDTO.class));

        Message studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.OK);

        // METROP updates the studymodule's cooperationNetworks to empty
        studyModule.setCooperationNetworks(Collections.emptyList());
        courseUnit.setCooperationNetworks(Collections.emptyList());

        studyModule.setSubElements(Collections.singletonList(courseUnit));
        updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModuleWriteDTO.class));

        studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.OK);

        // validate courseunit parent references
        CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnitOrganisation.getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntity);
        assertNotNull(courseUnitEntity.getParents());
        assertEquals(1, courseUnitEntity.getParents().size());

        StudyElementReference studyModuleReference = courseUnitEntity.getParents().get(0);

        assertEquals(studyModule.getStudyElementId(), studyModuleReference.getReferenceIdentifier());
        assertEquals(studyModuleOrganisation.getOrganisationTkCode(), studyModuleReference.getReferenceOrganizer());
        assertEquals(StudyElementType.STUDY_MODULE, studyModuleReference.getReferenceType());
    }

    @Test
    public void testSendingUpdateStudyModuleMessage_shouldFailCourseUnitReferencesFromDifferentOrganisationInDifferentNetwork() throws JMSException {
        String studyModuleOrganisingOrganisationId = "METROP";
        String courseUnitOrganisingOrganisationId = "LAUREA";

        NetworkEntity studyModuleNetworkEntity = persistNetworkEntity("CN-1",
            new LocalisedString("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(studyModuleOrganisingOrganisationId));
        CooperationNetwork studyModuleNetwork = DtoInitializer.getCooperationNetwork(
            studyModuleNetworkEntity.getId(), studyModuleNetworkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        NetworkEntity courseUnitNetworkEntity = persistNetworkEntity("CN-2",
            new LocalisedString("Verkosto 2", "Verkosto en", "Verkosto sv"), Arrays.asList(courseUnitOrganisingOrganisationId));
        CooperationNetwork courseUnitNetwork = DtoInitializer.getCooperationNetwork(
            courseUnitNetworkEntity.getId(), courseUnitNetworkEntity.getName(), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        Organisation studyModuleOrganisation = DtoInitializer.getOrganisation(studyModuleOrganisingOrganisationId, studyModuleOrganisingOrganisationId);
        Organisation courseUnitOrganisation = DtoInitializer.getOrganisation(courseUnitOrganisingOrganisationId, courseUnitOrganisingOrganisationId);
        OrganisationReference studyModuleOrganisationReference = DtoInitializer.getOrganisationReference(studyModuleOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);
        OrganisationReference courseUnitOrganisationReference = DtoInitializer.getOrganisationReference(courseUnitOrganisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        StudyModuleWriteDTO studyModule = DtoInitializer.getCreateStudyModuleRequestDTO("ID1", "RAIRAI",
            new LocalisedString("Kokonaisuuden nimi 1", "Kokonaisuuden nimi 1 Englanniksi", null), Collections.singletonList(studyModuleNetwork),
            Collections.singletonList(studyModuleOrganisationReference), new BigDecimal(2.5), new BigDecimal(5), Collections.emptyList());

        CourseUnitWriteDTO courseUnit = DtoInitializer.getCreateCourseUnitRequestDTO("CU-ID1", "JEPJEP",
            new LocalisedString("Opintojakson nimi 1", "Opintojakson nimi 1 Englanniksi", null), Collections.singletonList(courseUnitNetwork),
            Collections.singletonList(courseUnitOrganisationReference), new BigDecimal(2.5), new BigDecimal(5));

        // LAUREA creates the courseunit
        CreateCourseUnitRequest createCourseUnitRequest = new CreateCourseUnitRequest();
        createCourseUnitRequest.setCourseUnits(Collections.singletonList(courseUnit));

        Message courseUnitResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createCourseUnitRequest, courseUnitOrganisation.getOrganisationTkCode());
        DefaultResponse courseUnitResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(courseUnitResponseMessage);
        assertTrue(courseUnitResp.getStatus() == Status.OK);

        // METROP creates the studymodule
        CreateStudyModuleRequest createStudyModuleRequest = new CreateStudyModuleRequest();
        createStudyModuleRequest.setStudyModules(Collections.singletonList(studyModule));

        Message studyModuleResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, createStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleResponseMessage);
        assertTrue(studyModuleResp.getStatus() == Status.OK);

        // METROP updates the studymodule to include the LAUREA courseunit
        studyModule.setSubElements(Collections.singletonList(courseUnit));

        UpdateStudyModuleRequest updateStudyModuleRequest = new UpdateStudyModuleRequest();
        updateStudyModuleRequest.setStudyModule(this.modelMapper.map(studyModule, StudyModuleWriteDTO.class));

        Message studyModuleUpdateResponseMessage = JmsHelper.sendAndReceiveObject(jmsTemplate, updateStudyModuleRequest, studyModuleOrganisation.getOrganisationTkCode());
        DefaultResponse studyModuleUpdateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(studyModuleUpdateResponseMessage);
        assertTrue(studyModuleUpdateResp.getStatus() == Status.FAILED);

        // validate courseunit parent references
        CourseUnitEntity courseUnitEntity = courseUnitService.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnit.getStudyElementId(), courseUnitOrganisation.getOrganisationTkCode()).orElse(null);
        assertNotNull(courseUnitEntity);
        assertTrue(CollectionUtils.isEmpty(courseUnitEntity.getParents()));
    }
}
