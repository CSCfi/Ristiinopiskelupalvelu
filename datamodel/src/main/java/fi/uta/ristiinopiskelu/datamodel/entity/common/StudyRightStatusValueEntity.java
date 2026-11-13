package fi.uta.ristiinopiskelu.datamodel.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Opiskeluoikeuden tila"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Opiskeluoikeudentila,Studier%C3%A4ttersstatus
 */
public enum StudyRightStatusValueEntity {
    ACTIVE(1),
    OPTION(2),
    ENDED(3),
    PASSIVATED(4),
    FORFEITED(5);

    private final int code;

    StudyRightStatusValueEntity(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static StudyRightStatusValueEntity fromValue(int code) {
        for (StudyRightStatusValueEntity b : StudyRightStatusValueEntity.values()) {
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

