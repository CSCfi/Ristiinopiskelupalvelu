package fi.uta.ristiinopiskelu.datamodel.entity.common.studyrecord;

import fi.uta.ristiinopiskelu.datamodel.dto.current.common.studyrecord.CompletedCreditTransaction;

public enum CompletedCreditTransactionEntity {

    NEW_COMPLETED_CREDIT,
    NEW_CREDIT_REPLACES_FORMER,
    CORRECTION,
    REMOVAL;

    public static CompletedCreditTransactionEntity from(CompletedCreditTransaction source) {
        return source == null ? null : CompletedCreditTransactionEntity.valueOf(source.name());
    }
}
