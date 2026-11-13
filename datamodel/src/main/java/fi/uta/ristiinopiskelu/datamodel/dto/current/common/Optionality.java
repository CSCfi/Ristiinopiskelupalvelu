package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

import fi.uta.ristiinopiskelu.datamodel.entity.common.OptionalityEntity;

public enum Optionality {
    ALL,
    MIN_MAX_AMOUNT, // use amountValueMin and amountValueMax to save value (if no min max use same value in both)
    MIN_MAX_CREDITS; // use creditsMin and creditsMax to save value (if no min max use same value in both)

    public static Optionality from(OptionalityEntity source) {
        return source == null ? null : Optionality.valueOf(source.name());
    }
}
