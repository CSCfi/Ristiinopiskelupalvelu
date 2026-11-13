package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.studymodule.StudyModuleWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CompositeIdentifiedEntityTypeEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.LocalisedStringEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.OptionalityEntity;
import fi.uta.ristiinopiskelu.datamodel.entity.common.StudyModuleTypeEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Document(indexName = "opintokokonaisuudet", createIndex = false)
public class StudyModuleEntity extends StudyElementEntity implements Serializable {

    private Integer amountValueMin;
    private Integer amountValueMax;
    private OptionalityEntity optionality;
    private LocalisedStringEntity optionalityFurtherInformation;
    private StudyModuleTypeEntity studyModuleType;
    private BigDecimal credits;

    public StudyModuleEntity() {
        setType(CompositeIdentifiedEntityTypeEntity.STUDY_MODULE);
    }

    public static StudyModuleEntity from(StudyModuleWriteDTO source) {
        if (source == null) {
            return null;
        }

        StudyModuleEntity target = new StudyModuleEntity();
        AbstractStudyElementWriteDTO.copyStudyElementWriteDtoToEntity(source, target);
        target.setAmountValueMin(source.getAmountValueMin());
        target.setAmountValueMax(source.getAmountValueMax());
        target.setOptionality(OptionalityEntity.from(source.getOptionality()));
        target.setOptionalityFurtherInformation(LocalisedStringEntity.from(source.getOptionalityFurtherInformation()));
        target.setStudyModuleType(StudyModuleTypeEntity.from(source.getStudyModuleType()));
        target.setCredits(source.getCredits());
        return target;
    }
}
