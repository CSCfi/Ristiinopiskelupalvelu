package fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord.CompletedCreditTransactionEntity;

public enum CompletedCreditTransaction {

    NEW_COMPLETED_CREDIT,
    NEW_CREDIT_REPLACES_FORMER,
    CORRECTION,
    REMOVAL;

    public static CompletedCreditTransaction from(CompletedCreditTransactionEntity source) {
        return source == null ? null : CompletedCreditTransaction.valueOf(source.name());
    }
}
