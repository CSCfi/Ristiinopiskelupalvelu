package fi.uta.ristiinopiskelu.handler.integration.route.current.courseunit;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Organisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.OrganisationRole;
import fi.uta.ristiinopiskelu.datamodel.entity.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.*;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.NetworkOrganisationEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.network.ValidityEntity;
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
import fi.uta.ristiinopiskelu.messaging.message.current.notification.CompositeIdentifiedEntityModifiedNotification;
import fi.uta.ristiinopiskelu.persistence.repository.CourseUnitRepository;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.RealisationRepository;
import fi.uta.ristiinopiskelu.persistence.utils.DateUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

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
                    new LocalisedStringEntity(organisationId, null, null), this.messageSchemaVersion);
                organisation.setSchemaVersion(this.messageSchemaVersion);
                organisationService.create(organisation);
            }
        }
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_containsSpecialCharactersAndXss_shoudlSucceedAndTagsRemoved() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "ID1",
                		"name": {
                			"values": {
                				"en": "<script type='text/javascript'>xss here</script>",
                				"sv": "<strong>blaah</strong> jaksonnimi2 sv",
                				"fi": "<xc jaksonnimi2 fi"
                			}
                		}
                	}
                }
            """;

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
    public void testSendingUpdateCourseUnitMessage_shouldSucceed() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "ID1",
                		"name": {
                			"values": {
                				"fi": "jaksonnimi2 fi",
                				"en": "jaksonnimi2 en",
                				"sv": "jaksonnimi2 sv"
                			}
                		}
                	}
                }
            """;

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ACTIVE, result.getStatus());
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

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().minusDays(2));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        ValidityEntity validity = new ValidityEntity(ValidityEntity.ContinuityEntity.FIXED, OffsetDateTime.now().minusYears(1), OffsetDateTime.now().minusDays(2));
        NetworkEntity networkEntity = EntityInitializer.getNetworkEntity(network.getId(), network.getName(),
            Collections.singletonList(new NetworkOrganisationEntity(organizingOrganisationId, true, validity)),
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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> courseUnit.getCooperationNetworks().stream()
                .anyMatch(cn -> cn.getId().equals(resultCn.getId())
                    && cn.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                    && cn.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                    && cn.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
                )));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_networksChanged_shouldNotifyOriginalAndNewNetworkMembers() throws JMSException {
        String organizingOrganisationId = "TUNI";
        String removedOrganisationId = "COURSEUNIT-REMOVED-ORG";
        String addedOrganisationId = "COURSEUNIT-ADDED-ORG";

        OrganisationEntity removedOrganisation = EntityInitializer.getOrganisationEntity(removedOrganisationId, removedOrganisationId,
            new LocalisedStringEntity(removedOrganisationId, null, null), messageSchemaVersion);
        removedOrganisation.setNotificationsEnabled(true);
        organisationService.create(removedOrganisation);

        OrganisationEntity addedOrganisation = EntityInitializer.getOrganisationEntity(addedOrganisationId, addedOrganisationId,
            new LocalisedStringEntity(addedOrganisationId, null, null), messageSchemaVersion);
        addedOrganisation.setNotificationsEnabled(true);
        organisationService.create(addedOrganisation);

        NetworkEntity originalNetwork = persistNetworkEntity("CN-COURSEUNIT-ORIGINAL",
            new LocalisedStringEntity("Original network", null, null), Arrays.asList(organizingOrganisationId, removedOrganisationId));
        NetworkEntity updatedNetwork = persistNetworkEntity("CN-COURSEUNIT-UPDATED",
            new LocalisedStringEntity("Updated network", null, null), Arrays.asList(organizingOrganisationId, addedOrganisationId));

        CooperationNetworkEntity originalCooperationNetwork = EntityInitializer.getCooperationNetwork(originalNetwork.getId(), originalNetwork.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        CourseUnitEntity courseUnitEntity = EntityInitializer.getCourseUnitEntity("CU-NOTIFICATIONS", "CU-NOTIFICATIONS",
            organizingOrganisationId, Collections.singletonList(originalCooperationNetwork), null);
        courseUnitEntity.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnitEntity);

        String updateJson = """
            {
              "courseUnit": {
                "studyElementId": "CU-NOTIFICATIONS",
                "cooperationNetworks": [
                  {
                    "id": "CN-COURSEUNIT-UPDATED"
                  }
                ]
              }
            }
            """;

        Message updateResponseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson,
            MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);

        DefaultResponse updateResp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(updateResponseMessage);
        assertEquals(Status.OK, updateResp.getStatus());

        CourseUnitEntity updatedCourseUnit = courseUnitRepository.findByStudyElementIdAndOrganizingOrganisationId(
            courseUnitEntity.getStudyElementId(), organizingOrganisationId).orElse(null);
        assertNotNull(updatedCourseUnit);
        assertEquals(1, updatedCourseUnit.getCooperationNetworks().size());
        assertEquals(updatedNetwork.getId(), updatedCourseUnit.getCooperationNetworks().getFirst().getId());

        Message removedOrganisationMessage = JmsHelper.receiveObject(jmsTemplate, removedOrganisationId);
        assertNotNull(removedOrganisationMessage);
        CompositeIdentifiedEntityModifiedNotification removedOrganisationNotification =
            (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(removedOrganisationMessage);
        assertEquals(1, removedOrganisationNotification.getDeleted().size());
        assertTrue(removedOrganisationNotification.getCreated().isEmpty());
        assertTrue(removedOrganisationNotification.getUpdated().isEmpty());
        assertEquals(courseUnitEntity.getStudyElementId(), removedOrganisationNotification.getDeleted().getFirst().getReferenceIdentifier());
        assertEquals(organizingOrganisationId, removedOrganisationNotification.getDeleted().getFirst().getReferenceOrganizer());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, removedOrganisationNotification.getDeleted().getFirst().getType());

        Message addedOrganisationMessage = JmsHelper.receiveObject(jmsTemplate, addedOrganisationId);
        assertNotNull(addedOrganisationMessage);
        CompositeIdentifiedEntityModifiedNotification addedOrganisationNotification =
            (CompositeIdentifiedEntityModifiedNotification) jmsTemplate.getMessageConverter().fromMessage(addedOrganisationMessage);
        assertEquals(1, addedOrganisationNotification.getCreated().size());
        assertTrue(addedOrganisationNotification.getUpdated().isEmpty());
        assertTrue(addedOrganisationNotification.getDeleted().isEmpty());
        assertEquals(courseUnitEntity.getStudyElementId(), addedOrganisationNotification.getCreated().getFirst().getReferenceIdentifier());
        assertEquals(organizingOrganisationId, addedOrganisationNotification.getCreated().getFirst().getReferenceOrganizer());
        assertEquals(CompositeIdentifiedEntityType.COURSE_UNIT, addedOrganisationNotification.getCreated().getFirst().getType());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_noCooperationNetworkNameOrStatusGiven_shouldFillCooperationNetworkFields() throws JMSException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity = persistNetworkEntity("CN-1", new LocalisedStringEntity("Verkosto 1", "Verkosto en", "Verkosto sv"), Arrays.asList(organizingOrganisationId));

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(networkEntity.getId(), new LocalisedStringEntity("JUUJUU", null, null),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "ID1",
                           "cooperationNetworks": [
                               {
                                   "id": "%s",
                                   "enrollable": true
                               }
                           ]
                    }
                }
            """.formatted(networkEntity.getId());

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ACTIVE, result.getStatus());
        assertTrue(result.getCooperationNetworks().stream()
            .allMatch(resultCn -> networkEntity.getId().equals(resultCn.getId())
                && networkEntity.getName().getValue("fi").equals(resultCn.getName().getValue("fi"))
                && networkEntity.getName().getValue("en").equals(resultCn.getName().getValue("en"))
                && networkEntity.getName().getValue("sv").equals(resultCn.getName().getValue("sv"))
            ));
    }

    @Test
    public void testSendingUpdateCourseUnitMessageWithEmptyCooperationNetworks_shouldSucceed() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
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
                        "cooperationNetworks": []
                    }
                }
            """;

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ACTIVE, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedContainsForwardSlash() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "I/D1", "RAI/RAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "I/D1",
                		"status": "ARCHIVED",
                		"name": {
                			"values": {
                				"fi": "jaksonnimi2 fi",
                				"en": "jaksonnimi2 en",
                				"sv": "jaksonnimi2 sv"
                			}
                		}
                	}
                }
            """;

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ARCHIVED, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_missingStudyElementId_shouldFail() throws JMSException {
        String updateCourseUnitJson =
            """
                {
                	"courseUnit": {
                		"name": {
                			"values": {
                				"fi": "jaksonnimi2 fi",
                				"en": "jaksonnimi2 en",
                				"sv": "jaksonnimi2 sv"
                			}
                		}
                	}
                }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
        assertEquals(1, resp.getErrors().size());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailBrokenJson() throws JMSException {
        String updateCourseUnitJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "IDASDASD",,,,,,,
                		"name": {
                			"values": {
                				"fi": "jaksonnimi2 fi",
                				"en": "jaksonnimi2 en",
                				"sv": "jaksonnimi2 sv"
                			}
                		}
                	}
                }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedNoMatchingNetworksForRealisation() throws JMSException {
        String organizingOrganisationId = "TUNI";

        NetworkEntity networkEntity1 = persistNetworkEntity("CN-1", null, Collections.singletonList(organizingOrganisationId));
        NetworkEntity networkEntity2 = persistNetworkEntity("CN-3", null, Collections.singletonList(organizingOrganisationId));

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(networkEntity1.getId(), networkEntity1.getName(),
            true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        StudyElementReferenceEntity reference = EntityInitializer.getStudyElementReferenceForCourseUnit(
            courseUnit.getStudyElementId(), organizingOrganisationId);
        RealisationEntity realisationEntity = EntityInitializer.getRealisationEntity("R1", "RCODE1", organizingOrganisationId,
            Collections.singletonList(reference), Collections.singletonList(network));
        realisationRepository.create(realisationEntity);

        String updateCourseUnitJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "%s",
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
                                        "fi": "Verkosto 3",
                                        "en": "Network 3",
                                        "sv": null
                                    }
                                },
                                "enrollable": true
                            }
                        ]
                    }
                }
            """.formatted(courseUnit.getStudyElementId(), networkEntity2.getId());

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateCourseUnitJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), "TUNI");
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.OK);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSuccessAndIgnoreType() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "ID1",
                        "type": "COURSE_UNIT",        "name": {
                            "values": {
                                "fi": "jaksonnimi2 fi",
                                "en": "jaksonnimi2 en",
                                "sv": "jaksonnimi2 sv"
                            }
                        }
                    }
                }
            """;

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSuccessAndSuccessfullySaveHistory() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "ID1",
                        "studyElementIdentifierCode": "RAIRAI",
                        "name": {
                            "values": {
                                "fi": "jaksonnimi2 fi",
                                "en": "jaksonnimi2 en",
                                "sv": "jaksonnimi2 sv"
                            }
                        }
                    }
                }
            """;

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
        assertNotNull(updatedCourseUnit.getCreatedTime());
        assertNotNull(updatedCourseUnit.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, updatedCourseUnit.getType());

        List<CourseUnitEntity> historyEntities = HistoryHelper.queryHistoryIndex(
            elasticsearchTemplate,"opintojaksot-history", CourseUnitEntity.class);
        assertNotNull(historyEntities);
        assertEquals(1, historyEntities.size());

        CourseUnitEntity historyEntity = historyEntities.get(0);
        assertEquals(courseUnit.getName().getValue("fi"), historyEntity.getName().getValue("fi"));
        assertEquals(courseUnit.getName().getValue("en"), historyEntity.getName().getValue("en"));
        assertEquals(courseUnit.getName().getValue("sv"), historyEntity.getName().getValue("sv"));

        String courseUnitUpdateJson2 =
            """
                {
                    "courseUnit": {
                        "studyElementId": "ID1",
                        "studyElementIdentifierCode": "RAIRAI",
                        "name": {
                            "values": {
                                "fi": "jaksonnimi3 fi",
                                "en": "jaksonnimi3 en",
                                "sv": "jaksonnimi3 sv"
                            }
                        }
                    }
                }
            """;

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
        assertNotNull(updatedCourseUnit2.getCreatedTime());
        assertNotNull(updatedCourseUnit2.getUpdateTime());
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, updatedCourseUnit2.getType());

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
    public void testSendingUpdateCourseUnitMessage_shouldNotFailBecauseOfExtraField() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "ID1",
                        "studyElementIdentifierCode": "RAIRAI",
                        "type": "COURSE_UNIT",
                        "thisdoesnotexist": "somerandomcontent"
                    }
                }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        assertNotNull(responseMessage);

        Object updResponse = jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(updResponse instanceof DefaultResponse);
        DefaultResponse response = (DefaultResponse) updResponse;
        assertTrue(response.getStatus() == Status.OK);
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldFailStatusNull() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "ID1",
                		"status": null
                	}
                }
            """;

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, courseUnitUpdateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        JsonValidationFailedResponse resp = (JsonValidationFailedResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);
        assertTrue(resp.getStatus() == Status.FAILED);

        List<CourseUnitEntity> savedCourseUnits = StreamSupport.stream(courseUnitRepository.findAll(Pageable.unpaged()).spliterator(), false).collect(Collectors.toList());
        assertTrue(savedCourseUnits != null);
        assertEquals(1, savedCourseUnits.size());

        CourseUnitEntity result = savedCourseUnits.get(0);
        assertNull(result.getUpdateTime());
        assertEquals(StudyStatusEntity.ACTIVE, result.getStatus());
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_shouldSucceedCooperationNetworksEmpty() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "ID1",
                		"cooperationNetworks": []
                	}
                }
            """;

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
    public void testSendingUpdateCourseUnitMessage_shouldSucceedCooperationNetworksNull() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                	"courseUnit": {
                		"studyElementId": "ID1",
                		"cooperationNetworks": null
                	}
                }
            """;

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
    public void testSendingUpdateCourseUnitMessage_containsRealisations_shouldNotUpdateRealisationInfoAndSucceed() throws JMSException {
        String organizingOrganisationId = "TUNI";

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");
        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntity(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName);
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);
        courseUnitRepository.create(courseUnit);

        String courseUnitUpdateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "ID1",
                        "studyElementIdentifierCode": "RAIRAI",
                        "name": {
                            "values": {
                                "fi": "jaksonnimi 2 fi",
                                "en": "jaksonnimi 2 en",
                                "sv": "jaksonnimi 2 sv"
                            }
                        },
                        "completionOptions": [
                            {
                                "assessmentItems": [
                                    {
                                        "primaryCourseUnitId": null,
                                        "name": {
                                            "values": {
                                                "fi": "arvioinninkohde 1"
                                            }
                                        },
                                        "realisations": [
                                            {
                                                "realisationId": "ID1",
                                                "realisationIdentifierCode": "IDENTIFIERCODE",
                                                "selections": null,
                                                "questionSets": null,
                                                "enrollmentStartDateTime": "2019-12-24T15:55:52.134+02:00",
                                                "enrollmentEndDateTime": "2020-01-23T15:55:52.134+02:00",
                                                "startDate": "2019-12-08",
                                                "endDate": "2020-02-08",
                                                "personReferences": null,
                                                "minSeats": null,
                                                "maxSeats": null,
                                                "location": null,
                                                "organisationSpecificDescriptions": null,
                                                "studyElementReferences": [
                                                    {
                                                        "referenceIdentifier": "ID1",
                                                        "referenceOrganizer": "TUNI",
                                                        "referenceType": "ASSESSMENT_ITEM",
                                                        "referenceAssessmentItemId": "AI-1"
                                                    }
                                                ],
                                                "name": {
                                                    "values": {
                                                        "fi": "nimi"
                                                    }
                                                },
                                                "organisationReferences": [
                                                    {
                                                        "percent": 100,
                                                        "organisationRole": 1,
                                                        "target": {
                                                            "organisationIdentifier": "TUNI",
                                                            "organisationTkCode": "TUNI",
                                                            "organisationName": null,
                                                            "unitName": null,
                                                            "nameDescription": null,
                                                            "postalAddress": null,
                                                            "streetAddress": null,
                                                            "municipalityCode": null,
                                                            "phone": null,
                                                            "url": null
                                                        },
                                                        "description": null
                                                    }
                                                ],
                                                "groupSelections": null,
                                                "cooperationNetworks": [
                                                    {
                                                        "validityStartDate": "2019-01-08",
                                                        "validityEndDate": "2021-01-08",
                                                        "enrollable": true,
                                                        "id": "CN-1",
                                                        "name": {
                                                            "values": {
                                                                "fi": "Verkosto 1",
                                                                "sv": "Verkosto sv",
                                                                "en": "Verkosto en"
                                                            }
                                                        }
                                                    }
                                                ],
                                                "createdTime": null,
                                                "updateTime": null,
                                                "groupQuotas": null,
                                                "status": null,
                                                "creditsMin": null,
                                                "creditsMax": null,
                                                "teachingLanguage": null,
                                                "minEduGuidanceArea": null
                                            },
                                            {
                                                "realisationId": "ID2",
                                                "realisationIdentifierCode": "IDENTIFIERCODE2",
                                                "selections": null,
                                                "questionSets": null,
                                                "enrollmentStartDateTime": "2019-12-19T15:55:52.134+02:00",
                                                "enrollmentEndDateTime": "2020-01-28T15:55:52.134+02:00",
                                                "startDate": "2019-11-08",
                                                "endDate": "2020-03-08",
                                                "personReferences": null,
                                                "minSeats": null,
                                                "maxSeats": null,
                                                "location": null,
                                                "organisationSpecificDescriptions": null,
                                                "studyElementReferences": [
                                                    {
                                                        "referenceIdentifier": "ID1",
                                                        "referenceOrganizer": "TUNI",
                                                        "referenceType": "ASSESSMENT_ITEM",
                                                        "referenceAssessmentItemId": "AI-2"
                                                    }
                                                ],
                                                "name": {
                                                    "values": {
                                                        "fi": "nimi2"
                                                    }
                                                },
                                                "organisationReferences": [
                                                    {
                                                        "percent": 100,
                                                        "organisationRole": 1,
                                                        "target": {
                                                            "organisationIdentifier": "TUNI",
                                                            "organisationTkCode": "TUNI",
                                                            "organisationName": null,
                                                            "unitName": null,
                                                            "nameDescription": null,
                                                            "postalAddress": null,
                                                            "streetAddress": null,
                                                            "municipalityCode": null,
                                                            "phone": null,
                                                            "url": null
                                                        },
                                                        "description": null
                                                    }
                                                ],
                                                "groupSelections": null,
                                                "cooperationNetworks": [
                                                    {
                                                        "validityStartDate": "2019-01-08",
                                                        "validityEndDate": "2021-01-08",
                                                        "enrollable": true,
                                                        "id": "CN-1",
                                                        "name": {
                                                            "values": {
                                                                "fi": "Verkosto 1",
                                                                "sv": "Verkosto sv",
                                                                "en": "Verkosto en"
                                                            }
                                                        }
                                                    }
                                                ],
                                                "createdTime": null,
                                                "updateTime": null,
                                                "groupQuotas": null,
                                                "status": null,
                                                "creditsMin": null,
                                                "creditsMax": null,
                                                "teachingLanguage": null,
                                                "minEduGuidanceArea": null
                                            }
                                        ],
                                        "type": null,
                                        "assessmentItemId": "AI-1",
                                        "creditsMin": null,
                                        "creditsMax": null
                                    }
                                ],
                                "name": {
                                    "values": {
                                        "fi": "suoritustapa 1"
                                    }
                                },
                                "completionOptionId": "CO-1",
                                "description": "kuvaus"
                            },
                            {
                                "assessmentItems": [
                                    {
                                        "primaryCourseUnitId": null,
                                        "name": null,
                                        "realisations": null,
                                        "type": null,
                                        "assessmentItemId": "AI-2",
                                        "creditsMin": null,
                                        "creditsMax": null
                                    }
                                ],
                                "name": null,
                                "completionOptionId": "CO-2",
                                "description": "kuvaus"
                            }
                        ],
                        "realisations": [
                            {
                                "realisationId": "ID3",
                                "realisationIdentifierCode": "IDENTIFIERCODE3",
                                "selections": null,
                                "questionSets": null,
                                "enrollmentStartDateTime": "2019-12-19T15:55:52.134+02:00",
                                "enrollmentEndDateTime": "2020-01-28T15:55:52.134+02:00",
                                "startDate": "2019-11-08",
                                "endDate": "2020-03-08",
                                "personReferences": null,
                                "minSeats": null,
                                "maxSeats": null,
                                "location": null,
                                "organisationSpecificDescriptions": null,
                                "studyElementReferences": [
                                    {
                                        "referenceIdentifier": "ID1",
                                        "referenceOrganizer": "TUNI",
                                        "referenceType": "ASSESSMENT_ITEM",
                                        "referenceAssessmentItemId": "AI-2"
                                    }
                                ],
                                "name": {
                                    "values": {
                                        "fi": "nimi3"
                                    }
                                },
                                "organisationReferences": [
                                    {
                                        "percent": 100,
                                        "organisationRole": 1,
                                        "target": {
                                            "organisationIdentifier": "TUNI",
                                            "organisationTkCode": "TUNI",
                                            "organisationName": null,
                                            "unitName": null,
                                            "nameDescription": null,
                                            "postalAddress": null,
                                            "streetAddress": null,
                                            "municipalityCode": null,
                                            "phone": null,
                                            "url": null
                                        },
                                        "description": null
                                    }
                                ],
                                "groupSelections": null,
                                "cooperationNetworks": [
                                    {
                                        "validityStartDate": "2019-01-08",
                                        "validityEndDate": "2021-01-08",
                                        "enrollable": true,
                                        "id": "CN-1",
                                        "name": {
                                            "values": {
                                                "fi": "Verkosto 1",
                                                "sv": "Verkosto sv",
                                                "en": "Verkosto en"
                                            }
                                        }
                                    }
                                ],
                                "createdTime": null,
                                "updateTime": null,
                                "groupQuotas": null,
                                "status": null,
                                "creditsMin": null,
                                "creditsMax": null,
                                "teachingLanguage": null,
                                "minEduGuidanceArea": null
                            }
                        ],
                        "sendingTime": "2020-01-08T15:55:52.134+02:00",
                        "creditsMin": 2.5,
                        "creditsMax": 5,
                        "organisationReferences": [
                            {
                                "percent": 100,
                                "organisationRole": 1,
                                "target": {
                                    "organisationIdentifier": "TUNI",
                                    "organisationTkCode": "TUNI",
                                    "organisationName": null,
                                    "unitName": null,
                                    "nameDescription": null,
                                    "postalAddress": null,
                                    "streetAddress": null,
                                    "municipalityCode": null,
                                    "phone": null,
                                    "url": null
                                },
                                "description": null
                            }
                        ]
                    }
                }
            """;

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
        assertEquals(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT, result.getType());
        assertEquals(StudyStatusEntity.ACTIVE, result.getStatus());
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
    public void testSendingUpdateCourseUnitMessage_removesAssessmentItemWithExistingRealisation_shouldFail() throws JMSException {
        String organizingOrganisationId = "TUNI";

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);
        OrganisationReference organisationReference = DtoInitializer.getOrganisationReference(organisation, OrganisationRole.ROLE_MAIN_ORGANIZER);

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Collections.singletonList(removedAssessmentItemEntity));

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName, Arrays.asList(completionOptionEntity, completionOptionEntity2));
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);

        courseUnitRepository.create(courseUnit);

        StudyElementReferenceEntity studyElementReference = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity.getAssessmentItemId());

        StudyElementReferenceEntity removedReference = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), removedAssessmentItemEntity.getAssessmentItemId());

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("ID1", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference), Collections.singletonList(network));

        realisationRepository.create(realisation);

        RealisationEntity realisationWithRefToRemovedAssessmentItem = EntityInitializer.getRealisationEntity("ID2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(removedReference), Collections.singletonList(network));

        realisationRepository.create(realisationWithRefToRemovedAssessmentItem);

        String updateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "%s",
                        "completionOptions": [
                            {
                                "completionOptionId": "%s",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti",
                                "assessmentItems": [
                                    {
                                        "assessmentItemId": "%s",
                                        "primaryCourseUnitId": null,
                                        "name": {
                                            "values": {
                                                "fi": "arvioinninkohde 1"
                                            }
                                         }
                                    }
                                ]
                            },
                            {
                                "completionOptionId": "%s",
                                "description": "completion option in which assessment item is removed that has realisation reference ",
                                "assessmentItems": [
                                    {
                                        "assessmentItemId": "NEWASSESSMENTITEM",
                                        "primaryCourseUnitId": null,
                                        "name": {
                                            "values": {
                                                "fi": "uusi arvioinninkohde"
                                            }
                                        }
                                    }
                                ]            }
                        ]
                    }
                }
            """.formatted(courseUnit.getStudyElementId(), completionOptionEntity.getCompletionOptionId(), assessmentItemEntity.getAssessmentItemId(), completionOptionEntity2.getCompletionOptionId());

        Message responseMessage = JmsHelper.sendAndReceiveJson(jmsTemplate, updateJson, MessageType.UPDATE_COURSEUNIT_REQUEST.name(), organizingOrganisationId);
        DefaultResponse resp = (DefaultResponse) jmsTemplate.getMessageConverter().fromMessage(responseMessage);

        assertTrue(resp.getStatus() == Status.FAILED);
        assertTrue(resp.getMessage().contains(removedAssessmentItemEntity.getAssessmentItemId()));
        assertTrue(resp.getMessage().contains(realisationWithRefToRemovedAssessmentItem.getRealisationId()));
    }

    @Test
    public void testSendingUpdateCourseUnitMessage_removesAssessmentItemWithExistingRealisation_shouldReAddDenormalizedData() throws JMSException {
        String organizingOrganisationId = "TUNI";

        Organisation organisation = DtoInitializer.getOrganisation(organizingOrganisationId, organizingOrganisationId);

        AssessmentItemEntity assessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-1", "ID1");
        AssessmentItemEntity assessmentItemEntity2 = EntityInitializer.getAssessmentItemEntity("AI-2", "ID2");
        AssessmentItemEntity removedAssessmentItemEntity = EntityInitializer.getAssessmentItemEntity("AI-3", "ID3");
        CompletionOptionEntity completionOptionEntity = EntityInitializer.getCompletionOptionEntity("CO-1", Collections.singletonList(assessmentItemEntity));
        CompletionOptionEntity completionOptionEntity2 = EntityInitializer.getCompletionOptionEntity("CO-2", Arrays.asList(assessmentItemEntity2, removedAssessmentItemEntity));

        CooperationNetworkEntity network = EntityInitializer.getCooperationNetwork(
            "CN-1", new LocalisedStringEntity("Verkosto", null, null), true, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        LocalisedStringEntity originalName = new LocalisedStringEntity("jaksonnimi fi", "jaksonnimi en", "jaksonnimi sv");

        CourseUnitEntity courseUnit = EntityInitializer.getCourseUnitEntityWithCompletionOptions(
            "ID1", "RAIRAI", organizingOrganisationId, Collections.singletonList(network), originalName, Arrays.asList(completionOptionEntity, completionOptionEntity2));
        courseUnit.setStatus(StudyStatusEntity.ACTIVE);

        courseUnitRepository.create(courseUnit);

        StudyElementReferenceEntity studyElementReference = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity.getAssessmentItemId());

        StudyElementReferenceEntity studyElementReference2 = EntityInitializer.getStudyElementReferenceForAssessmentItem(
            courseUnit.getStudyElementId(), organisation.getOrganisationTkCode(), assessmentItemEntity2.getAssessmentItemId());

        RealisationEntity realisation = EntityInitializer.getRealisationEntity("ID1", courseUnit.getOrganizingOrganisationId(),
            Arrays.asList(studyElementReference), Collections.singletonList(network));
        realisation.setName(new LocalisedStringEntity("Toteutus 1", "Realisation1", "Real1"));
        realisation.setStartDate(LocalDate.now().minusMonths(1));
        realisation.setEndDate(LocalDate.now().plusMonths(1));
        realisation.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(15));
        realisation.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(15));

        realisationRepository.create(realisation);

        RealisationEntity realisation2 = EntityInitializer.getRealisationEntity("ID2", courseUnit.getOrganizingOrganisationId(),
            Collections.singletonList(studyElementReference2), Collections.singletonList(network));
        realisation2.setName(new LocalisedStringEntity("Toteutus 2", "Realisation2", "Real2"));
        realisation2.setStartDate(LocalDate.now().minusMonths(2));
        realisation2.setEndDate(LocalDate.now().plusMonths(2));
        realisation2.setEnrollmentStartDateTime(OffsetDateTime.now().minusDays(20));
        realisation2.setEnrollmentEndDateTime(OffsetDateTime.now().plusDays(20));

        realisationRepository.create(realisation2);

        String updateJson =
            """
                {
                    "courseUnit": {
                        "studyElementId": "%s",
                        "completionOptions": [
                            {
                                "completionOptionId": "%s",
                                "description": "aktiivinen verkko-opintojaksolle osallistuminen, verkkotehtÃ¤vien tekeminen, itsenÃ¤inen opiskelu sekÃ¤ moodle-tentti",
                                "assessmentItems": [
                                    {
                                        "assessmentItemId": "%s",
                                        "primaryCourseUnitId": null,
                                        "name": {
                                            "values": {
                                                "fi": "arvioinninkohde 1"
                                            }
                                         }
                                    }
                                ]
                            },
                            {
                                "completionOptionId": "%s",
                                "description": "completion option in which one assessment item is removed ",
                                "assessmentItems": [
                                    {
                                        "assessmentItemId": "%s",
                                        "primaryCourseUnitId": null,
                                        "name": {
                                            "values": {
                                                "fi": "uusi arvioinninkohde"
                                            }
                                        }
                                    }
                                ]            }
                        ]
                    }
                }
            """.formatted(courseUnit.getStudyElementId(), completionOptionEntity.getCompletionOptionId(), assessmentItemEntity.getAssessmentItemId(), completionOptionEntity2.getCompletionOptionId(), assessmentItemEntity2.getAssessmentItemId());

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
