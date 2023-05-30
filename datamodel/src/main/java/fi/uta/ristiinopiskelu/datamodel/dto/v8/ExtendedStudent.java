package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ExtendedStudent extends Student {

    private StudyRight hostStudyRight;
    private StudyRight homeStudyRight;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dateOfBirth;

    private int gender; // 1 = male, 2 = female
    private List<Address> addresses;
    private Country countryOfCitizenship;
    private String municipalityOfResidence;
    private String motherTongue;
    private String preferredLanguage;
    private List<String> email;
    private List<Phone> phone;
    private Boolean safetyProhibition;

    public StudyRight getHostStudyRight() {
        return hostStudyRight;
    }

    public void setHostStudyRight(StudyRight hostStudyRight) {
        this.hostStudyRight = hostStudyRight;
    }

    public StudyRight getHomeStudyRight() {
        return homeStudyRight;
    }

    public void setHomeStudyRight(StudyRight homeStudyRight) {
        this.homeStudyRight = homeStudyRight;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public Country getCountryOfCitizenship() {
        return countryOfCitizenship;
    }

    public void setCountryOfCitizenship(Country countryOfCitizenship) {
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

    public Boolean getSafetyProhibition() {
        return safetyProhibition;
    }

    public void setSafetyProhibition(Boolean safetyProhibition) {
        this.safetyProhibition = safetyProhibition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtendedStudent)) return false;
        ExtendedStudent that = (ExtendedStudent) o;
        return gender == that.gender &&
                Objects.equals(hostStudyRight, that.hostStudyRight) &&
                Objects.equals(homeStudyRight, that.homeStudyRight) &&
                Objects.equals(dateOfBirth, that.dateOfBirth) &&
                Objects.equals(addresses, that.addresses) &&
                Objects.equals(countryOfCitizenship, that.countryOfCitizenship) &&
                Objects.equals(municipalityOfResidence, that.municipalityOfResidence) &&
                Objects.equals(motherTongue, that.motherTongue) &&
                Objects.equals(preferredLanguage, that.preferredLanguage) &&
                Objects.equals(email, that.email) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(safetyProhibition, that.safetyProhibition);
    }

    @Override
    public int hashCode() {

        return Objects.hash(hostStudyRight, homeStudyRight, dateOfBirth, gender, addresses, countryOfCitizenship, municipalityOfResidence, motherTongue, preferredLanguage, email, phone, safetyProhibition);
    }
}
