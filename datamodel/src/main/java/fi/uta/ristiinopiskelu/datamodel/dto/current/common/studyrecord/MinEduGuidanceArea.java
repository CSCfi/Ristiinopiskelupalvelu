package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Koulutusala"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Koulutusala,Utbildningsomr%C3%A5de
 */
public enum MinEduGuidanceArea {
    EDUCATION(1),
    ARTS_AND_CULTURE(2),
    HUMANITIES(3),
    SOCIAL_SCIENCES(4),
    BUSINESS_ADMINIST_AND_LAW(5),
    NATURAL_SCIENCES(6),
    IT_AND_COM_TECH(7),
    ENGINEERING_AND_TECH(8),
    AGRICULTURE_AND_FORESTY(9),
    MEDICAL_SCIENCE(10),
    HEALTH_AND_WELFARE(11),
    SERVICES(12);

    private final int code;

    MinEduGuidanceArea(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static MinEduGuidanceArea fromValue(int code) {
        for (MinEduGuidanceArea b : MinEduGuidanceArea.values()) {
            if (b.code == code) {
                return b;
            }
        }
        return null;
    }

    // Show values correctly in Swagger
    @Override
    public String toString() {
        return String.valueOf(this.code);
    }
}
