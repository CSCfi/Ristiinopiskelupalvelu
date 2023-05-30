package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.AssessmentItemType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "AssessmentItem")
public class AssessmentItemEntity {

    private String assessmentItemId;
    private String primaryCourseUnitId;
    private AssessmentItemType type;
    private BigDecimal creditsMin = null;
    private BigDecimal creditsMax = null;
    private LocalisedString name;
    private List<CourseUnitRealisationEntity> realisations;

    public String getAssessmentItemId() {
        return assessmentItemId;
    }

    public void setAssessmentItemId(String assessmentItemId) {
        this.assessmentItemId = assessmentItemId;
    }

    public String getPrimaryCourseUnitId() {
        return primaryCourseUnitId;
    }

    public void setPrimaryCourseUnitId(String primaryCourseUnitId) {
        this.primaryCourseUnitId = primaryCourseUnitId;
    }

    public AssessmentItemType getType() {
        return type;
    }

    public void setType(AssessmentItemType type) {
        this.type = type;
    }

    public BigDecimal getCreditsMin() {
        return creditsMin;
    }

    public void setCreditsMin(BigDecimal creditsMin) {
        this.creditsMin = creditsMin;
    }

    public BigDecimal getCreditsMax() {
        return creditsMax;
    }

    public void setCreditsMax(BigDecimal creditsMax) {
        this.creditsMax = creditsMax;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }

    public List<CourseUnitRealisationEntity> getRealisations() {
        return realisations;
    }

    public void setRealisations(List<CourseUnitRealisationEntity> realisations) {
        this.realisations = realisations;
    }
}
