package fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.courseunit;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.AssessmentItemType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.realisation.RealisationWriteDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * AssessmentItem
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
public class AssessmentItemWriteDTO {

    @JsonProperty("assessmentItemId")
    private String assessmentItemId;
    /*
     * Suoritettaessa kertoo mille opintojaksolle ensisijaisesti arvioinnin kohde on
     * luotu
     */
    private String primaryCourseUnitId;
    private LocalisedString name;
    private List<RealisationWriteDTO> realisations = null;

    /**
     * M2 table 5 1.1
     * 
     * - lectures
     * 
     * - small group tutorials
     * 
     * - individual teaching
     * 
     * - group work
     * 
     * - individual work
     * 
     * - Written exam
     * 
     * - Exam electronic exam
     * 
     * - mid-term exam
     * 
     * - oral exam
     * 
     * - Essay
     * 
     * - Practice
     * 
     * - Thesis
     * 
     * - Project Internship
     * 
     * - Portfolio
     * 
     * - Learning diary
     * 
     * - Participation in course work
     * 
     * - Seminar
     * 
     * - competence test
     * 
     */
    private AssessmentItemType type;

    @JsonProperty("creditsMin")
    @Schema(description = "M2 1.22.2")
    private BigDecimal creditsMin = null;
  
    @JsonProperty("creditsMax")
    @Schema(description = "M2 1.22.3")
    private BigDecimal creditsMax = null;

    public String getAssessmentItemId() {
        return assessmentItemId;
    }

    public void setAssessmentItemId(String assesmentItemId) {
        this.assessmentItemId = assesmentItemId;
    }

    /**
     * @return String return the primaryCourseUnitId
     */
    public String getPrimaryCourseUnitId() {
        return primaryCourseUnitId;
    }

    /**
     * @param primaryCourseUnitId the primaryCourseUnitId to set
     */
    public void setPrimaryCourseUnitId(String primaryCourseUnitId) {
        this.primaryCourseUnitId = primaryCourseUnitId;
    }

    /**
     * @return AssessmentItemType return the type
     */
    public AssessmentItemType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(AssessmentItemType type) {
        this.type = type;
    }

    /**
     * @return List<AssessmentItemRealisation> return the realisations
     */
    public List<RealisationWriteDTO> getRealisations() {
        return realisations;
    }

    /**
     * @param realisations the realisations to set
     */
    public void setRealisations(List<RealisationWriteDTO> realisations) {
        this.realisations = realisations;
    }

    /**
     * @return Float return the creditsMin
     */
    public BigDecimal getCreditsMin() {
        return creditsMin;
    }

    /**
     * @param creditsMin the creditsMin to set
     */
    public void setCreditsMin(BigDecimal creditsMin) {
        this.creditsMin = creditsMin;
    }

    /**
     * @return Float return the creditsMax
     */
    public BigDecimal getCreditsMax() {
        return creditsMax;
    }

    /**
     * @param creditsMax the creditsMax to set
     */
    public void setCreditsMax(BigDecimal creditsMax) {
        this.creditsMax = creditsMax;
    }

    public LocalisedString getName() {
        return name;
    }

    public void setName(LocalisedString name) {
        this.name = name;
    }
}
