package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CooperationNetwork;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Schema(name = "CourseUnitRealisation")
public class CourseUnitRealisationEntity {
    private String realisationId;
    private String realisationIdentifierCode;
    private String organizingOrganisationId;
    private LocalisedString name = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentStartDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentEndDateTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate = null;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate = null;

    private boolean enrollmentClosed = false;
    private List<CooperationNetwork> cooperationNetworks;
    private List<String> teachingLanguage;
    private StudyStatus status;
    private List<MinEduGuidanceArea> minEduGuidanceArea;

    public CourseUnitRealisationEntity() {
    }

    public String getRealisationId() {
        return realisationId;
    }

    public void setRealisationId(String realisationId) {
        this.realisationId = realisationId;
    }

    public String getRealisationIdentifierCode() {
        return realisationIdentifierCode;
    }

    public void setRealisationIdentifierCode(String realisationIdentifierCode) {
        this.realisationIdentifierCode = realisationIdentifierCode;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public OffsetDateTime getEnrollmentStartDateTime() {
        return enrollmentStartDateTime;
    }

    public void setEnrollmentStartDateTime(OffsetDateTime enrollmentStartDateTime) {
        this.enrollmentStartDateTime = enrollmentStartDateTime;
    }

    public OffsetDateTime getEnrollmentEndDateTime() {
        return enrollmentEndDateTime;
    }

    public void setEnrollmentEndDateTime(OffsetDateTime enrollmentEndDateTime) {
        this.enrollmentEndDateTime = enrollmentEndDateTime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public boolean isEnrollmentClosed() {
        return enrollmentClosed;
    }

    public void setEnrollmentClosed(boolean enrollmentClosed) {
        this.enrollmentClosed = enrollmentClosed;
    }

    public List<CooperationNetwork> getCooperationNetworks() {
        return cooperationNetworks;
    }

    public void setCooperationNetworks(List<CooperationNetwork> cooperationNetworks) {
        this.cooperationNetworks = cooperationNetworks;
    }

    public List<String> getTeachingLanguage() {
        return teachingLanguage;
    }

    public void setTeachingLanguage(List<String> teachingLanguage) {
        this.teachingLanguage = teachingLanguage;
    }

    public void setStatus(StudyStatus status) {
        this.status = status;
    }

    public StudyStatus getStatus() {
        return status;
    }

    public List<MinEduGuidanceArea> getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(List<MinEduGuidanceArea> minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseUnitRealisationEntity)) return false;
        CourseUnitRealisationEntity that = (CourseUnitRealisationEntity) o;
        return isEnrollmentClosed() == that.isEnrollmentClosed() &&
            Objects.equals(getRealisationId(), that.getRealisationId()) &&
            Objects.equals(getRealisationIdentifierCode(), that.getRealisationIdentifierCode()) &&
            Objects.equals(getOrganizingOrganisationId(), that.getOrganizingOrganisationId()) &&
            Objects.equals(getName(), that.getName()) &&
            Objects.equals(getEnrollmentStartDateTime(), that.getEnrollmentStartDateTime()) &&
            Objects.equals(getEnrollmentEndDateTime(), that.getEnrollmentEndDateTime()) &&
            Objects.equals(getStartDate(), that.getStartDate()) &&
            Objects.equals(getEndDate(), that.getEndDate()) &&
            Objects.equals(getCooperationNetworks(), that.getCooperationNetworks()) &&
            Objects.equals(getTeachingLanguage(), that.getTeachingLanguage()) &&
            getStatus() == that.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRealisationId(), getRealisationIdentifierCode(), getOrganizingOrganisationId(), getName(), getEnrollmentStartDateTime(), getEnrollmentEndDateTime(), getStartDate(), getEndDate(), isEnrollmentClosed(), getCooperationNetworks(), getTeachingLanguage(), getStatus());
    }
}
