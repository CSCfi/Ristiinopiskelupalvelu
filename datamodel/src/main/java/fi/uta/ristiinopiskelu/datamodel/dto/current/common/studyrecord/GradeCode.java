package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Arvosana"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Arvosana-asteikot,Vitsordsskalor
 */
public enum GradeCode {
    GRADE_HYL("HYL"),
    GRADE_HYV("HYV"),
    GRADE_1("1"),
    GRADE_2("2"),
    GRADE_3("3"),
    GRADE_4("4"),
    GRADE_5("5"),
    GRADE_TT("TT"),
    GRADE_HT("HT"),
    GRADE_KH("KH"),
    GRADE_A("A"),
    GRADE_B("B"),
    GRADE_N("N"),
    GRADE_C("C"),
    GRADE_M("M"),
    GRADE_E("E"),
    GRADE_L("L");

    private final String code;

    GradeCode(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static GradeCode fromValue(String text) {
        for (GradeCode b : GradeCode.values()) {
            if (b.code.equals(text)) {
                return b;
            }
        }
        return null;
    }

    // Show values correctly in Swagger
    @Override
    public String toString() {
        return this.code;
    }
}
