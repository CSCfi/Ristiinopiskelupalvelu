package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.GradingScale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GradingScaleEntity {

    @Field(type = FieldType.Integer)
    private ScaleValueEntity scale;

    public static GradingScaleEntity from(GradingScale source) {
        if (source == null) {
            return null;
        }
        GradingScaleEntity target = new GradingScaleEntity();
        target.setScale(source.getScale() == null ? null : ScaleValueEntity.fromValue(source.getScale().getCode()));
        return target;
    }
}
