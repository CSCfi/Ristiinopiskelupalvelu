package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.CompositeIdentifiedEntityType;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.LocalisedString;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Optionality;
import fi.uta.ristiinopiskelu.datamodel.dto.current.common.StudyModuleType;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.math.BigDecimal;

@Document(indexName = "opintokokonaisuudet", createIndex = false)
public class StudyModuleEntity extends StudyElementEntity implements Serializable {

    private Integer amountValueMin;
    private Integer amountValueMax;
    private Optionality optionality;
    private LocalisedString optionalityFurtherInformation;
    private StudyModuleType studyModuleType;
    private BigDecimal credits;

    public StudyModuleEntity() {
        setType(CompositeIdentifiedEntityType.STUDY_MODULE);
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

    public LocalisedString getOptionalityFurtherInformation() {
        return optionalityFurtherInformation;
    }

    public void setOptionalityFurtherInformation(LocalisedString optionalityFurtherInformation) {
        this.optionalityFurtherInformation = optionalityFurtherInformation;
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

