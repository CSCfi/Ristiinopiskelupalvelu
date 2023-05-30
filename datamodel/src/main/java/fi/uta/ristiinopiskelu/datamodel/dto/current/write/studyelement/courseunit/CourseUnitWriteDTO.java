package fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.ScaleValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import org.springframework.data.annotation.Transient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class CourseUnitWriteDTO extends AbstractStudyElementWriteDTO {

    private ScaleValue assessmentScale;
    private List<LocalisedString> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOptionWriteDTO> completionOptions;
    private List<RealisationWriteDTO> realisations = null;

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

    public List<CompletionOptionWriteDTO> getCompletionOptions() {
        return completionOptions;
    }

    public void setCompletionOptions(List<CompletionOptionWriteDTO> completionOptions) {
        this.completionOptions = completionOptions;
    }

    public List<RealisationWriteDTO> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<RealisationWriteDTO> realisations) {
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
    public List<AssessmentItemWriteDTO> getAssessmentItems() {
        List<AssessmentItemWriteDTO> assessmentItemEntities = new ArrayList<>();
        if(!CollectionUtils.isEmpty(completionOptions)) {
            for(CompletionOptionWriteDTO completionOption : completionOptions) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    assessmentItemEntities.addAll(completionOption.getAssessmentItems());
                }
            }
        }
        return assessmentItemEntities;
    }
}
