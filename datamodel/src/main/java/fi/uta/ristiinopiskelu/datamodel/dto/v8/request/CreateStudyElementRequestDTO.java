package fi.uta.ristiinopiskelu.datamodel.dto.v8.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.ExternalCodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.validation.CodeSetConstraint;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.MinEduGuidanceArea;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible=true)
@JsonSubTypes({ @JsonSubTypes.Type(value = CreateStudyModuleRequestDTO.class, name = "STUDY_MODULE"),
    @JsonSubTypes.Type(value = CreateCourseUnitRequestDTO.class, name = "COURSE_UNIT"),
    @JsonSubTypes.Type(value = CreateDegreeRequestDTO.class, name = "DEGREE")
})
@JsonInclude(NON_NULL)
public abstract class CreateStudyElementRequestDTO {

    private Boolean missing = null;
    private OffsetDateTime sendingTime = null;
    private OffsetDateTime createdTime = null;
    private OffsetDateTime updateTime = null;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate validityStartDate = null;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate validityEndDate = null;

    private String studyElementId = null;
    private String studyElementPermanentId = null;
    private String studyElementIdentifierCode = null;
    private List<ExternalCodeReference> classificationCodes = null;
    private String abbreviation = null;
    private LocalisedString name = null;
    private LocalisedString description = null;
    private BigDecimal creditsMin = null;
    private BigDecimal creditsMax = null;
    private LocalisedString creditsDescription = null;
    private List<OrganisationReference> organisationReferences = null;
    private List<PersonReference> personReferences = null;
    private List<Keyword> keywords = null;
    private LocalisedString preconditions = null;
    private List<String> teachingLanguage = null;
    private List<String> languagesOfCompletion = null;
    private LocalisedString furtherInformation = null;
    private List<@CodeSetConstraint(codeSetKey = "study_right_type") CodeReference> targetGroups = null;
    private List<CooperationNetwork> cooperationNetworks = null;
    private List<CreateStudyElementRequestDTO> subElements;
    private LocalisedString objective;
    private LocalisedString content;
    private StudyElementType type;
    private List<Description> organisationSpecificDescriptions = null;
    private List<StudyElementReference> parents = null;
    private StudyStatus status;
    private MinEduGuidanceArea minEduGuidanceArea;

    public Boolean getMissing() {
        return missing;
    }

    public void setMissing(Boolean missing) {
        this.missing = missing;
    }

    public OffsetDateTime getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(OffsetDateTime sendingTime) {
        this.sendingTime = sendingTime;
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

    public LocalDate getValidityStartDate() {
        return validityStartDate;
    }

    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    public LocalDate getValidityEndDate() {
        return validityEndDate;
    }

    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    public String getStudyElementId() {
        return studyElementId;
    }

    public void setStudyElementId(String studyElementId) {
        this.studyElementId = studyElementId;
    }

    public String getStudyElementPermanentId() {
        return studyElementPermanentId;
    }

    public void setStudyElementPermanentId(String studyElementPermanentId) {
        this.studyElementPermanentId = studyElementPermanentId;
    }

    public String getStudyElementIdentifierCode() {
        return studyElementIdentifierCode;
    }

    public void setStudyElementIdentifierCode(String studyElementIdentifierCode) {
        this.studyElementIdentifierCode = studyElementIdentifierCode;
    }

    public List<ExternalCodeReference> getClassificationCodes() {
        return classificationCodes;
    }

    public void setClassificationCodes(List<ExternalCodeReference> classificationCodes) {
        this.classificationCodes = classificationCodes;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public LocalisedString getDescription() {
        return description;
    }

    public void setDescription(LocalisedString description) {
        this.description = description;
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

    public LocalisedString getCreditsDescription() {
        return creditsDescription;
    }

    public void setCreditsDescription(LocalisedString creditsDescription) {
        this.creditsDescription = creditsDescription;
    }

    public List<OrganisationReference> getOrganisationReferences() {
        return organisationReferences;
    }

    public void setOrganisationReferences(List<OrganisationReference> organisationReferences) {
        this.organisationReferences = organisationReferences;
    }

    public List<PersonReference> getPersonReferences() {
        return personReferences;
    }

    public void setPersonReferences(List<PersonReference> personReferences) {
        this.personReferences = personReferences;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public LocalisedString getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(LocalisedString preconditions) {
        this.preconditions = preconditions;
    }

    public List<String> getTeachingLanguage() {
        return teachingLanguage;
    }

    public void setTeachingLanguage(List<String> teachingLanguage) {
        this.teachingLanguage = teachingLanguage;
    }

    public List<String> getLanguagesOfCompletion() {
        return languagesOfCompletion;
    }

    public void setLanguagesOfCompletion(List<String> languagesOfCompletion) {
        this.languagesOfCompletion = languagesOfCompletion;
    }

    public LocalisedString getFurtherInformation() {
        return furtherInformation;
    }

    public void setFurtherInformation(LocalisedString furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    public List<CodeReference> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(List<CodeReference> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public List<CooperationNetwork> getCooperationNetworks() {
        return cooperationNetworks;
    }

    public void setCooperationNetworks(List<CooperationNetwork> cooperationNetworks) {
        this.cooperationNetworks = cooperationNetworks;
    }

    public List<CreateStudyElementRequestDTO> getSubElements() {
        return subElements;
    }

    public void setSubElements(List<CreateStudyElementRequestDTO> subElements) {
        this.subElements = subElements;
    }

    public LocalisedString getObjective() {
        return objective;
    }

    public void setObjective(LocalisedString objective) {
        this.objective = objective;
    }

    public LocalisedString getContent() {
        return content;
    }

    public void setContent(LocalisedString content) {
        this.content = content;
    }

    public StudyElementType getType() {
        return type;
    }

    public void setType(StudyElementType type) {
        this.type = type;
    }

    public List<Description> getOrganisationSpecificDescriptions() {
        return organisationSpecificDescriptions;
    }

    public void setOrganisationSpecificDescriptions(List<Description> organisationSpecificDescriptions) {
        this.organisationSpecificDescriptions = organisationSpecificDescriptions;
    }

    public List<StudyElementReference> getParents() {
        return parents;
    }

    public void setParents(List<StudyElementReference> parents) {
        this.parents = parents;
    }

    public StudyStatus getStatus() {
        return status;
    }

    public void setStatus(StudyStatus status) {
        this.status = status;
    }

    public MinEduGuidanceArea getMinEduGuidanceArea() {
        return minEduGuidanceArea;
    }

    public void setMinEduGuidanceArea(MinEduGuidanceArea minEduGuidanceArea) {
        this.minEduGuidanceArea = minEduGuidanceArea;
    }
}
