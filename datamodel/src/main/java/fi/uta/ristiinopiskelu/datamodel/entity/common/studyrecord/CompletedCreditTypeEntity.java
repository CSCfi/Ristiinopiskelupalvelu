package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Opintosuorituksen laji"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Opintosuorituksenlaji,Studieprestationensart.1
 */
public enum CompletedCreditTypeEntity {
    DEGREE_PROGRAMME_COMPLETION(1),
    OTHER_COMPLETION(2),
    PARTIALLY_COMPLETED(3);

    private final int code;

    CompletedCreditTypeEntity(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static CompletedCreditTypeEntity fromValue(int code) {
        for (CompletedCreditTypeEntity b : CompletedCreditTypeEntity.values()) {
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
