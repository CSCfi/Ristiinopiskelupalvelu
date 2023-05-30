package fi.uta.ristiinopiskelu.messaging.message.current.student;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Address;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Country;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Phone;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyRightIdentifier;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.student.StudentWarning;

import java.util.List;

public class ForwardedUpdateStudentRequest extends AbstractForwardedStudentMessage {
    private String studentRequestId;
    private String oid;
    private String homeEppn;
    private String hostEppn;
    private String firstNames;
    private String surName;
    private String givenName;

    private List<Address> address;
    private List<Country> countryOfCitizenship;
    private String municipalityOfResidence;
    private String motherTongue;
    private String preferredLanguage;
    private List<String> email;
    private List<Phone> phone;
    private List<StudentWarning> warnings;
    private List<StudyRightIdentifier> hostStudyRightIdentifiers;

    @Override
    public String getStudentRequestId() {
        return studentRequestId;
    }

    @Override
    public void setStudentRequestId(String studentRequestId) {
        this.studentRequestId = studentRequestId;
    }

    @Override
    public List<StudyRightIdentifier> getHostStudyRightIdentifiers() {
        return hostStudyRightIdentifiers;
    }

    @Override
    public void setHostStudyRightIdentifiers(List<StudyRightIdentifier> hostStudyRightIdentifiers) {
        this.hostStudyRightIdentifiers = hostStudyRightIdentifiers;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
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

    public List<Address> getAddress() {
        return address;
    }

    public void setAddress(List<Address> address) {
        this.address = address;
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

    public List<StudentWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<StudentWarning> warnings) {
        this.warnings = warnings;
    }
}
