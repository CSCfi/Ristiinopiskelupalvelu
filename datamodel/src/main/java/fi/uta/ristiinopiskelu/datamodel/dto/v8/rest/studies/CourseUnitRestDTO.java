package fi.uta.ristiinopiskelu.datamodel.dto.v8.rest.studies;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord.ScaleValue;
import fi.uta.ristiinopiskelu.datamodel.entity.AssessmentItemEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CompletionOptionEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitRealisationEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Transient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "CourseUnit")
public class CourseUnitRestDTO extends StudyElementRestDTO {

    private ScaleValue assessmentScale;
    private List<LocalisedString> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOptionEntity> completionOptions;
    private List<CourseUnitRealisationEntity> realisations;

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

    public void setCompletionOptions(List<CompletionOptionEntity> completionOptions) {
        this.completionOptions = completionOptions;
    }

    public List<CompletionOptionEntity> getCompletionOptions() {
        return completionOptions;
    }

    public List<CourseUnitRealisationEntity> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<CourseUnitRealisationEntity> realisations) {
        this.realisations = realisations;
    }

    @JsonIgnore
    @Transient
    public List<AssessmentItemEntity> getAssessmentItems() {
        List<AssessmentItemEntity> assessmentItemEntities = new ArrayList<>();
        if(!CollectionUtils.isEmpty(completionOptions)) {
            for(CompletionOptionEntity completionOption : completionOptions) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    assessmentItemEntities.addAll(completionOption.getAssessmentItems());
                }
            }
        }
        return assessmentItemEntities;
    }
}
