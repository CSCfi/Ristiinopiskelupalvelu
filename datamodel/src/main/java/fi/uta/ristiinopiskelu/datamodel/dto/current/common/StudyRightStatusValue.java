package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Opiskeluoikeuden tila"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Opiskeluoikeudentila,Studier%C3%A4ttersstatus
 */
public enum StudyRightStatusValue {
    ACTIVE(1),
    OPTION(2),
    ENDED(3),
    PASSIVATED(4),
    FORFEITED(5);

    private final int code;

    StudyRightStatusValue(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static StudyRightStatusValue fromValue(int code) {
        for (StudyRightStatusValue b : StudyRightStatusValue.values()) {
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

