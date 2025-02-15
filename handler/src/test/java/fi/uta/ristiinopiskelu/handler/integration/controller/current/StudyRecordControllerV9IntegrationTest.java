package fi.uta.ristiinopiskelu.handler.integration.controller.current;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Language;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.network.NetworkOrganisation;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyrecord.StudyRecordReadDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.search.studyrecord.*;
import fi.uta.ristiinopiskelu.datamodel.entity.NetworkEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyRecordEntity;
import fi.uta.ristiinopiskelu.handler.EmbeddedActiveMQInitializer;
import fi.uta.ristiinopiskelu.handler.EmbeddedElasticsearchInitializer;
import fi.uta.ristiinopiskelu.handler.TestEsConfig;
import fi.uta.ristiinopiskelu.handler.helper.DtoInitializer;
import fi.uta.ristiinopiskelu.handler.helper.EntityInitializer;
import fi.uta.ristiinopiskelu.messaging.util.Oid;
import fi.uta.ristiinopiskelu.persistence.repository.NetworkRepository;
import fi.uta.ristiinopiskelu.persistence.repository.StudyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({
        EmbeddedActiveMQInitializer.class,
        EmbeddedElasticsearchInitializer.class
})
@SpringBootTest(classes = TestEsConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class StudyRecordControllerV9IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Test
    public void testFindStudyRecords_bySendingOrganisationAndSendingOrganisationInOwnNetwork_shouldSucceed() throws Exception {
        List<NetworkOrganisation> organisations = Arrays.asList(
            new NetworkOrganisation("UEF", true,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))),
            new NetworkOrganisation("TUNI", false,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))));
                
        NetworkEntity network = EntityInitializer.getNetworkEntity("CN-1", new LocalisedString("test", null, null),
            organisations, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(2)), true);

        networkRepository.create(network);

        StudyRecordStudent student = new StudyRecordStudent();
        student.setFirstNames("Matti");
        student.setSurName("Testaaja");
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

        StudyRecordEntity studyRecord = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity("UEF", "TUNI", student));
        StudyRecordEntity studyRecord2 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity("UEF", "TUNI", student));
        StudyRecordEntity studyRecord3 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity("TUNI", "UEF", student));

        assertEquals(3, StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList()).size());
        
        StudyRecordSearchParameters searchParameters = new StudyRecordSearchParameters();
        searchParameters.setSendingOrganisation("UEF");

        MvcResult result = getMvcResult(searchParameters);

        List<StudyRecordReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(2, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord.getId())),
            hasProperty("id", is(studyRecord2.getId()))
        ));

        searchParameters.setSendingOrganisation(null);
        searchParameters.setReceivingOrganisation("UEF");

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord3.getId()))
        ));
    }

    @Test
    public void testFindStudyRecords_bySendingOrganisationAndSendingOrganisationNotRequestingOrganisation_shouldSucceed() throws Exception {
        List<NetworkOrganisation> organisations = Arrays.asList(
            new NetworkOrganisation("UEF", true,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))),
            new NetworkOrganisation("TUNI", false,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))));

        NetworkEntity network = EntityInitializer.getNetworkEntity("CN-1", new LocalisedString("test", null, null),
            organisations, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(2)), true);

        networkRepository.create(network);

        StudyRecordStudent student = new StudyRecordStudent();
        student.setFirstNames("Matti");
        student.setSurName("Testaaja");
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        
        studyRecordRepository.create(EntityInitializer.getStudyRecordEntity("JYU", "TUNI", student));
        studyRecordRepository.create(EntityInitializer.getStudyRecordEntity("LAY", "UEF", student));

        assertEquals(2, StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList()).size());

        StudyRecordSearchParameters searchParameters = new StudyRecordSearchParameters();
        searchParameters.setSendingOrganisation("JYU");

        this.mockMvc.perform(post("/api/v9/studyrecords/search")
                .header("eppn", "testailija@testailija.fi")
                .header("SSL_CLIENT_S_DN_O", "UEF")
                .content(objectMapper.writeValueAsString(searchParameters))
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andReturn();

        searchParameters.setSendingOrganisation("LAY");

        this.mockMvc.perform(post("/api/v9/studyrecords/search")
                .header("eppn", "testailija@testailija.fi")
                .header("SSL_CLIENT_S_DN_O", "UEF")
                .content(objectMapper.writeValueAsString(searchParameters))
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testFindStudyRecords_byCompletedCreditParameters_shouldSucceed() throws Exception {
        List<NetworkOrganisation> organisations = Arrays.asList(
            new NetworkOrganisation("UEF", true,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))),
            new NetworkOrganisation("TUNI", false,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))));

        NetworkEntity network = EntityInitializer.getNetworkEntity("CN-1", new LocalisedString("test", null, null),
            organisations, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(2)), true);

        networkRepository.create(network);

        StudyRecordStudent student = new StudyRecordStudent();
        student.setFirstNames("Matti");
        student.setSurName("Testaaja");
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

        CompletedCredit completedCredit = DtoInitializer.getCompletedCredit("CC1", "CU1",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.EDUCATION, new LocalisedString("matin opintojakso 1", null, null));
        CompletedCredit completedCredit2 = DtoInitializer.getCompletedCredit("CC2", "CU2",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.AGRICULTURE_AND_FORESTY, new LocalisedString("tepon opintojakso 2", null, null));
        CompletedCredit completedCredit3 = DtoInitializer.getCompletedCredit("CC3", "SM1",
            CompletedCreditTargetType.STUDY_MODULE, MinEduGuidanceArea.ARTS_AND_CULTURE, new LocalisedString("heikin opintokokonaisuus 1", null, null));
        CompletedCredit completedCredit4 = DtoInitializer.getCompletedCredit("CC4", "CU3",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.HUMANITIES, new LocalisedString("jarmon opintojakso 3", null, null));
        
        StudyRecordEntity studyRecord = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit));
        StudyRecordEntity studyRecord2 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit2));
        StudyRecordEntity studyRecord3 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "TUNI", "UEF", student, completedCredit3));
        StudyRecordEntity studyRecord4 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "TUNI", "UEF", student, completedCredit4));

        assertEquals(4, StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList()).size());

        StudyRecordSearchParameters searchParameters = new StudyRecordSearchParameters();
        searchParameters.setSendingOrganisation("UEF");
        searchParameters.setCompletedCreditTargetId("CU1");

        MvcResult result = getMvcResult(searchParameters);

        List<StudyRecordReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord.getId()))
        ));

        searchParameters.setSendingOrganisation(null);
        searchParameters.setReceivingOrganisation("UEF");
        searchParameters.setCompletedCreditTargetId(null);
        searchParameters.setCompletedCreditTargetType(CompletedCreditTargetType.STUDY_MODULE);

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord3.getId()))
        ));

        searchParameters.setSendingOrganisation("UEF");
        searchParameters.setReceivingOrganisation(null);
        searchParameters.setCompletedCreditTargetId(null);
        searchParameters.setCompletedCreditTargetType(null);
        searchParameters.setCompletedCreditNameLanguage(Language.FI);
        searchParameters.setCompletedCreditName("tepon*");

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord2.getId()))
        ));
    }

    @Test
    public void testFindStudyRecords_byGradeStatus_shouldSucceed() throws Exception {
        List<NetworkOrganisation> organisations = Arrays.asList(
            new NetworkOrganisation("UEF", true,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))),
            new NetworkOrganisation("TUNI", false,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))));

        NetworkEntity network = EntityInitializer.getNetworkEntity("CN-1", new LocalisedString("test", null, null),
            organisations, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(2)), true);

        networkRepository.create(network);

        StudyRecordStudent student = new StudyRecordStudent();
        student.setFirstNames("Matti");
        student.setSurName("Testaaja");
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));

        CompletedCredit completedCredit = DtoInitializer.getCompletedCredit("CC1", "CU1",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.EDUCATION, new LocalisedString("matin opintojakso 1", null, null));

        CompletedCredit completedCredit2 = DtoInitializer.getCompletedCredit("CC2", "CU2",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.AGRICULTURE_AND_FORESTY, new LocalisedString("tepon opintojakso 2", null, null));
        completedCredit2.setAssessment(DtoInitializer.getCompletedCreditAssessment("hylätty", null, ScaleValue.FIVE_LEVEL, GradeCode.GRADE_HYL));

        CompletedCredit completedCredit3 = DtoInitializer.getCompletedCredit("CC3", "SM1",
            CompletedCreditTargetType.STUDY_MODULE, MinEduGuidanceArea.ARTS_AND_CULTURE, new LocalisedString("heikin opintokokonaisuus 1", null, null));
        completedCredit3.setAssessment(DtoInitializer.getCompletedCreditAssessment("hyväksytty", null, ScaleValue.FIVE_LEVEL, GradeCode.GRADE_3));

        StudyRecordEntity studyRecord = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit));
        StudyRecordEntity studyRecord2 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit2));
        StudyRecordEntity studyRecord3 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit3));

        assertEquals(3, StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList()).size());

        StudyRecordSearchParameters searchParameters = new StudyRecordSearchParameters();
        searchParameters.setSendingOrganisation("UEF");
        searchParameters.setGradeStatus(GradeStatus.APPROVED);

        MvcResult result = getMvcResult(searchParameters);

        List<StudyRecordReadDTO> actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord3.getId()))
        ));

        searchParameters.setGradeStatus(GradeStatus.REJECTED);

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord2.getId()))
        ));

        searchParameters.setGradeStatus(GradeStatus.UNGRADED);

        result = getMvcResult(searchParameters);

        actualResult = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});
        assertEquals(1, actualResult.size());
        assertThat(actualResult, containsInAnyOrder(
            hasProperty("id", is(studyRecord.getId()))
        ));
    }

    @Test
    public void testFindStudyRecordAmountCsv_groupBySendingOrganisationAndDivideByGrading_shouldSucceed() throws Exception {
        createCsvTestStudyRecords();
        
        StudyRecordAmountSearchParameters amountSearchParameters = new StudyRecordAmountSearchParameters();
        amountSearchParameters.setSendingOrganisation("UEF");
        amountSearchParameters.setGroupBy(StudyRecordGrouping.SENDING_ORGANISATION);
        amountSearchParameters.setDivideBy(StudyRecordDividing.GRADING);

        MvcResult result = getMvcResult("/api/v9/studyrecords/search/amounts/csv", objectMapper.writeValueAsString(amountSearchParameters));

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("\"UEF\",\"1\",\"1\",\"2\""));
    }

    @Test
    public void testFindStudyRecordAmountCsv_groupByReceivingOrganisationAndDivideByGrading_shouldSucceed() throws Exception {
        createCsvTestStudyRecords();
        
        StudyRecordAmountSearchParameters amountSearchParameters = new StudyRecordAmountSearchParameters();
        amountSearchParameters.setSendingOrganisation("UEF");
        amountSearchParameters.setGroupBy(StudyRecordGrouping.RECEIVING_ORGANISATION);
        amountSearchParameters.setDivideBy(StudyRecordDividing.GRADING);

        MvcResult result = getMvcResult("/api/v9/studyrecords/search/amounts/csv", objectMapper.writeValueAsString(amountSearchParameters));

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("\"TUNI\",\"1\",\"1\",\"2\""));
    }

    @Test
    public void testFindStudyRecordAmountCsv_groupByDatesAndDivideByGrading_shouldSucceed() throws Exception {
        createCsvTestStudyRecords();
        
        LocalDate groupingStartDate = LocalDate.of(2023, 5, 1);
        LocalDate groupingEndDate = LocalDate.of(2023, 9, 1);

        StudyRecordAmountSearchParameters amountSearchParameters = new StudyRecordAmountSearchParameters();
        amountSearchParameters.setSendingOrganisation("UEF");
        amountSearchParameters.setGroupBy(StudyRecordGrouping.DATES);
        amountSearchParameters.setGroupByDates(List.of(new StudyRecordGroupingDates(groupingStartDate, groupingEndDate)));
        amountSearchParameters.setDivideBy(StudyRecordDividing.GRADING);

        MvcResult result = getMvcResult("/api/v9/studyrecords/search/amounts/csv", objectMapper.writeValueAsString(amountSearchParameters));

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("\"2023-05-01:2023-09-01\",\"1\""));
    }

    @Test
    public void testFindStudyRecordAmountCsv_groupByStudyElementIdentifierCodeAndDivideByGrading_shouldSucceed() throws Exception {
        createCsvTestStudyRecords();
        
        StudyRecordAmountSearchParameters amountSearchParameters = new StudyRecordAmountSearchParameters();
        amountSearchParameters.setSendingOrganisation("UEF");
        amountSearchParameters.setGroupBy(StudyRecordGrouping.STUDYELEMENT_IDENTIFIER_CODE);
        amountSearchParameters.setDivideBy(StudyRecordDividing.MIN_EDU_GUIDANCE_AREA);

        MvcResult result = getMvcResult("/api/v9/studyrecords/search/amounts/csv", objectMapper.writeValueAsString(amountSearchParameters));

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("\"CC1-CU1\",\"1\""));
        assertTrue(csv.contains("\"CC2-CU2\",\"1\""));
        assertTrue(csv.contains("\"CC3-CU3\",\"1\""));
        assertTrue(csv.contains("\"CC4-SM1\",\"1\""));
    }

    @Test
    public void testFindStudyRecordAmountCsv_groupByStudyElementIdentifierCodeAndDivideByOrganisationResponsibleForCompletion_shouldSucceed() throws Exception {
        createCsvTestStudyRecords();

        StudyRecordAmountSearchParameters amountSearchParameters = new StudyRecordAmountSearchParameters();
        amountSearchParameters.setSendingOrganisation("UEF");
        amountSearchParameters.setGroupBy(StudyRecordGrouping.SENDING_ORGANISATION);
        amountSearchParameters.setDivideBy(StudyRecordDividing.ORGANISATION_RESPONSIBLE_FOR_COMPLETION);

        MvcResult result = getMvcResult("/api/v9/studyrecords/search/amounts/csv", objectMapper.writeValueAsString(amountSearchParameters));

        String csv = result.getResponse().getContentAsString();
        assertTrue(csv.contains("\"ORGANISAATIO\",\"ORG1\",\"ORG2\""));
        assertTrue(csv.contains("\"UEF\",\"2\",\"2\""));
    }

    private void createCsvTestStudyRecords() {
        List<NetworkOrganisation> organisations = Arrays.asList(
            new NetworkOrganisation("UEF", true,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))),
            new NetworkOrganisation("TUNI", false,
                DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(1))));

        NetworkEntity network = EntityInitializer.getNetworkEntity("CN-1", new LocalisedString("test", null, null),
            organisations, DtoInitializer.getIndefinitelyValidity(OffsetDateTime.now().minusDays(2)), true);

        networkRepository.create(network);

        StudyRecordStudent student = new StudyRecordStudent();
        student.setFirstNames("Matti");
        student.setSurName("Testaaja");
        student.setOid(UUID.randomUUID().toString());

        StudyRecordOrganisation studyRecordOrganisation = new StudyRecordOrganisation();
        studyRecordOrganisation.setOrganisationTkCode("ORG1");

        StudyRecordOrganisation studyRecordOrganisation2 = new StudyRecordOrganisation();
        studyRecordOrganisation2.setOrganisationTkCode("ORG2");

        CompletedCredit completedCredit = DtoInitializer.getCompletedCredit("CC1", "CU1",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.EDUCATION, new LocalisedString("matin opintojakso 1", null, null));
        completedCredit.getCompletedCreditTarget().setCompletedCreditTargetIdentifierCode("CC1-CU1");
        completedCredit.setOrganisationResponsibleForCompletion(studyRecordOrganisation);

        CompletedCredit completedCredit2 = DtoInitializer.getCompletedCredit("CC2", "CU2",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.EDUCATION, new LocalisedString("matin opintojakso 2", null, null));
        completedCredit2.setOrganisationResponsibleForCompletion(studyRecordOrganisation);
        completedCredit2.getCompletedCreditTarget().setCompletedCreditTargetIdentifierCode("CC2-CU2");

        CompletedCredit completedCredit3 = DtoInitializer.getCompletedCredit("CC3", "CU3",
            CompletedCreditTargetType.COURSE_UNIT, MinEduGuidanceArea.AGRICULTURE_AND_FORESTY, new LocalisedString("tepon opintojakso 1", null, null));
        completedCredit3.setOrganisationResponsibleForCompletion(studyRecordOrganisation2);
        completedCredit3.setAssessment(DtoInitializer.getCompletedCreditAssessment("hylätty", null, ScaleValue.FIVE_LEVEL, GradeCode.GRADE_HYL));
        completedCredit3.getCompletedCreditTarget().setCompletedCreditTargetIdentifierCode("CC3-CU3");

        CompletedCredit completedCredit4 = DtoInitializer.getCompletedCredit("CC4", "SM1",
            CompletedCreditTargetType.STUDY_MODULE, MinEduGuidanceArea.ARTS_AND_CULTURE, new LocalisedString("heikin opintokokonaisuus 1", null, null));
        completedCredit4.setOrganisationResponsibleForCompletion(studyRecordOrganisation2);
        completedCredit4.setAssessment(DtoInitializer.getCompletedCreditAssessment("hyväksytty", null, ScaleValue.FIVE_LEVEL, GradeCode.GRADE_3));
        completedCredit4.setCompletionDate(LocalDate.of(2023, 5, 15));
        completedCredit4.getCompletedCreditTarget().setCompletedCreditTargetIdentifierCode("CC4-SM1");

        StudyRecordEntity studyRecord = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit));
        StudyRecordEntity studyRecord2 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit2));
        StudyRecordEntity studyRecord3 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit3));
        StudyRecordEntity studyRecord4 = studyRecordRepository.create(EntityInitializer.getStudyRecordEntity(
            "UEF", "TUNI", student, completedCredit4));

        assertEquals(4, StreamSupport.stream(studyRecordRepository.findAll().spliterator(), false).collect(Collectors.toList()).size());
    }

    private MvcResult getMvcResult(StudyRecordSearchParameters searchParams) throws Exception {
        return this.getMvcResult(objectMapper.writeValueAsString(searchParams));
    }

    private MvcResult getMvcResult(String content) throws Exception {
        return getMvcResult("/api/v9/studyrecords/search", content);
    }

    private MvcResult getMvcResult(String url, MultiValueMap<String, String> params) throws Exception {
        return this.mockMvc.perform(
                get(url)
                    .header("eppn", "testailija@testailija.fi")
                    .header("SSL_CLIENT_S_DN_O", "UEF")
                    .params(params)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }

    private MvcResult getMvcResult(String url, String content) throws Exception {
        return this.mockMvc.perform(
                post(url)
                    .header("eppn", "testailija@testailija.fi")
                    .header("SSL_CLIENT_S_DN_O", "UEF")
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }
}
