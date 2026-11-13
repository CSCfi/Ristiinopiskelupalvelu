package fi.uta.ristiinopiskelu.datamodel.entity;

import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.AbstractStudyElementWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.dto.current.write.studyelement.degree.DegreeWriteDTO;
import fi.uta.ristiinopiskelu.datamodel.entity.common.CompositeIdentifiedEntityTypeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "tutkinnot", createIndex = false)
public class DegreeEntity extends StudyElementEntity implements Serializable {

    private double duration;

    public DegreeEntity() {
        setType(CompositeIdentifiedEntityTypeEntity.DEGREE);
    }

    public DegreeEntity(double duration) {
        this();
        this.duration = duration;
    }

    public static DegreeEntity from(DegreeWriteDTO source) {
        if (source == null) {
            return null;
        }

        DegreeEntity target = new DegreeEntity();
        AbstractStudyElementWriteDTO.copyStudyElementWriteDtoToEntity(source, target);
        target.setDuration(source.getDuration());
        return target;
    }
}
