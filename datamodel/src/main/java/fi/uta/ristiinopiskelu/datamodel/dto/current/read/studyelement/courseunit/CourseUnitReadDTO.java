package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.ScaleValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Transient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "CourseUnit")
public class CourseUnitReadDTO extends AbstractStudyElementReadDTO {

    private ScaleValue assessmentScale;
    private List<LocalisedString> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOptionReadDTO> completionOptions;
    private List<CourseUnitRealisationReadDTO> realisations;

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

    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }

    public Integer getGroupSize() {
        return groupSize;
    }

    public void setCompletionOptions(List<CompletionOptionReadDTO> completionOptions) {
        this.completionOptions = completionOptions;
    }

    public List<CompletionOptionReadDTO> getCompletionOptions() {
        return completionOptions;
    }

    public List<CourseUnitRealisationReadDTO> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<CourseUnitRealisationReadDTO> realisations) {
        this.realisations = realisations;
    }

    @JsonIgnore
    @Transient
    public List<AssessmentItemReadDTO> getAssessmentItems() {
        List<AssessmentItemReadDTO> assessmentItemEntities = new ArrayList<>();
        if(!CollectionUtils.isEmpty(completionOptions)) {
            for(CompletionOptionReadDTO completionOption : completionOptions) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    assessmentItemEntities.addAll(completionOption.getAssessmentItems());
                }
            }
        }
        return assessmentItemEntities;
    }
}
