package fi.uta.ristiinopiskelu.handler.helper;

import com.github.mpolla.HetuUtil;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.ExtendedStudent;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.RoutingType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.messaging.message.current.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.current.studyrecord.CreateStudyRecordRequest;
import fi.uta.ristiinopiskelu.messaging.util.Oid;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MessageTemplateInitializer {
    public static CreateRegistrationRequest getCreateRegistrationRequestTemplate(String sendingOrganisationId, String receivingOrganisationId, String networkId) {
        StudyRightIdentifier studyRightIdentifier = new StudyRightIdentifier();
        studyRightIdentifier.setStudyRightId("OPISKOIK1");
        studyRightIdentifier.setOrganisationTkCodeReference(sendingOrganisationId);

        StudyRightStatus studyRightStatus = new StudyRightStatus();
        studyRightStatus.setStudyRightStatusValue(StudyRightStatusValue.ACTIVE);
        studyRightStatus.setStartDate(LocalDate.of(2017, 1, 1));
        studyRightStatus.setEndDate(LocalDate.of(2020, 6, 1));

        StudyRight studyRight = new StudyRight();
        studyRight.setIdentifiers(studyRightIdentifier);
        studyRight.setStudyRightStatus(studyRightStatus);

        Keyword keyword = new Keyword();
        keyword.setKey("testkey");
        keyword.setKeySet("testKeySet");
        keyword.setValue(new LocalisedString("value fi", "value en", "value sv"));
        studyRight.setKeywords(Collections.singletonList(keyword));

        ExtendedStudent student = new ExtendedStudent();
        student.setPersonId(HetuUtil.generateRandom());
        student.setOid(Oid.randomOid(Oid.PERSON_NODE_ID));
        student.setFirstNames("Matti");
        student.setSurName("Nykänen");
        student.setGivenName("Matti");
        student.setDateOfBirth(LocalDate.of(1950, 1, 1));
        student.setHomeEppn("masa@tuni.fi");
        student.setGender(1);
        student.setMotherTongue("fi");
        student.setHomeStudyRight(studyRight);
        student.setHomeStudentNumber("1234-1234");

        Address address = new Address();
        address.setStreet("Kotikatu 1");
        address.setCountry(Country.FI);
        address.setPostalCode("33100");
        address.setPostOffice("Peräkylä");
        student.setAddresses(Arrays.asList(address));

        CreateRegistrationRequest req = new CreateRegistrationRequest();
        req.setStudent(student);
        req.setSendingOrganisationTkCode(sendingOrganisationId);
        req.setReceivingOrganisationTkCode(receivingOrganisationId);
        req.setNetworkIdentifier(networkId);
        req.setEnrolmentDateTime(OffsetDateTime.now());

        return req;
    }

    public static CreateStudyRecordRequest getCreateStudyRecordRequestTemplate(String sendingOrganisationId, String receivingOrganisationId,
                                                                               String networkIdentifier, List<CompletedCredit> completedCredits,
                                                                               StudyRecordStudent student, RoutingType routingType) {

        CreateStudyRecordRequest createStudyRecordRequest = new CreateStudyRecordRequest();
        createStudyRecordRequest.setSendingOrganisation(sendingOrganisationId);
        createStudyRecordRequest.setReceivingOrganisation(receivingOrganisationId);
        createStudyRecordRequest.setNetworkIdentifier(networkIdentifier);
        createStudyRecordRequest.setCompletedCredits(completedCredits);
        createStudyRecordRequest.setSendingTime(OffsetDateTime.now());
        createStudyRecordRequest.setStudent(student);
        createStudyRecordRequest.setRoutingType(routingType);
        return createStudyRecordRequest;
    }
}
