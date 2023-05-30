package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.*;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.code.ExternalCodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.MinEduGuidanceArea;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = StudyModuleEntity.class, name = "STUDY_MODULE"),
    @JsonSubTypes.Type(value = CourseUnitEntity.class, name = "COURSE_UNIT"),
    @JsonSubTypes.Type(value = DegreeEntity.class, name = "DEGREE")
})
public abstract class StudyElementEntity extends CompositeIdentifiedEntity implements Serializable {
    private Boolean missing;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime sendingTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime createdTime;

    @Field(type = FieldType.Date, pattern = {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX"})
    private OffsetDateTime updateTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityStartDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate validityEndDate;

    private String studyElementPermanentId;
    private String studyElementId;
    private String studyElementIdentifierCode;
    private List<ExternalCodeReference> classificationCodes;
    private String abbreviation;
    private LocalisedString name;
    private LocalisedString description;
    private BigDecimal creditsMin;
    private BigDecimal creditsMax;
    private LocalisedString creditsDescription;
    private List<OrganisationReference> organisationReferences;
    private List<PersonReference> personReferences;
    private List<Keyword> keywords;
    private LocalisedString preconditions;
    private List<String> teachingLanguage;
    private List<String> languagesOfCompletion;
    private LocalisedString furtherInformation;
    private List<CodeReference> targetGroups;
    private List<CooperationNetwork> cooperationNetworks;
    private LocalisedString objective;
    private LocalisedString content;
    private List<Description> organisationSpecificDescriptions;
    private String organizingOrganisationId;
    private List<StudyElementReference> parents;
    private StudyStatus status;

    @Field(type = FieldType.Integer)
    private MinEduGuidanceArea minEduGuidanceArea;
    
    private CompositeIdentifiedEntityType type;

    public void setSendingTime(OffsetDateTime sendingTime) {
        this.sendingTime = sendingTime;
    }

    public void setCreatedTime(OffsetDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    public void setStudyElementIdentifierCode(String studyElementIdentifierCode) {
        this.studyElementIdentifierCode = studyElementIdentifierCode;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public void setDescription(LocalisedString description) {
        this.description = description;
    }

    public void setCreditsMin(BigDecimal creditsMin) {
        this.creditsMin = creditsMin;
    }

    public void setCreditsMax(BigDecimal creditsMax) {
        this.creditsMax = creditsMax;
    }

    public void setCreditsDescription(LocalisedString creditsDescription) {
        this.creditsDescription = creditsDescription;
    }

    public void setOrganisationReferences(List<OrganisationReference> organisationReferences) {
        this.organisationReferences = organisationReferences;
    }

    public void setPersonReferences(List<PersonReference> personReferences) {
        this.personReferences = personReferences;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public void setPreconditions(LocalisedString preconditions) {
        this.preconditions = preconditions;
    }

    public void setTeachingLanguage(List<String> teachingLanguage) {
        this.teachingLanguage = teachingLanguage;
    }

    public void setLanguagesOfCompletion(List<String> languagesOfCompletion) {
        this.languagesOfCompletion = languagesOfCompletion;
    }

    public void setFurtherInformation(LocalisedString furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    public void setTargetGroups(List<CodeReference> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public void setCooperationNetworks(List<CooperationNetwork> cooperationNetworks) {
        this.cooperationNetworks = cooperationNetworks;
    }

    public void setObjective(LocalisedString objective) {
        this.objective = objective;
    }

    public void setContent(LocalisedString content) {
        this.content = content;
    }

    public void setOrganisationSpecificDescriptions(List<Description> organisationSpecificDescriptions) {
        this.organisationSpecificDescriptions = organisationSpecificDescriptions;
    }

    public OffsetDateTime getSendingTime() {
        return this.sendingTime;
    }

    public OffsetDateTime getCreatedTime() {
        return this.createdTime;
    }

    public LocalDate getValidityStartDate() {
        return this.validityStartDate;
    }

    public LocalDate getValidityEndDate() {
        return this.validityEndDate;
    }

    public String getStudyElementIdentifierCode() {
        return this.studyElementIdentifierCode;
    }

    public String getAbbreviation() {
        return this.abbreviation;
    }

    public LocalisedString getName() {
        return this.name;
    }

    public LocalisedString getDescription() {
        return this.description;
    }

    public BigDecimal getCreditsMin() {
        return this.creditsMin;
    }

    public BigDecimal getCreditsMax() {
        return this.creditsMax;
    }

    public LocalisedString getCreditsDescription() {
        return this.creditsDescription;
    }

    public List<OrganisationReference> getOrganisationReferences() {
        return this.organisationReferences;
    }

    public List<PersonReference> getPersonReferences() {
        return this.personReferences;
    }

    public List<Keyword> getKeywords() {
        return this.keywords;
    }

    public LocalisedString getPreconditions() {
        return this.preconditions;
    }

    public List<String> getTeachingLanguage() {
        return this.teachingLanguage;
    }

    public List<String> getLanguagesOfCompletion() {
        return this.languagesOfCompletion;
    }

    public LocalisedString getFurtherInformation() {
        return this.furtherInformation;
    }

    public List<CodeReference> getTargetGroups() {
        return this.targetGroups;
    }

    @Override
    public List<CooperationNetwork> getCooperationNetworks() {
        return this.cooperationNetworks;
    }

    public LocalisedString getObjective() {
        return this.objective;
    }

    public LocalisedString getContent() {
        return this.content;
    }

    public List<Description> getOrganisationSpecificDescriptions() {
        return this.organisationSpecificDescriptions;
    }

    @Override
    public String getOrganizingOrganisationId() {
        return organizingOrganisationId;
    }

    @Override
    public void setOrganizingOrganisationId(String organizingOrganisationId) {
        this.organizingOrganisationId = organizingOrganisationId;
    }

    public String getStudyElementId() {
        return studyElementId;
    }

    public void setStudyElementId(String studyElementId) {
        this.studyElementId = studyElementId;
    }

    public List<ExternalCodeReference> getClassificationCodes() {
        return classificationCodes;
    }

    public void setClassificationCodes(List<ExternalCodeReference> classificationCodes) {
        this.classificationCodes = classificationCodes;
    }

    public List<StudyElementReference> getParents() {
        return parents;
    }

    public void setParents(List<StudyElementReference> parents) {
        this.parents = parents;
    }

    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(OffsetDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public StudyStatus getStatus() {
        return status;
    }

    public void setStatus(StudyStatus status) {
        this.status = status;
    }

    public Boolean getMissing() {
        return missing;
    }

    public void setMissing(Boolean missing) {
        this.missing = missing;
    }

    public String getStudyElementPermanentId() {
        return studyElementPermanentId;
    }

    public void setStudyElementPermanentId(String studyElementPermanentId) {
        this.studyElementPermanentId = studyElementPermanentId;
    }

    public MinEduGuidanceArea getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(MinEduGuidanceArea minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }

    public void setType(CompositeIdentifiedEntityType type) {
        this.type = type;
    }

    @Override
    public CompositeIdentifiedEntityType getType() {
        return type;
    }

    @JsonIgnore
    @Transient
    @Override
    public String getElementId() {
        return studyElementId;
    }
    
    public String toString() {
        return "StudyElement(id=" + this.getId() + ", missing=" + this.getMissing() +
                ", sendingTime=" + this.getSendingTime() + ", createdTime=" + this.getCreatedTime() +
                ", validityStartDate=" + this.getValidityStartDate() + ", validityEndDate=" + this.getValidityEndDate() +
                ", studyElementPermanentId=" + this.getStudyElementPermanentId() + ", code=" + this.getStudyElementIdentifierCode() +
                ", abbreviation=" + this.getAbbreviation() + ", name=" + this.getName() + ", description=" + this.getDescription() +
                ", creditsMin=" + this.getCreditsMin() + ", creditsMax=" + this.getCreditsMax() + ", creditsDescription=" + this.getCreditsDescription() +
                ", organisationReferences=" + this.getOrganisationReferences() + ", personReferences=" + this.getPersonReferences() +
                ", keywords=" + this.getKeywords() + ", preconditions=" + this.getPreconditions() + ", teachingLanguage=" + this.getTeachingLanguage() +
                ", languagesOfCompletion=" + this.getLanguagesOfCompletion() + ", furtherInformation=" + this.getFurtherInformation() +
                ", targetGroups=" + this.getTargetGroups() + ", cooperationNetworks=" + this.getCooperationNetworks() +
                ", objective=" + this.getObjective() + ", content=" + this.getContent() +
                ", organisationSpecificDescriptions=" + this.getOrganisationSpecificDescriptions() + ")";
    }

}
