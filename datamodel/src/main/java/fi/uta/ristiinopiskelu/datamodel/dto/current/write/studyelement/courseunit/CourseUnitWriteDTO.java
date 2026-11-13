package fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.ScaleValue;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.CourseUnitEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Transient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "CourseUnit", allOf = { AbstractStudyElementWriteDTO.class })
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

    public static CourseUnitWriteDTO from(CourseUnitEntity source) {
        if (source == null) {
            return null;
        }

        CourseUnitWriteDTO target = new CourseUnitWriteDTO();
        target.setMissing(source.getMissing());
        target.setSendingTime(source.getSendingTime());
        target.setCreatedTime(source.getCreatedTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setValidityStartDate(source.getValidityStartDate());
        target.setValidityEndDate(source.getValidityEndDate());
        target.setStudyElementId(source.getStudyElementId());
        target.setStudyElementPermanentId(source.getStudyElementPermanentId());
        target.setStudyElementIdentifierCode(source.getStudyElementIdentifierCode());
        target.setAbbreviation(source.getAbbreviation());
        target.setName(LocalisedString.from(source.getName()));
        target.setDescription(LocalisedString.from(source.getDescription()));
        target.setCreditsMin(source.getCreditsMin());
        target.setCreditsMax(source.getCreditsMax());
        target.setCreditsDescription(LocalisedString.from(source.getCreditsDescription()));
        target.setTeachingLanguage(source.getTeachingLanguage());
        target.setLanguagesOfCompletion(source.getLanguagesOfCompletion());
        target.setFurtherInformation(LocalisedString.from(source.getFurtherInformation()));
        target.setObjective(LocalisedString.from(source.getObjective()));
        target.setContent(LocalisedString.from(source.getContent()));
        target.setStatus(source.getStatus() == null ? null
            : fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyStatus.valueOf(source.getStatus().name()));
        target.setAssessmentScale(source.getAssessmentScale() == null ? null : ScaleValue.valueOf(source.getAssessmentScale().name()));
        target.setLearningMaterials(source.getLearningMaterials() == null ? null
            : source.getLearningMaterials().stream().map(LocalisedString::from).toList());
        target.setExamBookEnquiry(source.getExamBookEnquiry());
        target.setGroupSize(source.getGroupSize());
        return target;
    }
}
