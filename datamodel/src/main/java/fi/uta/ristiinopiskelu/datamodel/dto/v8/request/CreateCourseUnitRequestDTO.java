package fi.uta.ristiinopiskelu.datamodel.dto.v8.request;

import fi.uta.ristiinopiskelu.datamodel.dto.v8.CompletionOption;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.Realisation;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.ScaleValue;

import java.util.List;

public class CreateCourseUnitRequestDTO extends CreateStudyElementRequestDTO {

    private ScaleValue assessmentScale;
    private List<LocalisedString> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOption> completionOptions;
    private List<Realisation> realisations = null;

    // This field is not saved into opintojaksot-index. Field only has effect if it is given with
    // create/update studyModule message in subelement courseunit. -> this will be added to parent ref
    // onlyEnrollableWithParent field
    private Boolean onlyEnrollableWithParent;

    public ScaleValue getAssessmentScale() {
        return assessmentScale;
    }

    public void setAssessmentScale(ScaleValue assessmentScale) {
        this.assessmentScale = assessmentScale;
    }

    public List<LocalisedString> getLearningMaterials() {
        return learningMaterials;
    }

    public void setLearningMaterials(List<LocalisedString> learningMaterials) {
        this.learningMaterials = learningMaterials;
    }

    public Boolean getExamBookEnquiry() {
        return examBookEnquiry;
    }

    public void setExamBookEnquiry(Boolean examBookEnquiry) {
        this.examBookEnquiry = examBookEnquiry;
    }

    public Integer getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }

    public List<CompletionOption> getCompletionOptions() {
        return completionOptions;
    }

    public void setCompletionOptions(List<CompletionOption> completionOptions) {
        this.completionOptions = completionOptions;
    }

    public List<Realisation> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<Realisation> realisations) {
        this.realisations = realisations;
    }

    public Boolean getOnlyEnrollableWithParent() {
        return onlyEnrollableWithParent;
    }

    public void setOnlyEnrollableWithParent(Boolean onlyEnrollableWithParent) {
        this.onlyEnrollableWithParent = onlyEnrollableWithParent;
    }
}
