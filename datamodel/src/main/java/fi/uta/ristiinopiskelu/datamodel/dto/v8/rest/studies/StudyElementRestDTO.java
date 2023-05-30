package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies;

import com.fasterxml.jackson.annotation.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.*;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.CodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.ExternalCodeReference;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.code.validation.CodeSetConstraint;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.MinEduGuidanceArea;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Schema(name = "StudyElement")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StudyModuleRestDTO.class, name = "STUDY_MODULE"),
    @JsonSubTypes.Type(value = CourseUnitRestDTO.class, name = "COURSE_UNIT"),
    @JsonSubTypes.Type(value = DegreeRestDTO.class, name = "DEGREE")
})
@JsonInclude(NON_NULL)
public abstract class StudyElementRestDTO {

    @JsonProperty("missing")
    private Boolean missing = null;

    @JsonProperty("sendingTime")
    private OffsetDateTime sendingTime = null;

    @JsonProperty("createdTime")
    private OffsetDateTime createdTime = null;

    @JsonProperty("updateTime")
    private OffsetDateTime updateTime = null;

    /** fi: voimassaolo alkaa */
    @JsonProperty("validityStartDate")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate validityStartDate = null;

    /** fi: voimassaolo päättyy */
    @JsonProperty("validityEndDate")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate validityEndDate = null;

    @JsonProperty("studyElementId")
    private String studyElementId = null;

    @JsonProperty("studyElementPermanentId")
    private String studyElementPermanentId = null;

    @JsonProperty("studyElementIdentifierCode")
    private String studyElementIdentifierCode = null;

    @Valid
    @JsonProperty("classificationCode")
    private List<ExternalCodeReference> classificationCodes = null;

    @JsonProperty("abbreviation")
    private String abbreviation = null;

    @JsonProperty("name")
    private LocalisedString name = null;

    @JsonProperty("description")
    private LocalisedString description = null;

    @JsonProperty("creditsMin")
    @Schema(description = "M2 1.22.2")
    private BigDecimal creditsMin = null;

    @JsonProperty("creditsMax")
    @Schema(description = "M2 1.22.3")
    private BigDecimal creditsMax = null;

    @JsonProperty("creditsDescription")
    private LocalisedString creditsDescription = null;

    @JsonProperty("organisationReferences")
    private List<OrganisationReference> organisationReferences = null;

    @JsonProperty("personReferences")
    private List<PersonReference> personReferences = null;

    @JsonProperty("keywords")
    private List<Keyword> keywords = null;

    @JsonProperty("preconditions")
    private LocalisedString preconditions = null;

    @JsonProperty("teachingLanguage")
    private List<String> teachingLanguage = null;

    @JsonProperty("languagesOfCompletion")
    private List<String> languagesOfCompletion = null;

    @JsonProperty("furtherInformation")
    private LocalisedString furtherInformation = null;

    /**
     * - Flexible Study Rights Agreement (JOO) studies
     *
     * - AMK joint studies
     *
     * - Exchange studies
     *
     * - Open University
     *
     * - Open university of applied sciences
     */
    @Valid
    @JsonProperty("targetGroups")
    private List< @CodeSetConstraint(codeSetKey = "study_right_type") CodeReference> targetGroups = null;

    @JsonProperty("cooperationNetworks")
    private List<CooperationNetwork> cooperationNetworks = null;

    @JsonProperty("subElements")
    private List<StudyElementRestDTO> subElements;

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

    /**
     *
     * M2 0.3 CSCRISTIINOPISKELU-184
     *
     * @return sendingTime
     **/
    public OffsetDateTime getSendingTime() {
        return sendingTime;
    }

    /**
     * @param sendingTime the sendingTime to set
     */
    public void setSendingTime(OffsetDateTime sendingTime) {
        this.sendingTime = sendingTime;
    }

