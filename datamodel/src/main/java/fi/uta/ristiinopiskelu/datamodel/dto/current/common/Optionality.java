package fi.uta.ristiinopiskelu.datamodel.dto.current.common;

public enum Optionality {
    ALL,
    MIN_MAX_AMOUNT, // use amountValueMin and amountValueMax to save value (if no min max use same value in both)
    MIN_MAX_CREDITS // use creditsMin and creditsMax to save value (if no min max use same value in both)
}
