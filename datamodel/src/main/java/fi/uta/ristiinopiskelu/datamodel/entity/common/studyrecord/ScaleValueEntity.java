package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Arvosteluasteikko"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Arvosana-asteikot,Vitsordsskalor
 */
public enum ScaleValueEntity {

    FIVE_LEVEL(1),
    ACCEPTED(2),
    OTHER_DOMESTIC(3);

    private final int code;

    ScaleValueEntity(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static ScaleValueEntity fromValue(int code) {
        for (ScaleValueEntity b : ScaleValueEntity.values()) {
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
