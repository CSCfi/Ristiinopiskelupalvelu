package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = "toteutukset", createIndex = false)
public class RealisationEntity extends CompositeIdentifiedEntity implements Serializable {

    private String realisationId;
    private String realisationIdentifierCode;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime createdTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime updateTime;
    
    private List<StudyElementReference> studyElementReferences;
    private List<Selection> selections = null;
    private List<QuestionSet> questionSets = null;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentStartDateTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime enrollmentEndDateTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate = null;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate = null;

    private List<PersonReference> personReferences = null;
    private LocalisedString name = null;
    private Integer minSeats;
    private Integer maxSeats;
    private Address location;
    private List<Description> organisationSpecificDescriptions = null;
    private List<OrganisationReference> organisationReferences;
    private String organizingOrganisationId;
    private List<Selection> groupSelections = null;
    private List<CooperationNetwork> cooperationNetworks;
    private List<GroupQuota> groupQuotas;
    private StudyStatus status;
    private BigDecimal creditsMin;
    private BigDecimal creditsMax;
    private List<String> teachingLanguage;
    private List<MinEduGuidanceArea> minEduGuidanceArea;
    
    private boolean enrollmentClosed = false;

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

    /**
     * @return List<Selection> return the selections
     */
    public List<Selection> getSelections() {
        return selections;
    }

    /**
     * @param selections the selections to set
     */
    public void setSelections(List<Selection> selections) {
        this.selections = selections;
    }

    /**
     * @return List<QuestionSet> return the questionSets
     */
    public List<QuestionSet> getQuestionSets() {
        return questionSets;
    }

    /**
     * @param questionSets the questionSets to set
     */
    public void setQuestionSets(List<QuestionSet> questionSets) {
        this.questionSets = questionSets;
    }

    /**
     * @return OffsetDateTime return the enrollmentStartDateTime
     */
    public OffsetDateTime getEnrollmentStartDateTime() {
        return enrollmentStartDateTime;
    }

    /**
     * @param enrollmentStartDateTime the enrollmentStartDateTime to set
     */
    public void setEnrollmentStartDateTime(OffsetDateTime enrollmentStartDateTime) {
        this.enrollmentStartDateTime = enrollmentStartDateTime;
    }

    /**
     * @return OffsetDateTime return the enrollmentEndDateTime
     */
    public OffsetDateTime getEnrollmentEndDateTime() {
        return enrollmentEndDateTime;
    }

    /**
     * @param enrollmentEndDateTime the enrollmentEndDateTime to set
     */
    public void setEnrollmentEndDateTime(OffsetDateTime enrollmentEndDateTime) {
        this.enrollmentEndDateTime = enrollmentEndDateTime;
    }

    /**
     * @return int return the minSeats
     */
    public Integer getMinSeats() {
        return minSeats;
    }

    /**
     * @param minSeats the minSeats to set
     */
    public void setMinSeats(Integer minSeats) {
        this.minSeats = minSeats;
    }

    /**
     * @return int return the maxSeats
     */
    public Integer getMaxSeats() {
        return maxSeats;
    }

    /**
     * @param maxSeats the maxSeats to set
     */
    public void setMaxSeats(Integer maxSeats) {
        this.maxSeats = maxSeats;
    }

    /**
     * @return List<PersonReference> return the personReferences
     */
    public List<PersonReference> getPersonReferences() {
        return personReferences;
    }

    /**
     * @param personReferences the personReferences to set
     */
    public void setPersonReferences(List<PersonReference> personReferences) {
        this.personReferences = personReferences;
    }

    /**
     * @return Address return the location
     */
    public Address getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(Address location) {
        this.location = location;
    }

    /**
     * @return List<DescriptionField> return the organisationSpecificDescriptions
     */
    public List<Description> getOrganisationSpecificDescriptions() {
        return organisationSpecificDescriptions;
    }

    /**
     * @param organisationSpecificDescriptions the organisationSpecificDescriptions
     *                                         to set
     */
    public void setOrganisationSpecificDescriptions(List<Description> organisationSpecificDescriptions) {
        this.organisationSpecificDescriptions = organisationSpecificDescriptions;
    }

    public List<StudyElementReference> getStudyElementReferences() {
        return studyElementReferences;
    }

    public void setStudyElementReferences(List<StudyElementReference> studyElementReferences) {
        this.studyElementReferences = studyElementReferences;
    }

    public void setOrganisationReferences(List<OrganisationReference> organisationReferences) {
        this.organisationReferences = organisationReferences;
    }

    public List<OrganisationReference> getOrganisationReferences() {
        return organisationReferences;
    }

    @Override
    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public List<Selection> getGroupSelections() {
        return groupSelections;
    }

    public void setGroupSelections(List<Selection> groupSelections) {
        this.groupSelections = groupSelections;
    }

    public List<CooperationNetwork> getCooperationNetworks() {
        return cooperationNetworks;
    }

    public void setCooperationNetworks(List<CooperationNetwork> cooperationNetworks) {
        this.cooperationNetworks = cooperationNetworks;
    }

    public OffsetDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(OffsetDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(OffsetDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<GroupQuota> getGroupQuotas() {
        return groupQuotas;
    }

    public void setGroupQuotas(List<GroupQuota> groupQuotas) {
        this.groupQuotas = groupQuotas;
    }

    public StudyStatus getStatus() {
        return status;
    }

    public void setStatus(StudyStatus status) {
        this.status = status;
    }

    public BigDecimal getCreditsMin() {
        return creditsMin;
    }

    public void setCreditsMin(BigDecimal creditsMin) {
        this.creditsMin = creditsMin;
    }

    public BigDecimal getCreditsMax() {
        return creditsMax;
    }

    public void setCreditsMax(BigDecimal creditsMax) {
        this.creditsMax = creditsMax;
    }

    public List<String> getTeachingLanguage() {
        return teachingLanguage;
    }

    public void setTeachingLanguage(List<String> teachingLanguage) {
        this.teachingLanguage = teachingLanguage;
    }

    public List<MinEduGuidanceArea> getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(List<MinEduGuidanceArea> minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }

    public boolean isEnrollmentClosed() {
        return enrollmentClosed;
    }

    public void setEnrollmentClosed(boolean enrollmentClosed) {
        this.enrollmentClosed = enrollmentClosed;
    }

    @JsonIgnore
    @Transient
    @Override
    public String getElementId() {
        return realisationId;
    }

    @JsonIgnore
    @Transient
    @Override
    public CompositeIdentifiedEntityType getType() {
        return CompositeIdentifiedEntityType.REALISATION;
    }
}
