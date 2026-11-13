package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.GradeEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GradeV9", title = "GradeV9")
public class Grade {
    private String egtCode;
    private GradeCode code;

    public String getEgtCode() {
        return egtCode;
    }

    public void setEgtCode(String egtCode) {
        this.egtCode = egtCode;
    }

    public GradeCode getCode() {
        return code;
    }

    public void setCode(GradeCode code) {
        this.code = code;
    }

    public static Grade from(GradeEntity source) {
        if (source == null) {
            return null;
        }
        Grade target = new Grade();
        target.setEgtCode(source.getEgtCode());
        target.setCode(source.getCode() == null ? null : GradeCode.fromValue(source.getCode().getCode()));
        return target;
    }
}
