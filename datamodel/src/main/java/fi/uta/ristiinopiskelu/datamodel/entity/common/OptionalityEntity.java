package fi.uta.ristiinopiskelu.datamodel.entity.common;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.Optionality;

import java.util.Arrays;

public enum OptionalityEntity {
    ALL,
    MIN_MAX_AMOUNT, // use amountValueMin and amountValueMax to save value (if no min max use same value in both)
    MIN_MAX_CREDITS; // use creditsMin and creditsMax to save value (if no min max use same value in both)

    public static OptionalityEntity from(Optionality optionality) {
        if(optionality == null) {
            return null;
        }

        return Arrays.stream(OptionalityEntity.values())
            .filter(c -> c.name().equals(optionality.name()))
            .findFirst()
            .orElse(null);
    }
}
