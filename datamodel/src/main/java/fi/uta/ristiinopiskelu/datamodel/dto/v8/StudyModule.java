package fi.uta.ristiinopiskelu.datamodel.dto.v8;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * fi: Opintokokonaisuus
 * 
 * @author Eero Manninen <eero.manninen@studyo.fi>
 * 
 *         Based on
 *         https://wiki.eduuni.fi/download/attachments/70202805/CSC6_curriculum_api_1.0.0-oas3_swagger.json?version=1&modificationDate=1531828139937&api=v2
 */
/* TUNI EXTENSION */

@JsonInclude(NON_NULL)

/* TUNI EXTENSION ENDS */

public class StudyModule extends StudyElement {
    private Integer amountValueMin;
    private Integer amountValueMax;
    private Optionality optionality;
    private LocalisedString optionalityFurtherInformation;
    private BigDecimal credits;

    /**
     * M2 2.12.1
     * 
     * - Basic studies
     * 
     * - Intermediate studies
     * 
     * - Advanced studies
     * 
     * - Language and communication studies
     * 
     * - Training
     * 
     * - Post-graduate studies
     * 
     * - Other study module
     */
    private StudyModuleType studyModuleType;

    public StudyModule() {
        setType(StudyElementType.STUDY_MODULE);
    }

    public LocalisedString getOptionalityFurtherInformation() {
        return optionalityFurtherInformation;
    }

    public void setOptionalityFurtherInformation(LocalisedString optionalityFurtherInformation) {
        this.optionalityFurtherInformation = optionalityFurtherInformation;
    }

    public Integer getAmountValueMin() {
        return amountValueMin;
    }

    public void setAmountValueMin(Integer amountValueMin) {
        this.amountValueMin = amountValueMin;
    }

    public Integer getAmountValueMax() {
        return amountValueMax;
    }

    public void setAmountValueMax(Integer amountValueMax) {
        this.amountValueMax = amountValueMax;
    }

    public Optionality getOptionality() {
        return optionality;
    }
    
    public void setOptionality(Optionality optionality) {
        this.optionality = optionality;
    }

    public StudyModuleType getStudyModuleType() {
        return studyModuleType;
    }

    public void setStudyModuleType(StudyModuleType studyModuleType) {
        this.studyModuleType = studyModuleType;
    }

    public BigDecimal getCredits() {
        return credits;
    }

    public void setCredits(BigDecimal credits) {
        this.credits = credits;
    }
}
