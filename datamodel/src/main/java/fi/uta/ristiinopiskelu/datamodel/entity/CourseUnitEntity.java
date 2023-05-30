package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.ScaleValue;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Document(indexName = "opintojaksot", createIndex = false)
public class CourseUnitEntity extends StudyElementEntity implements Serializable {

    @Field(type = FieldType.Integer)
    private ScaleValue assessmentScale;
    
    private List<LocalisedString> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOptionEntity> completionOptions;
    private List<CourseUnitRealisationEntity> realisations;

    public CourseUnitEntity() {
        setType(CompositeIdentifiedEntityType.COURSE_UNIT);
    }

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
        if(!CollectionUtils.isEmpty(this.getCompletionOptions())) {
            for(CompletionOptionEntity completionOption : this.getCompletionOptions()) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    assessmentItemEntities.addAll(completionOption.getAssessmentItems());
                }
            }
        }
        return assessmentItemEntities;
    }

    @JsonIgnore
    @Transient
    public List<CourseUnitRealisationEntity> getAssessmentItemRealisations() {
        List<CourseUnitRealisationEntity> realisationEntities = new ArrayList<>();
        if(!CollectionUtils.isEmpty(this.getCompletionOptions())) {
            for(CompletionOptionEntity completionOption : this.getCompletionOptions()) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    for(AssessmentItemEntity ai : completionOption.getAssessmentItems()) {
                        if(!CollectionUtils.isEmpty(ai.getRealisations())) {
                            realisationEntities.addAll(ai.getRealisations());
                        }
                    }
                }
            }
        }
        return realisationEntities;
    }
}
