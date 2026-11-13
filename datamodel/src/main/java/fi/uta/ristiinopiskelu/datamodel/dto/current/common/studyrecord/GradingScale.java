package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.GradingScaleEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Schema(name = "GradingScaleV9", title = "GradingScaleV9")
public class GradingScale {

    @Field(type = FieldType.Integer)
    private ScaleValue scale;

    public ScaleValue getScale() {
        return scale;
    }

    public void setScale(ScaleValue scale) {
        this.scale = scale;
    }

    public static GradingScale from(GradingScaleEntity source) {
        if (source == null) {
            return null;
        }
        GradingScale target = new GradingScale();
        target.setScale(source.getScale() == null ? null : ScaleValue.fromValue(source.getScale().getCode()));
        return target;
    }
}
