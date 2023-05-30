package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.ScaleValue;
import org.springframework.data.annotation.Transient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class CourseUnit extends StudyElement {

    private ScaleValue assessmentScale;
    private List<LocalisedString> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOption> completionOptions;
    private List<Realisation> realisations = null;

    // This field is not saved into opintojaksot-index. Field only has effect if it is given with
    // create/update studyModule message in subelement courseunit. -> this will be added to parent ref
    // onlyEnrollableWithParent field
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean onlyEnrollableWithParent;

    public CourseUnit() {
        setType(StudyElementType.COURSE_UNIT);
    }

    /**
     * @return Code return the assessmentScale
     */
    public ScaleValue getAssessmentScale() {
        return assessmentScale;
    }

    /**
     * @param assessmentScale the assessmentScale to set
     */
    public void setAssessmentScale(ScaleValue assessmentScale) {
        this.assessmentScale = assessmentScale;
    }

    /**
     * @return List<LocalisedString> return the learningMaterials
     */
    public List<LocalisedString> getLearningMaterials() {
        return learningMaterials;
    }

    /**
     * @param learningMaterials the learningMaterials to set
     */
    public void setLearningMaterials(List<LocalisedString> learningMaterials) {
        this.learningMaterials = learningMaterials;
    }

    /**
     * @return Boolean return the examBookEnquiry
     */
    public Boolean isExamBookEnquiry() {
        return examBookEnquiry;
    }


    public Boolean getExamBookEnquiry() {
        return examBookEnquiry;
    }

    /**
     * @param examBookEnquiry the examBookEnquiry to set
     */
    public void setExamBookEnquiry(Boolean examBookEnquiry) {
        this.examBookEnquiry = examBookEnquiry;
    }

    /**
     * @return Integer return the groupSize
     */
    public Integer getGroupSize() {
        return groupSize;
    }

    /**
     * @param groupSize the groupSize to set
     */
    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }

    /**
     * @return List<CompletionOption> return the completionOptions
     */
    public List<CompletionOption> getCompletionOptions() {
        return completionOptions;
    }

    /**
     * @param completionOptions the completionOptions to set
     */
    public void setCompletionOptions(List<CompletionOption> completionOptions) {
        this.completionOptions = completionOptions;
    }

    /**
     * @return List<Realisation> return the realisations
     */
    public List<Realisation> getRealisations() {
        return realisations;
    }

    /**
     * @param realisations the realisations to set
     */
    public void setRealisations(List<Realisation> realisations) {
        this.realisations = realisations;
    }

    public Boolean getOnlyEnrollableWithParent() {
        return onlyEnrollableWithParent;
    }

    public void setOnlyEnrollableWithParent(Boolean onlyEnrollableWithParent) {
        this.onlyEnrollableWithParent = onlyEnrollableWithParent;
    }

    @JsonIgnore
    @Transient
    public List<AssessmentItem> getAssessmentItems() {
        List<AssessmentItem> assessmentItemEntities = new ArrayList<>();
        if(!CollectionUtils.isEmpty(completionOptions)) {
            for(CompletionOption completionOption : completionOptions) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    assessmentItemEntities.addAll(completionOption.getAssessmentItems());
                }
            }
        }
        return assessmentItemEntities;
    }
}
