package fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.studymodule;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Optionality;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyElementType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyModuleType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.read.studyelement.AbstractStudyElementReadDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyElementEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.StudyModuleEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Schema(name = "StudyModuleV9", title = "StudyModuleV9", type = "object")
@JsonInclude(NON_NULL)
public class StudyModuleReadDTO extends AbstractStudyElementReadDTO {

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

    public StudyModuleReadDTO() {
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

    public static StudyModuleReadDTO from(StudyModuleEntity source) {
        StudyModuleReadDTO target = new StudyModuleReadDTO();
        StudyElementEntity.copyStudyElementEntityToReadDto(source, target);
        target.setAmountValueMin(source.getAmountValueMin());
        target.setAmountValueMax(source.getAmountValueMax());
        target.setOptionality(Optionality.from(source.getOptionality()));
        target.setOptionalityFurtherInformation(LocalisedString.from(source.getOptionalityFurtherInformation()));
        target.setStudyModuleType(StudyModuleType.from(source.getStudyModuleType()));
        target.setCredits(source.getCredits());
        return target;
    }
}
