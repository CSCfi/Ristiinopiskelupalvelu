package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentMessageType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentStudyRight;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.UpdateStatus;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = "opiskelijat", createIndex = false)
public class StudentEntity extends GenericEntity {
    
    private String oid;
    private String personId;

    private String homeEppn;
    private String hostEppn;
    private String firstNames;
    private String surName;
    private String givenName;

    private StudentStudyRight homeStudyRight;
    private List<Address> addresses;
    private List<Country> countryOfCitizenship;
    private String municipalityOfResidence;
    private String motherTongue;
    private String preferredLanguage;
    private List<String> email;
    private List<Phone> phone;
    private List<UpdateStatus> statuses;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime timestamp;
    
    private List<StudentWarning> warnings;
    private String homeOrganisationTkCode;
    private StudentMessageType messageType;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getHomeEppn() {
        return homeEppn;
    }

    public void setHomeEppn(String homeEppn) {
        this.homeEppn = homeEppn;
    }

    public String getHostEppn() {
        return hostEppn;
    }

    public void setHostEppn(String hostEppn) {
        this.hostEppn = hostEppn;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public StudentStudyRight getHomeStudyRight() {
        return homeStudyRight;
    }

    public void setHomeStudyRight(StudentStudyRight homeStudyRight) {
        this.homeStudyRight = homeStudyRight;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<Country> getCountryOfCitizenship() {
        return countryOfCitizenship;
    }

    public void setCountryOfCitizenship(List<Country> countryOfCitizenship) {
        this.countryOfCitizenship = countryOfCitizenship;
    }

    public String getMunicipalityOfResidence() {
        return municipalityOfResidence;
    }

    public void setMunicipalityOfResidence(String municipalityOfResidence) {
        this.municipalityOfResidence = municipalityOfResidence;
    }

    public String getMotherTongue() {
        return motherTongue;
    }

    public void setMotherTongue(String motherTongue) {
        this.motherTongue = motherTongue;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public List<Phone> getPhone() {
        return phone;
    }

    public void setPhone(List<Phone> phone) {
        this.phone = phone;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<UpdateStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<UpdateStatus> statuses) {
        this.statuses = statuses;
    }

    public List<StudentWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<StudentWarning> warnings) {
        this.warnings = warnings;
    }

    public String getHomeOrganisationTkCode() {
        return homeOrganisationTkCode;
    }

    public void setHomeOrganisationTkCode(String homeOrganisationTkCode) {
        this.homeOrganisationTkCode = homeOrganisationTkCode;
    }

    public StudentMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(StudentMessageType messageType) {
        this.messageType = messageType;
    }
}
