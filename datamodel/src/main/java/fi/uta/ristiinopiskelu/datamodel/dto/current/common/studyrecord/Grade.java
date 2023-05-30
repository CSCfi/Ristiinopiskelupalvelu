package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

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
}

