package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.courseunit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.ScaleValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Transient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "CourseUnitV9", title = "CourseUnitV9", type = "object")
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

    public static CourseUnitReadDTO from(CourseUnitEntity source) {
        if(source == null) {
            return null;
        }

        CourseUnitReadDTO target = new CourseUnitReadDTO();
        StudyElementEntity.copyStudyElementEntityToReadDto(source, target);
        target.setAssessmentScale(ScaleValue.from(source.getAssessmentScale()));
        target.setLearningMaterials(source.getLearningMaterials() == null
            ? null
            : source.getLearningMaterials().stream().map(LocalisedString::from).toList());
        target.setExamBookEnquiry(source.getExamBookEnquiry());
        target.setGroupSize(source.getGroupSize());
        target.setCompletionOptions(CompletionOptionReadDTO.from(source.getCompletionOptions()));
        target.setRealisations(CourseUnitRealisationReadDTO.from(source.getRealisations()));
        return target;
    }
}
