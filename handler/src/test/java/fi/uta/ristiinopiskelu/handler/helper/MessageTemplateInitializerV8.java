package fi.uta.ristiinopiskelu.handler.helper;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.CompletedCredit;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.RoutingType;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.StudyRecordStudent;
import fi.uta.ristiinopiskelu.messaging.message.v8.registration.CreateRegistrationRequest;
import fi.uta.ristiinopiskelu.messaging.message.v8.studyrecord.CreateStudyRecordRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MessageTemplateInitializerV8 {
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
        student.setPersonId("010101-0101");
        student.setOid(UUID.randomUUID().toString());
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
                                                                               List<CompletedCredit> completedCredits, Student student,
                                                                               StudyRightIdentifier homeStudyRightIdentifier, StudyRightIdentifier hostStudyRightIdentifier,
                                                                               RoutingType routingType) {

        // Make copy of student to ignore ExtendedStudent fields in case that is set to param
        StudyRecordStudent studentCopy = new StudyRecordStudent();
        studentCopy.setPersonId(student.getPersonId());
        studentCopy.setOid(student.getOid());
        studentCopy.setFirstNames(student.getFirstNames());
        studentCopy.setHostStudentNumber("1235-1235");
        studentCopy.setSurName(student.getSurName());
        studentCopy.setHomeEppn(student.getHomeEppn());
        studentCopy.setHostEppn(student.getHostEppn());
        studentCopy.setGivenName(student.getGivenName());
        studentCopy.setHomeStudentNumber(student.getHomeStudentNumber());
        studentCopy.setHomeStudyRightIdentifier(homeStudyRightIdentifier);
        studentCopy.setHostStudyRightIdentifier(hostStudyRightIdentifier);

        CreateStudyRecordRequest createStudyRecordRequest = new CreateStudyRecordRequest();
        createStudyRecordRequest.setSendingOrganisation(sendingOrganisationId);
        createStudyRecordRequest.setReceivingOrganisation(receivingOrganisationId);
        createStudyRecordRequest.setCompletedCredits(completedCredits);
        createStudyRecordRequest.setSendingTime(OffsetDateTime.now());
        createStudyRecordRequest.setStudent(studentCopy);
        createStudyRecordRequest.setRoutingType(routingType);
        return createStudyRecordRequest;
    }
}
