package fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Optionality;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyModuleType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Schema(name = "StudyModule", allOf = { AbstractStudyElementWriteDTO.class })
@JsonInclude(NON_NULL)
public class StudyModuleWriteDTO extends AbstractStudyElementWriteDTO {

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
