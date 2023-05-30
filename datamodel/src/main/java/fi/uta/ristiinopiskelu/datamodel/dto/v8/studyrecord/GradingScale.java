package fi.uta.ristiinopiskelu.datamodel.dto.v8.studyrecord;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class GradingScale {

    @Field(type = FieldType.Integer)
    private ScaleValue scale;

    public ScaleValue getScale() {
        return scale;
    }

    public void setScale(ScaleValue scale) {
        this.scale = scale;
    }
}
