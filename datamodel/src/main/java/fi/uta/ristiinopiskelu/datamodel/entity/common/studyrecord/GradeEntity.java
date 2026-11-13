package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.Grade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GradeEntity {

    private String egtCode;
    private GradeCodeEntity code;

    public static GradeEntity from(Grade source) {
        if (source == null) {
            return null;
        }
        GradeEntity target = new GradeEntity();
        target.setEgtCode(source.getEgtCode());
        target.setCode(source.getCode() == null ? null : GradeCodeEntity.fromValue(source.getCode().getCode()));
        return target;
    }
}
