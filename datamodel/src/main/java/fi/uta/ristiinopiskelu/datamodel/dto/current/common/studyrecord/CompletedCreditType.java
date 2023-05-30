package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * VIRTA-codes, "Opintosuorituksen laji"
 *
 * https://wiki.eduuni.fi/display/CSCVIRTA/Tietovarannon+koodistot#Tietovarannonkoodistot-Opintosuorituksenlaji,Studieprestationensart.1
 */
public enum CompletedCreditType {
    DEGREE_PROGRAMME_COMPLETION(1),
    OTHER_COMPLETION(2),
    PARTIALLY_COMPLETED(3);

    private final int code;

    CompletedCreditType(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static CompletedCreditType fromValue(int code) {
        for (CompletedCreditType b : CompletedCreditType.values()) {
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