    /**
     * @return OffsetDateTime return the createdTime
     */
    public OffsetDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * @param createdTime the createdTime to set
     */
    public void setCreatedTime(OffsetDateTime createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * M2 1.1.2
     *
     * @return id
     **/
    public String getStudyElementId() {
        return studyElementId;
    }

    /**
     * @param studyElementId the id to set
     */
    public void setStudyElementId(String studyElementId) {
        this.studyElementId = studyElementId;
    }

    public String getStudyElementPermanentId() {
        return studyElementPermanentId;
    }

    public void setStudyElementPermanentId(String studyElementPermanentId) {
        this.studyElementPermanentId = studyElementPermanentId;
    }

    /**
     * @return String return the code
     */
    public String getStudyElementIdentifierCode() {
        return studyElementIdentifierCode;
    }

    /**
     * @param studyElementIdentifierCode the code to set
     */
    public void setStudyElementIdentifierCode(String studyElementIdentifierCode) {
        this.studyElementIdentifierCode = studyElementIdentifierCode;
    }

    /**
     * M2 1.2.2
     *
     * @return abbreviation
     **/
    @Schema(description = "M2 1.2.2")
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * @param abbreviation the abbreviation to set
     */
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public List<ExternalCodeReference> getClassificationCodes() {
        return classificationCodes;
    }

    public void setClassificationCodes(List<ExternalCodeReference> classificationCodes) {
        this.classificationCodes = classificationCodes;
    }

    /**
     * M2 1.2.1
     *
     * @return name
     **/
    @Schema(required = true, description = "M2 1.2.1")
    public LocalisedString getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(LocalisedString name) {
        this.name = name;
    }

    @Schema(description = "M2 1.22.5")
    public LocalisedString getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(LocalisedString description) {
        this.description = description;
    }

    /**
     * @return BigDecimal return the creditsMin
     */
    public BigDecimal getCreditsMin() {
        return creditsMin;
    }

    /**
     * @param creditsMin the creditsMin to set
     */
    public void setCreditsMin(BigDecimal creditsMin) {
        this.creditsMin = creditsMin;
    }

    /**
     * @return BigDecimal return the creditsMax
     */
    public BigDecimal getCreditsMax() {
        return creditsMax;
    }

    /**
     * @param creditsMax the creditsMax to set
     */
    public void setCreditsMax(BigDecimal creditsMax) {
        this.creditsMax = creditsMax;
    }

    /**
     * @return LocalisedString return the creditsDescription
     */
    public LocalisedString getCreditsDescription() {
        return creditsDescription;
    }

    /**
     * @param creditsDescription the creditsDescription to set
     */
    public void setCreditsDescription(LocalisedString creditsDescription) {
        this.creditsDescription = creditsDescription;
    }

    /**
     * @return List<Keyword> return the keywords
     */
    public List<Keyword> getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    /**
     * @return LocalisedString return the preconditions
     */
    public LocalisedString getPreconditions() {
        return preconditions;
    }

    /**
     * @param preconditions the preconditions to set
     */
    public void setPreconditions(LocalisedString preconditions) {
        this.preconditions = preconditions;
    }

    /**
     * @return List<String> return the teachingLanguage
     */
    public List<String> getTeachingLanguage() {
        return teachingLanguage;
    }

    /**
     * @param teachingLanguage the teachingLanguage to set
     */
    public void setTeachingLanguage(List<String> teachingLanguage) {
        this.teachingLanguage = teachingLanguage;
    }

    /**
     * @return List<String> return the languagesOfCompletion
     */
    public List<String> getLanguagesOfCompletion() {
        return languagesOfCompletion;
    }

    /**
     * @param languagesOfCompletion the languagesOfCompletion to set
     */
    public void setLanguagesOfCompletion(List<String> languagesOfCompletion) {
        this.languagesOfCompletion = languagesOfCompletion;
    }

    /**
     * @return List<CooperationNetwork> return the cooperationNetwork
     */
    public List<CooperationNetwork> getCooperationNetworks() {
        return cooperationNetworks;
    }

    /**
     * @param cooperationNetworks the cooperationNetwork to set
     */
    public void setCooperationNetworks(List<CooperationNetwork> cooperationNetworks) {
        this.cooperationNetworks = cooperationNetworks;
    }

    /**
     * @return List<StudyElement> return the subElements
     */
    public List<StudyElementRestDTO> getSubElements() {
        return subElements;
    }

    /**
     * @param subElements the subElements to set
     */
    public void setSubElements(List<StudyElementRestDTO> subElements) {
        this.subElements = subElements;
    }

    /**
     * @return List<OrganisationReference> return the organisationReferences
     */
    public List<OrganisationReference> getOrganisationReferences() {
        return organisationReferences;
    }

    /**
     * @param organisationReferences the organisationReferences to set
     */
    public void setOrganisationReferences(List<OrganisationReference> organisationReferences) {
        this.organisationReferences = organisationReferences;
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
     * @return List<Code> return the targetGroups
     */
    public List<CodeReference> getTargetGroups() {
        return targetGroups;
    }

    /**
     * @param targetGroups the targetGroups to set
     */
    public void setTargetGroups(List<CodeReference> targetGroups) {
        this.targetGroups = targetGroups;
    }

    /**
     * @return OffsetDateTime return the validityStartDate
     */
    public LocalDate getValidityStartDate() {
        return validityStartDate;
    }

    /**
     * @param validityStartDate the validityStartDate to set
     */
    public void setValidityStartDate(LocalDate validityStartDate) {
        this.validityStartDate = validityStartDate;
    }

    /**
     * @return OffsetDateTime return the validityEndDate
     */
    public LocalDate getValidityEndDate() {
        return validityEndDate;
    }

    /**
     * @param validityEndDate the validityEndDate to set
     */
    public void setValidityEndDate(LocalDate validityEndDate) {
        this.validityEndDate = validityEndDate;
    }

    /**
     * @return LocalisedString return the furtherInformation
     */
    public LocalisedString getFurtherInformation() {
        return furtherInformation;
    }

    /**
     * @param furtherInformation the furtherInformation to set
     */
    public void setFurtherInformation(LocalisedString furtherInformation) {
        this.furtherInformation = furtherInformation;
    }

    /**
     * @return LocalisedString return the objective
     */
    public LocalisedString getObjective() {
        return objective;
    }

    /**
     * @param objective the objective to set
     */
    public void setObjective(LocalisedString objective) {
        this.objective = objective;
    }

    /**
     * @return LocalisedString return the content
     */
    public LocalisedString getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(LocalisedString content) {
        this.content = content;
    }

    /**
     * @return Type return the type
     */
    public StudyElementType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(StudyElementType type) {
        this.type = type;
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

    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(OffsetDateTime updateTime) {
        this.updateTime = updateTime;
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

    @JsonIgnore
    public String getOrganizingOrganisationId() {
        if(!CollectionUtils.isEmpty(this.getOrganisationReferences())) {
            for(OrganisationReference ref : this.getOrganisationReferences()) {
                if (ref.getOrganisationRole() == OrganisationRole.ROLE_MAIN_ORGANIZER) {
                    return ref.getOrganisation().getOrganisationTkCode();
                }
            }
        }

        return null;
    }
}
