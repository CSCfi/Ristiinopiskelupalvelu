package fi.uta.ristiinopiskelu.datamodel.dto.current.common.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA+KOSKI-codes, "Opiskeluoikeuden tyyppi"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Opiskeluoikeudentyyppi,Studier%C3%A4ttenstyp
 */
public enum StudyRightType {
    BACHELOR("1"),
    UNDERGRADUATE_DEGREE("2"),
    MASTERS_DEGREE("3"),
    GRADUATE_DEGREE("4"),
    MEDICAL_SPECIALIZATION("5"),
    LICENTIATE("6"),
    PHD("7"),
    STUDENT_MOBILITY("8"),
    EXCHANGE("9"),
    CONTINUING_EDUCATION("10"),
    SPECIALIST_TEACHING("12"),
    OPEN_STUDIES("13"),
    TEACHERS_PEDAGOGICAL_STUDIES("14"),
    TEACHERS_STUDIES("15"),
    INDENTURE_EDUCATION("16"),
    PREPATORY_EDUCATION("17"),
    DISTINCT_RIGHT("18"),
    SPECIALIST_EDUCATION("19"),
    ADULT_BASIC_EDUCATION("aikuistenperusopetus"),
    VOCATIONAL_EDUCATION("ammatillinenkoulutus"),
    DIA_DEGREE("diatutkinto"),
    PRESCHOOL_EDUCATION("esiopetus"),
    IB_DEGREE("ibtutkinto"),
    INTERNATIONAL_SCHOOL("internationalschool"),
    HIGHER_EDUCATION("korkeakoulutus"),
    HIGHSCHOOL_EDUCATION("lukiokoulutus"),
    LUVA("luva"),
    PREPARATORY_EDUCATION_FOR_BASIC_EDUCATION("perusopetukseenvalmistavaopetus"),
    FURTHER_EDUCATION_IN_BASIC_EDUCATION("perusopetuksenlisaopetus"),
    BASIC_EDUCATION("perusopetus"),
    MATRICULATION_EXAMINATION("ylioppilastutkinto");

    private final String code;

    StudyRightType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static StudyRightType fromValue(String text) {
        for (StudyRightType b : StudyRightType.values()) {
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
