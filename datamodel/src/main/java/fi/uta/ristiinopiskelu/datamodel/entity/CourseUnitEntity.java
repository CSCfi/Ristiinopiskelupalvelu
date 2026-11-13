package fi.uta.ristiinopiskelu.datamodel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit.CourseUnitWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CompositeIdentifiedEntityTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.ScaleValueEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "opintojaksot", createIndex = false)
public class CourseUnitEntity extends StudyElementEntity implements Serializable {

    @Field(type = FieldType.Integer)
    private ScaleValueEntity assessmentScale;

    private List<LocalisedStringEntity> learningMaterials = null;
    private Boolean examBookEnquiry = null;
    private Integer groupSize = null;
    private List<CompletionOptionEntity> completionOptions;
    private List<CourseUnitRealisationEntity> realisations;

    public CourseUnitEntity() {
        setType(CompositeIdentifiedEntityTypeEntity.COURSE_UNIT);
    }

    @JsonIgnore
    @Transient
    public List<AssessmentItemEntity> getAssessmentItems() {
        List<AssessmentItemEntity> assessmentItemEntities = new ArrayList<>();
        if (!CollectionUtils.isEmpty(this.getCompletionOptions())) {
            for (CompletionOptionEntity completionOption : this.getCompletionOptions()) {
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
        if (!CollectionUtils.isEmpty(this.getCompletionOptions())) {
            for (CompletionOptionEntity completionOption : this.getCompletionOptions()) {
                if (!CollectionUtils.isEmpty(completionOption.getAssessmentItems())) {
                    for (AssessmentItemEntity ai : completionOption.getAssessmentItems()) {
                        if (!CollectionUtils.isEmpty(ai.getRealisations())) {
                            realisationEntities.addAll(ai.getRealisations());
                        }
                    }
                }
            }
        }
        return realisationEntities;
    }

    public static CourseUnitEntity from(CourseUnitWriteDTO source) {
        if (source == null) {
            return null;
        }

        CourseUnitEntity target = new CourseUnitEntity();
        AbstractStudyElementWriteDTO.copyStudyElementWriteDtoToEntity(source, target);
        target.setAssessmentScale(source.getAssessmentScale() == null ? null : ScaleValueEntity.valueOf(source.getAssessmentScale().name()));
        target.setLearningMaterials(source.getLearningMaterials() == null ? null
            : source.getLearningMaterials().stream().map(LocalisedStringEntity::from).toList());
        target.setExamBookEnquiry(source.getExamBookEnquiry());
        target.setGroupSize(source.getGroupSize());
        target.setCompletionOptions(CompletionOptionEntity.from(source.getCompletionOptions()));
        target.setRealisations(CourseUnitRealisationEntity.from(source.getRealisations()));
        return target;
    }
}
